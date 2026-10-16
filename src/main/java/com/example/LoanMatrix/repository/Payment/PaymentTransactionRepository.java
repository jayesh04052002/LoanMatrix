package com.example.LoanMatrix.repository.Payment;

import com.example.LoanMatrix.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction , Long> {

    Optional<PaymentTransaction> findByRazorpayOrderId(String razorpayOrderId);

    Optional<PaymentTransaction> findByRazorpayPaymentId(String razorpayPaymentId);

    List<PaymentTransaction> findByEmiSchedule_IdAndStatus(Long emiScheduleId, String status);

    List<PaymentTransaction> findByForeClosureRequest_Id(Long requestId);
}
