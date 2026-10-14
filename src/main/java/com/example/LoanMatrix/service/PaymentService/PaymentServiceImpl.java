package com.example.LoanMatrix.service.PaymentService;

import com.example.LoanMatrix.config.RazorpayConfig;
import com.example.LoanMatrix.dto.Payment.CreatePaymentRequest;
import com.example.LoanMatrix.dto.Payment.PaymentHistoryResponse;
import com.example.LoanMatrix.dto.Payment.PaymentOrderResponse;
import com.example.LoanMatrix.dto.Payment.VerifyPaymentRequest;
import com.example.LoanMatrix.entity.*;
import com.example.LoanMatrix.exception.EmiAlreadyPaidException;
import com.example.LoanMatrix.exception.InvalidPaymentSignatureException;
import com.example.LoanMatrix.exception.PaymentAlreadyVerifiedException;
import com.example.LoanMatrix.repository.EmiScheduleRepository;
import com.example.LoanMatrix.repository.LoanPaymentRepository;
import com.example.LoanMatrix.repository.Payment.PaymentTransactionRepository;
import com.example.LoanMatrix.repository.Payment.PenaltyChargeRepository;
import com.example.LoanMatrix.repository.LoanAccountRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final EmiScheduleRepository emiScheduleRepository;
    private final LoanPaymentRepository loanPaymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PenaltyChargeRepository penaltyChargeRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final RazorpayConfig razorpayConfig;

    @Override
    public PaymentOrderResponse createPaymentOrder(CreatePaymentRequest request) throws RazorpayException {

        log.info("Payment order creation started");

        EmiSchedule emiSchedule = emiScheduleRepository
                .findByIdAndLoanAccount_Id(
                        request.getEmiScheduleId(),
                        request.getLoanAccountId()
                )
                .orElseThrow(() -> {
                    log.warn("EMI schedule not found for the requested loan account");
                    return new RuntimeException("EMI schedule not found");
                });

        log.debug("EMI schedule fetched successfully: {}", emiSchedule.getId());

        if ("PAID".equalsIgnoreCase(emiSchedule.getPaymentStatus())) {
            log.warn("Payment order creation rejected because EMI is already paid");
            throw new EmiAlreadyPaidException("EMI is already paid");
        }

        List<EmiSchedule> emiSchedules =
                emiScheduleRepository.findByLoanAccount_Id(request.getLoanAccountId());

        log.debug("EMI schedules fetched: {}", emiSchedules.size());

        BigDecimal payableAmount = BigDecimal.ZERO;

        for (int i = 0; i < emiSchedules.size(); i++) {
            EmiSchedule schedule = emiSchedules.get(i);

            if (schedule.getDueDate().isAfter(emiSchedule.getDueDate())) {
                continue;
            }

            if ("PAID".equalsIgnoreCase(schedule.getPaymentStatus())) {
                continue;
            }

            BigDecimal scheduleEmi = schedule.getEmi() == null
                    ? BigDecimal.ZERO
                    : schedule.getEmi();

            payableAmount = payableAmount.add(scheduleEmi);

            List<PenaltyCharge> penalties =
                    penaltyChargeRepository.findByEmiSchedule_Id(schedule.getId());

            log.debug("Penalty records fetched for an EMI: {}", penalties.size());

            for (int j = 0; j < penalties.size(); j++) {
                PenaltyCharge penalty = penalties.get(j);

                if (!"PAID".equalsIgnoreCase(penalty.getStatus())) {
                    BigDecimal penaltyAmount = penalty.getPenaltyAmount() == null
                            ? BigDecimal.ZERO
                            : penalty.getPenaltyAmount();

                    payableAmount = payableAmount.add(penaltyAmount);
                }
            }
        }

        log.info("Payable amount calculation completed");

        RazorpayClient razorpayClient = new RazorpayClient(
                razorpayConfig.getKeyId(),
                razorpayConfig.getKeySecret()
        );

        JSONObject orderRequest = new JSONObject();

        orderRequest.put(
                "amount",
                payableAmount.multiply(BigDecimal.valueOf(100)).intValue()
        );

        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "EMI_" + request.getEmiScheduleId());

        log.info("Creating Razorpay order");

        Order razorpayOrder = razorpayClient.orders.create(orderRequest);

        String razorpayOrderId = razorpayOrder.get("id");

        log.info("Razorpay order created successfully");

        PaymentTransaction transaction = new PaymentTransaction();

        transaction.setLoanAccount(emiSchedule.getLoanAccount());
        transaction.setEmiSchedule(emiSchedule);
        transaction.setRazorpayOrderId(razorpayOrderId);
        transaction.setAmount(payableAmount);
        transaction.setStatus("CREATED");
        transaction.setCreatedAt(LocalDateTime.now());

        paymentTransactionRepository.save(transaction);

        log.info("Payment transaction saved successfully");

        return new PaymentOrderResponse(
                razorpayOrderId,
                payableAmount,
                "INR",
                razorpayConfig.getKeyId()
        );
    }

    @Override
    @Transactional
    public void verifyPayment(VerifyPaymentRequest request) throws RazorpayException {

        log.info("Payment verification started");

        if (request == null) {
            log.warn("Payment verification request is null");
            throw new RuntimeException("Payment verification request is required");
        }

        if (request.getRazorpayOrderId() == null
                || request.getRazorpayOrderId().isBlank()) {
            log.warn("Payment verification rejected because order ID is missing");
            throw new RuntimeException("Razorpay order ID is required");
        }

        if (request.getRazorpayPaymentId() == null
                || request.getRazorpayPaymentId().isBlank()) {
            log.warn("Payment verification rejected because payment ID is missing");
            throw new RuntimeException("Razorpay payment ID is required");
        }

        if (request.getRazorpaySignature() == null
                || request.getRazorpaySignature().isBlank()) {
            log.warn("Payment verification rejected because signature is missing");
            throw new RuntimeException("Razorpay signature is required");
        }

        PaymentTransaction transaction = paymentTransactionRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> {
                    log.warn("Payment transaction not found for the supplied order ID");
                    return new RuntimeException("Payment transaction not found");
                });

        if ("SUCCESS".equals(transaction.getStatus())) {
            log.warn("Payment verification rejected because payment is already verified");
            throw new PaymentAlreadyVerifiedException("Payment already verified");
        }

        String payload = request.getRazorpayOrderId()
                + "|" + request.getRazorpayPaymentId();

        boolean isValid = Utils.verifySignature(
                payload,
                request.getRazorpaySignature(),
                razorpayConfig.getKeySecret()
        );

        if (!isValid) {
            log.warn("Payment verification failed due to invalid signature");
            throw new InvalidPaymentSignatureException("Invalid payment signature");
        }

        log.debug("Payment signature verified successfully");

        EmiSchedule emiSchedule = transaction.getEmiSchedule();

        if ("PAID".equals(emiSchedule.getPaymentStatus())) {
            log.warn("Payment verification rejected because EMI is already paid");
            throw new EmiAlreadyPaidException("EMI is already paid");
        }

        LoanAccount loanAccount = transaction.getLoanAccount();
        BigDecimal paymentAmount = transaction.getAmount();
        BigDecimal principalAmount = emiSchedule.getPrincipalAmount();

        LoanPayment loanPayment = new LoanPayment();

        loanPayment.setLoanAccount(loanAccount);
        loanPayment.setPaymentAmount(paymentAmount);
        loanPayment.setPaymentDate(LocalDate.now());
        loanPayment.setPaymentStatus("SUCCESS");
        loanPayment.setPaymentName("EMI Payment");

        loanPaymentRepository.save(loanPayment);

        log.debug("Loan payment record saved successfully");

        emiSchedule.setPaymentStatus("PAID");
        emiSchedule.setPaidDate(LocalDate.now());

        emiScheduleRepository.save(emiSchedule);

        log.debug("EMI schedule updated successfully");

        BigDecimal currentTotalPaid = loanAccount.getTotalPaidAmount();

        if (currentTotalPaid == null) {
            currentTotalPaid = BigDecimal.ZERO;
        }

        loanAccount.setTotalPaidAmount(currentTotalPaid.add(paymentAmount));

        BigDecimal currentOutstandingPrincipal =
                loanAccount.getOutstandingPrincipal();

        if (currentOutstandingPrincipal == null) {
            currentOutstandingPrincipal = BigDecimal.ZERO;
        }

        loanAccount.setOutstandingPrincipal(
                currentOutstandingPrincipal.subtract(principalAmount)
        );

        loanAccountRepository.save(loanAccount);

        log.debug("Loan account payment details updated successfully");

        transaction.setRazorpayPaymentId(request.getRazorpayPaymentId());
        transaction.setRazorpaySignature(request.getRazorpaySignature());
        transaction.setStatus("SUCCESS");

        paymentTransactionRepository.save(transaction);

        log.info("Payment verified and transaction updated successfully");
    }

    @Override
    public List<PaymentHistoryResponse> getPaymentHistory(Long loanAccountId) {

        log.info("Fetching payment history for loan account ID: {}", loanAccountId);

        List<LoanPayment> payments =
                loanPaymentRepository.findByLoanAccount_Id(loanAccountId);

        log.debug("Payment history records fetched: {}", payments.size());

        List<PaymentHistoryResponse> response = new ArrayList<>();

        for (LoanPayment payment : payments) {
            response.add(new PaymentHistoryResponse(
                    payment.getId(),
                    payment.getPaymentAmount(),
                    payment.getPaymentDate(),
                    payment.getPaymentStatus(),
                    payment.getPaymentName()
            ));
        }

        log.info("Payment history retrieved successfully");

        return response;
    }
}
