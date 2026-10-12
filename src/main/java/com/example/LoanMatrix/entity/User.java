package com.example.LoanMatrix.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserId")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RoleId", nullable = false)
    private Role role;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CustomerId", unique = true)
    private Customer customer;

    @Column(name = "FirstName", nullable = false)
    private String firstName;

    @Column(name = "LastName", nullable = false)
    private String lastName;

    @Column(name = "Email", nullable = false, unique = true)
    private String email;

    @Column(name = "Mobile")
    private String mobile;

    @Column(name = "Password", nullable = false)
    private String password;

    @Column(name = "Otp")
    private String otp;

    @Column(name = "OtpExpiry")
    private LocalDateTime otpExpiry;

    @Column(name = "IsEmailVerified")
    private Boolean emailVerified;
}