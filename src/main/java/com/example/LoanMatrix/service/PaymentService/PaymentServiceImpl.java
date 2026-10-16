package com.example.LoanMatrix.service.PaymentService;

import com.example.LoanMatrix.config.RazorpayConfig;
import com.example.LoanMatrix.dto.Payment.CreatePaymentRequest;
import com.example.LoanMatrix.dto.Payment.PaymentHistoryResponse;
import com.example.LoanMatrix.dto.Payment.PaymentOrderResponse;
import com.example.LoanMatrix.dto.Payment.VerifyPaymentRequest;
import com.example.LoanMatrix.entity.*;
import com.example.LoanMatrix.exception.*;
import com.example.LoanMatrix.repository.EmiScheduleRepository;
import com.example.LoanMatrix.repository.ForeClosureRequestRepository;
import com.example.LoanMatrix.repository.LoanAccountRepository;
import com.example.LoanMatrix.repository.LoanPaymentRepository;
import com.example.LoanMatrix.repository.Payment.PaymentTransactionRepository;
import com.example.LoanMatrix.repository.closure.LoanClosureRepository;
import com.example.LoanMatrix.service.EmiScheduler.EmiSchedulerService;
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
    private final LoanAccountRepository loanAccountRepository;
    private final RazorpayConfig razorpayConfig;
    private final PenaltyService penaltyService;
    private final LoanClosureRepository loanClosureRepository;
    private final ForeClosureRequestRepository foreClosureRequestRepository;
    private final EmiSchedulerService emiSchedulerService;

    private Order createOrder(BigDecimal amount, String receipt) throws RazorpayException {

        RazorpayClient client = new RazorpayClient(razorpayConfig.getKeyId(), razorpayConfig.getKeySecret());

        JSONObject data = new JSONObject();
        data.put("amount", amount.movePointRight(2).intValueExact());
        data.put("currency", "INR");
        data.put("receipt", receipt);

        return client.orders.create(data);
    }

    @Override
    public PaymentOrderResponse createPaymentOrder(CreatePaymentRequest request) throws RazorpayException {

        LoanAccount loanAccount = loanAccountRepository.findById(request.getLoanAccountId()).orElseThrow(() -> new LoanAccountNotFoundException("Loan account not found"));
        EmiSchedule emiSchedule = emiScheduleRepository.findByIdAndLoanAccount_Id(request.getEmiScheduleId(), loanAccount.getId()).orElseThrow(() -> new EmiNotFoundException("EMI schedule not found for this loan account"));

        if ("PAID".equalsIgnoreCase(emiSchedule.getPaymentStatus())) {
            throw new EmiAlreadyPaidException("EMI is already paid");
        }

        BigDecimal emiAmount = emiSchedule.getEmi() == null ? BigDecimal.ZERO : emiSchedule.getEmi();
        BigDecimal penaltyAmount = penaltyService.calculateAndUpdatePenalty(emiSchedule);
        BigDecimal payableAmount = emiAmount.add(penaltyAmount);

        Order order = createOrder(payableAmount, "EMI_" + request.getEmiScheduleId());

        String orderId = order.get("id");

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setLoanAccount(loanAccount);
        transaction.setEmiSchedule(emiSchedule);
        transaction.setRazorpayOrderId(orderId);
        transaction.setAmount(payableAmount);
        transaction.setStatus("CREATED");
        transaction.setCreatedAt(LocalDateTime.now());

        paymentTransactionRepository.save(transaction);

        return new PaymentOrderResponse(orderId, payableAmount, "INR", razorpayConfig.getKeyId());
    }

    @Override
    @Transactional
    public void verifyPayment(VerifyPaymentRequest request) throws RazorpayException {

        if (request == null || request.getRazorpayOrderId() == null || request.getRazorpayOrderId().isBlank() || request.getRazorpayPaymentId() == null || request.getRazorpayPaymentId().isBlank() || request.getRazorpaySignature() == null || request.getRazorpaySignature().isBlank()) {
            throw new RuntimeException("Payment details are required");
        }

        PaymentTransaction transaction = paymentTransactionRepository.findByRazorpayOrderId(request.getRazorpayOrderId()).orElseThrow(() -> new RuntimeException("Payment transaction not found"));
        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        boolean valid = Utils.verifySignature(payload, request.getRazorpaySignature(), razorpayConfig.getKeySecret());

        if (!valid) {
            throw new InvalidPaymentSignatureException("Invalid payment signature");
        }

        if ("SUCCESS".equalsIgnoreCase(transaction.getStatus())) {
            throw new PaymentAlreadyVerifiedException("Payment already verified");
        }

        EmiSchedule emiSchedule = transaction.getEmiSchedule();

        if ("PAID".equalsIgnoreCase(emiSchedule.getPaymentStatus())) {
            throw new EmiAlreadyPaidException("EMI is already paid");
        }

        LoanAccount loanAccount = transaction.getLoanAccount();
        BigDecimal paymentAmount = transaction.getAmount();
        BigDecimal principalAmount = emiSchedule.getPrincipalAmount();

        LoanPayment payment = new LoanPayment();
        payment.setLoanAccount(loanAccount);
        payment.setPaymentAmount(paymentAmount);
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentStatus("SUCCESS");
        payment.setPaymentName("EMI Payment");

        loanPaymentRepository.save(payment);

        emiSchedule.setPaymentStatus("PAID");
        emiSchedule.setPaidDate(LocalDate.now());
        emiScheduleRepository.save(emiSchedule);

        BigDecimal totalPaid = loanAccount.getTotalPaidAmount();

        if (totalPaid == null) {
            totalPaid = BigDecimal.ZERO;
        }

        loanAccount.setTotalPaidAmount(totalPaid.add(paymentAmount));
        BigDecimal outstanding = loanAccount.getOutstandingPrincipal();

        if (outstanding == null) {
            outstanding = BigDecimal.ZERO;
        }

        loanAccount.setOutstandingPrincipal(outstanding.subtract(principalAmount));
        loanAccountRepository.save(loanAccount);

        transaction.setRazorpayPaymentId(request.getRazorpayPaymentId());
        transaction.setRazorpaySignature(request.getRazorpaySignature());
        transaction.setStatus("SUCCESS");
        emiSchedulerService.recalculateSchedule(loanAccount.getId());

        paymentTransactionRepository.save(transaction);
    }

    // PAYMENT HISTORY

    @Override
    public List<PaymentHistoryResponse> getPaymentHistory(Long loanAccountId) {

        List<LoanPayment> payments = loanPaymentRepository.findByLoanAccount_Id(loanAccountId);
        List<PaymentHistoryResponse> response = new ArrayList<>();

        for (LoanPayment payment : payments) {
            response.add(new PaymentHistoryResponse(payment.getId(), payment.getPaymentAmount(), payment.getPaymentDate(), payment.getPaymentStatus(), payment.getPaymentName()));
        }
        return response;
    }

    // PARTIAL FORECLOSURE PAYMENT

    @Override
    public PaymentOrderResponse createPartialClosurePaymentOrder(Long requestId) throws RazorpayException {

        ForeClosureRequest request = foreClosureRequestRepository.findById(requestId).orElseThrow(() -> new RuntimeException("Foreclosure request not found"));

        if (!"Approved".equalsIgnoreCase(request.getStatus())) {
            throw new RuntimeException("Only approved foreclosure requests can be paid");
        }

        if (Boolean.TRUE.equals(request.getPaid())) {
            throw new RuntimeException("Foreclosure request is already paid");
        }

        BigDecimal amount = request.getPartialAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid partial payment amount");
        }

        Order order = createOrder(amount, "PARTIAL_" + requestId);
        String orderId = order.get("id");

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setLoanAccount(request.getLoanAccount());
        transaction.setEmiSchedule(null);
        transaction.setForeClosureRequest(request);
        transaction.setRazorpayOrderId(orderId);
        transaction.setAmount(amount);
        transaction.setStatus("CREATED");
        transaction.setCreatedAt(LocalDateTime.now());

        paymentTransactionRepository.save(transaction);

        return new PaymentOrderResponse(orderId, amount, "INR", razorpayConfig.getKeyId());
    }

    @Override
    @Transactional
    public void verifyPartialClosurePayment(VerifyPaymentRequest request) throws RazorpayException {

        if (request == null || request.getRazorpayOrderId() == null || request.getRazorpayOrderId().isBlank() || request.getRazorpayPaymentId() == null || request.getRazorpayPaymentId().isBlank() || request.getRazorpaySignature() == null || request.getRazorpaySignature().isBlank()) {

            throw new RuntimeException("Payment details are required");
        }

        PaymentTransaction transaction = paymentTransactionRepository.findByRazorpayOrderId(request.getRazorpayOrderId()).orElseThrow(() -> new RuntimeException("Transaction not found"));

        ForeClosureRequest closureRequest = transaction.getForeClosureRequest();

        if (closureRequest == null || transaction.getEmiSchedule() != null) {
            throw new RuntimeException("Invalid partial closure transaction");
        }

        if ("SUCCESS".equalsIgnoreCase(transaction.getStatus()) || Boolean.TRUE.equals(closureRequest.getPaid())) {
            throw new RuntimeException("Payment already processed");
        }

        if (!"CREATED".equalsIgnoreCase(transaction.getStatus())) {
            throw new RuntimeException("Transaction is not awaiting payment");
        }

        if (!"Approved".equalsIgnoreCase(closureRequest.getStatus())) {
            throw new RuntimeException("Foreclosure request is not approved");
        }

        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();

        boolean valid = Utils.verifySignature(payload, request.getRazorpaySignature(), razorpayConfig.getKeySecret());

        if (!valid) {
            throw new InvalidPaymentSignatureException("Invalid signature");
        }

        LoanAccount loanAccount = transaction.getLoanAccount();
        BigDecimal amount = transaction.getAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid payment amount");
        }

        if (closureRequest.getPartialAmount() == null || closureRequest.getPartialAmount().compareTo(amount) != 0) {
            throw new RuntimeException("Partial closure amount mismatch");
        }

        BigDecimal outstanding = loanAccount.getOutstandingPrincipal();

        if (outstanding == null || amount.compareTo(outstanding) > 0) {
            throw new RuntimeException("Insufficient outstanding principal");
        }

        LoanPayment payment = new LoanPayment();
        payment.setLoanAccount(loanAccount);
        payment.setPaymentAmount(amount);
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentStatus("SUCCESS");
        payment.setPaymentName("Partial Closure Payment");

        loanPaymentRepository.save(payment);

        BigDecimal totalPaid = loanAccount.getTotalPaidAmount();

        if (totalPaid == null) {
            totalPaid = BigDecimal.ZERO;
        }

        loanAccount.setTotalPaidAmount(totalPaid.add(amount));
        loanAccount.setOutstandingPrincipal(outstanding.subtract(amount));

        loanAccountRepository.save(loanAccount);

        LoanClosure closure = new LoanClosure();
        closure.setLoanAccount(loanAccount);
        closure.setClosureType("PARTIAL_FORECLOSURE");
        closure.setFinalSettlementAmount(amount);
        closure.setClosureDate(LocalDate.now());
        closure.setClosedBy(closureRequest.getClosedBy());
        closure.setRemarks(closureRequest.getReason() != null ? closureRequest.getReason() : "Partial settlement processed successfully");
        closure.setClosureStatus("PARTIAL_CLOSED");

        loanClosureRepository.save(closure);

        closureRequest.setPaid(true);
        closureRequest.setPaidDate(LocalDate.now());
        closureRequest.setStatus("Completed");

        foreClosureRequestRepository.save(closureRequest);

        transaction.setRazorpayPaymentId(request.getRazorpayPaymentId());
        transaction.setRazorpaySignature(request.getRazorpaySignature());
        transaction.setStatus("SUCCESS");
        emiSchedulerService.recalculateSchedule(loanAccount.getId());
        paymentTransactionRepository.save(transaction);
    }

    // FULL FORECLOSURE PAYMENT

    @Override
    public PaymentOrderResponse createFullClosurePaymentOrder(Long requestId) throws RazorpayException {

        ForeClosureRequest request = foreClosureRequestRepository.findById(requestId).orElseThrow(() -> new RuntimeException("Foreclosure request not found"));

        if (!"Approved".equalsIgnoreCase(request.getStatus())) {
            throw new RuntimeException("Only approved foreclosure requests can be paid");
        }

        if (Boolean.TRUE.equals(request.getPaid())) {
            throw new RuntimeException("Foreclosure request is already paid");
        }

        BigDecimal amount = request.getForeClosureAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid full closure amount");
        }

        Order order = createOrder(amount, "FULL_" + requestId);
        String orderId = order.get("id");

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setLoanAccount(request.getLoanAccount());
        transaction.setEmiSchedule(null);
        transaction.setForeClosureRequest(request);
        transaction.setRazorpayOrderId(orderId);
        transaction.setAmount(amount);
        transaction.setStatus("CREATED");
        transaction.setCreatedAt(LocalDateTime.now());

        paymentTransactionRepository.save(transaction);

        return new PaymentOrderResponse(orderId, amount, "INR", razorpayConfig.getKeyId());
    }

    @Override
    @Transactional
    public void verifyFullClosurePayment(VerifyPaymentRequest request) throws RazorpayException {

        if (request == null || request.getRazorpayOrderId() == null || request.getRazorpayOrderId().isBlank() || request.getRazorpayPaymentId() == null || request.getRazorpayPaymentId().isBlank() || request.getRazorpaySignature() == null || request.getRazorpaySignature().isBlank()) {

            throw new RuntimeException("Payment details are required");
        }

        PaymentTransaction transaction = paymentTransactionRepository.findByRazorpayOrderId(request.getRazorpayOrderId()).orElseThrow(() -> new RuntimeException("Transaction not found"));

        ForeClosureRequest closureRequest = transaction.getForeClosureRequest();

        if (closureRequest == null || transaction.getEmiSchedule() != null || !"FULL".equalsIgnoreCase(closureRequest.getForeClosureType())) {

            throw new RuntimeException("Invalid full closure transaction");
        }

        if ("SUCCESS".equalsIgnoreCase(transaction.getStatus()) || Boolean.TRUE.equals(closureRequest.getPaid())) {
            throw new RuntimeException("Payment already processed");
        }

        if (!"CREATED".equalsIgnoreCase(transaction.getStatus())) {
            throw new RuntimeException("Transaction is not awaiting payment");
        }

        if (!"Approved".equalsIgnoreCase(closureRequest.getStatus())) {
            throw new RuntimeException("Full closure request is not approved");
        }

        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();

        boolean valid = Utils.verifySignature(payload, request.getRazorpaySignature(), razorpayConfig.getKeySecret());

        if (!valid) {
            throw new InvalidPaymentSignatureException("Invalid signature");
        }

        BigDecimal amount = transaction.getAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || closureRequest.getForeClosureAmount() == null || amount.compareTo(closureRequest.getForeClosureAmount()) != 0) {

            throw new RuntimeException("Full closure amount mismatch");
        }

        LoanAccount loanAccount = transaction.getLoanAccount();

        LoanPayment payment = new LoanPayment();
        payment.setLoanAccount(loanAccount);
        payment.setPaymentAmount(amount);
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentStatus("SUCCESS");
        payment.setPaymentName("Full Closure Payment");

        loanPaymentRepository.save(payment);

        BigDecimal totalPaid = loanAccount.getTotalPaidAmount();

        if (totalPaid == null) {
            totalPaid = BigDecimal.ZERO;
        }

        loanAccount.setTotalPaidAmount(totalPaid.add(amount));
        loanAccount.setOutstandingPrincipal(BigDecimal.ZERO);
        loanAccount.setLoanStatus("CLOSED");

        loanAccountRepository.save(loanAccount);

        LoanClosure closure = new LoanClosure();
        closure.setLoanAccount(loanAccount);
        closure.setClosureType("FORECLOSURE");
        closure.setFinalSettlementAmount(amount);
        closure.setClosureDate(LocalDate.now());
        closure.setClosedBy(closureRequest.getClosedBy());
        closure.setRemarks(closureRequest.getReason() != null ? closureRequest.getReason() : "Full settlement processed via foreclosure");
        closure.setClosureStatus("CLOSED");

        loanClosureRepository.save(closure);

        closureRequest.setPaid(true);
        closureRequest.setPaidDate(LocalDate.now());
        closureRequest.setStatus("Completed");

        foreClosureRequestRepository.save(closureRequest);

        transaction.setRazorpayPaymentId(request.getRazorpayPaymentId());
        transaction.setRazorpaySignature(request.getRazorpaySignature());
        transaction.setStatus("SUCCESS");
        emiSchedulerService.recalculateSchedule(loanAccount.getId());
        paymentTransactionRepository.save(transaction);
    }
}
