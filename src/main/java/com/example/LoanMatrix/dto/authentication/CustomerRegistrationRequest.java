package com.example.LoanMatrix.dto.authentication;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CustomerRegistrationRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must contain at least 8 characters")
    private String password;

    @NotBlank(message = "Mobile number is required")
    @Pattern(
            regexp = "^[6-9][0-9]{9}$",
            message = "Enter a valid 10-digit mobile number"
    )
    private String mobileNo;

    @NotBlank(message = "Aadhaar number is required")
    @Pattern(
            regexp = "^[0-9]{12}$",
            message = "Aadhaar number must contain 12 digits"
    )
    private String aadhaarNo;

    @NotBlank(message = "PAN card number is required")
    @Pattern(
            regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$",
            message = "Enter a valid PAN number"
    )
    private String panCard;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Employment type is required")
    private String employmentType;

    @NotNull(message = "Monthly income is required")
    @Positive(message = "Monthly income must be greater than 0")
    private BigDecimal monthlyIncome;

    @PositiveOrZero(message = "Monthly investment amount cannot be negative")
    private BigDecimal monthlyInvestmentAmount;

}
