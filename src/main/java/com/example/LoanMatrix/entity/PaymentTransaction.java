package com.example.LoanMatrix.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PaymentTransactionId")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LoanAccountId", nullable = false)
    private LoanAccount loanAccount;

    // EMI payment relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emi_schedule_id", nullable = true)
    private EmiSchedule emiSchedule;

    // Partial closure payment relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fore_closure_request_id")
    private ForeClosureRequest foreClosureRequest;

    @Column(name = "RazorpayOrderId", nullable = false, unique = true)
    private String razorpayOrderId;

    @Column(name = "RazorpayPaymentId")
    private String razorpayPaymentId;

    @Column(name = "RazorpaySignature")
    private String razorpaySignature;

    @Column(name = "Amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "Status")
    private String status;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;
}