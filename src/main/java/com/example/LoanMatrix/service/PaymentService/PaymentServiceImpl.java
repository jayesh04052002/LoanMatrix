package com.example.LoanMatrix.service.PaymentService;

import com.example.LoanMatrix.config.RazorpayConfig;
import com.example.LoanMatrix.dto.Payment.CreatePaymentRequest;
import com.example.LoanMatrix.dto.Payment.PaymentHistoryResponse;
import com.example.LoanMatrix.dto.Payment.PaymentOrderResponse;
import com.example.LoanMatrix.dto.Payment.VerifyPaymentRequest;
import com.example.LoanMatrix.entity.*;
import com.example.LoanMatrix.exception.EmiAlreadyPaidException;
import com.example.LoanMatrix.exception.EmiNotFoundException;
import com.example.LoanMatrix.exception.InvalidPaymentSignatureException;
import com.example.LoanMatrix.exception.PaymentAlreadyVerifiedException;
import com.example.LoanMatrix.repository.Payment.EmiScheduleRepository;
import com.example.LoanMatrix.repository.Payment.LoanPaymentRepository;
import com.example.LoanMatrix.repository.Payment.PaymentTransactionRepository;
import com.example.LoanMatrix.repository.Payment.PenaltyChargeRepository;
import com.example.LoanMatrix.repository.Support.LoanAccountRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
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
public class PaymentServiceImpl implements PaymentService {

    private final EmiScheduleRepository emiScheduleRepository;
    private final LoanPaymentRepository loanPaymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PenaltyChargeRepository penaltyChargeRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final RazorpayConfig razorpayConfig;

    @Override
    public PaymentOrderResponse createPaymentOrder(CreatePaymentRequest request) throws RazorpayException {

        // Fetch the selected EMI
        EmiSchedule emiSchedule = emiScheduleRepository
                .findByIdAndLoanAccount_Id(
                        request.getEmiScheduleId(),
                        request.getLoanAccountId()
                )
                .orElseThrow(() -> new RuntimeException("EMI schedule not found"));

// Check whether the selected EMI is already paid
        if ("PAID".equalsIgnoreCase(emiSchedule.getPaymentStatus())) {
            throw new EmiAlreadyPaidException("EMI is already paid");
        }

// Fetch all EMIs for this loan account
        List<EmiSchedule> emiSchedules = emiScheduleRepository.findByLoanAccount_Id(request.getLoanAccountId());

        BigDecimal payableAmount = BigDecimal.ZERO;
        for (int i = 0; i < emiSchedules.size(); i++) {
            EmiSchedule schedule = emiSchedules.get(i);
            // Include only the selected EMI and earlier EMIs
            if (schedule.getDueDate().isAfter(emiSchedule.getDueDate())) {
                continue;
            }

            // Skip EMIs that are already paid
            if ("PAID".equalsIgnoreCase(schedule.getPaymentStatus())) {
                continue;
            }

            // Add the unpaid EMI amount
            BigDecimal scheduleEmi = schedule.getEmi() == null
                    ? BigDecimal.ZERO
                    : schedule.getEmi();

            payableAmount = payableAmount.add(scheduleEmi);

            // Fetch penalties for this EMI
            List<PenaltyCharge> penalties = penaltyChargeRepository.findByEmiSchedule_Id(schedule.getId());

            for (int j = 0; j < penalties.size(); j++) {
                PenaltyCharge penalty = penalties.get(j);

                // Add penalties that are not paid
                if (!"PAID".equalsIgnoreCase(penalty.getStatus())) {
                    BigDecimal penaltyAmount = penalty.getPenaltyAmount() == null ? BigDecimal.ZERO : penalty.getPenaltyAmount();
                    payableAmount = payableAmount.add(penaltyAmount);
                }
            }
        }

        System.out.println("Total Payable: " + payableAmount);

//        return new PaymentOrderResponse(
//                null, payableAmount, "INR", razorpayConfig.getKeyId()
//        );

//        TODO: create razorpay order

//        TODO: this basically create connection object for razorpay
        RazorpayClient razorpayClient = new RazorpayClient(razorpayConfig.getKeyId(), razorpayConfig.getKeySecret());

//        TODO: creating a JSON object that will be sent to the razorpay
        JSONObject orderRequest = new JSONObject();

//        TODO: set the amount
        orderRequest.put("amount", payableAmount.multiply(BigDecimal.valueOf(100)).intValue());

//        TODO: set currency
        orderRequest.put("currency", "INR");

//        TODO: Create receipt
        orderRequest.put("receipt", "EMI_" + request.getEmiScheduleId());

//        TODO: send the request to the razorpay, create razorpay order
        Order razorpayOrder = razorpayClient.orders.create(orderRequest);

//        TODO: get the razorpay ID
        String razorpayOrderId = razorpayOrder.get("id");

        PaymentTransaction transaction = new PaymentTransaction();

        transaction.setLoanAccount(emiSchedule.getLoanAccount());
        transaction.setEmiSchedule(emiSchedule);
        transaction.setRazorpayOrderId(razorpayOrderId);
        transaction.setAmount(payableAmount);
        transaction.setStatus("CREATED");
        transaction.setCreatedAt(LocalDateTime.now());

        paymentTransactionRepository.save(transaction);

//        TODO: sending the information back to the controller, return the response
        return new PaymentOrderResponse(razorpayOrderId, payableAmount, "INR", razorpayConfig.getKeyId());

    }

    @Override
    @Transactional
    public void verifyPayment(VerifyPaymentRequest request) throws RazorpayException {

        if (request == null) {
            throw new RuntimeException("Payment verification request is required");
        }

        if (request.getRazorpayOrderId() == null || request.getRazorpayOrderId().isBlank()) {
            throw new RuntimeException("Razorpay order ID is required");
        }

        if (request.getRazorpayPaymentId() == null || request.getRazorpayPaymentId().isBlank()) {
            throw new RuntimeException("Razorpay payment ID is required");
        }

        if (request.getRazorpaySignature() == null || request.getRazorpaySignature().isBlank()) {
            throw new RuntimeException("Razorpay signature is required");
        }

        PaymentTransaction transaction = paymentTransactionRepository.findByRazorpayOrderId(request.getRazorpayOrderId()).orElseThrow(() -> new RuntimeException("Payment transaction not found"));

        if ("SUCCESS".equals(transaction.getStatus())) {
            throw new PaymentAlreadyVerifiedException("Payment already verified");
        }

        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        boolean isValid = Utils.verifySignature(payload, request.getRazorpaySignature(), razorpayConfig.getKeySecret());

        if (!isValid) {
            throw new InvalidPaymentSignatureException("Invalid payment signature");
        }

        EmiSchedule emiSchedule = transaction.getEmiSchedule();

        if ("PAID".equals(emiSchedule.getPaymentStatus())) {
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

        emiSchedule.setPaymentStatus("PAID");
        emiSchedule.setPaidDate(LocalDate.now());

        emiScheduleRepository.save(emiSchedule);

        BigDecimal currentTotalPaid = loanAccount.getTotalPaidAmount();

        if (currentTotalPaid == null) {
            currentTotalPaid = BigDecimal.ZERO;
        }

        loanAccount.setTotalPaidAmount(currentTotalPaid.add(paymentAmount));
        BigDecimal currentOutstandingPrincipal = loanAccount.getOutstandingPrincipal();

        if (currentOutstandingPrincipal == null) {
            currentOutstandingPrincipal = BigDecimal.ZERO;
        }

        loanAccount.setOutstandingPrincipal(currentOutstandingPrincipal.subtract(principalAmount));
        loanAccountRepository.save(loanAccount);
        transaction.setRazorpayPaymentId(request.getRazorpayPaymentId());
        transaction.setRazorpaySignature(request.getRazorpaySignature());
        transaction.setStatus("SUCCESS");
        paymentTransactionRepository.save(transaction);
    }


    @Override
    public List<PaymentHistoryResponse> getPaymentHistory(Long loanAccountId) {

        List<LoanPayment> payments = loanPaymentRepository.findByLoanAccount_Id(loanAccountId);

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

        return response;
    }

}
