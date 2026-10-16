package com.example.LoanMatrix.service.PaymentService;

import com.example.LoanMatrix.dto.Payment.CreatePaymentRequest;
import com.example.LoanMatrix.dto.Payment.PaymentHistoryResponse;
import com.example.LoanMatrix.dto.Payment.PaymentOrderResponse;
import com.example.LoanMatrix.dto.Payment.VerifyPaymentRequest;
import com.example.LoanMatrix.entity.LoanPayment;
import com.razorpay.RazorpayException;

import java.util.List;

public interface PaymentService {

    PaymentOrderResponse createPaymentOrder(CreatePaymentRequest request) throws RazorpayException;
    void verifyPayment(VerifyPaymentRequest request) throws RazorpayException;
    List<PaymentHistoryResponse> getPaymentHistory(Long loanAccountId);
    PaymentOrderResponse createPartialClosurePaymentOrder(Long requestId) throws RazorpayException;
    void verifyPartialClosurePayment(VerifyPaymentRequest request) throws RazorpayException;
    PaymentOrderResponse createFullClosurePaymentOrder(Long requestId) throws RazorpayException;
    void verifyFullClosurePayment(VerifyPaymentRequest request) throws RazorpayException;
}
