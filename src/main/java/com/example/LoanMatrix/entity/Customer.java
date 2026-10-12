package com.example.LoanMatrix.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CustomerId")
    private Long id;

    @Column(name = "FirstName", nullable = false)
    private String firstName;

    @Column(name = "LastName", nullable = false)
    private String lastName;

    @Column(name = "Email", nullable = false, unique = true)
    private String email;

    @Column(name = "Password", nullable = false)
    private String password;

    @Column(name = "MobileNo", nullable = false)
    private String mobileNo;

    @Column(name = "AadhaarNo", nullable = false, unique = true)
    private String aadhaarNo;

    @Column(name = "PanCard", unique = true)
    private String panCard;

    @Column(name = "DateOfBirth")
    private LocalDate dateOfBirth;

    @Column(name = "EmploymentType")
    private String employmentType;

    @Column(name = "MonthlyIncome", precision = 18, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(name = "MonthlyInvestmentAmount", precision = 18, scale = 2)
    private BigDecimal monthlyInvestmentAmount;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;
}
