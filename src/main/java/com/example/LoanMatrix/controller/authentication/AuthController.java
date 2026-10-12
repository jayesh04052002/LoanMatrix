package com.example.LoanMatrix.controller.authentication;


import com.example.LoanMatrix.dto.authentication.CustomerRegistrationRequest;
import com.example.LoanMatrix.dto.authentication.LoginRequest;
import com.example.LoanMatrix.dto.authentication.OtpVerificationRequest;
import com.example.LoanMatrix.entity.Customer;
import com.example.LoanMatrix.response.ApiResponse;
import com.example.LoanMatrix.response.CustomerRegistrationResponse;
import com.example.LoanMatrix.response.LoginResponse;
import com.example.LoanMatrix.service.authentication.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CustomerRegistrationResponse>> registerCustomer(
            @Valid @RequestBody CustomerRegistrationRequest request) {

        Customer customer = authService.registerCustomer(request);

        CustomerRegistrationResponse registrationResponse =
                CustomerRegistrationResponse.builder()
                        .id(customer.getId())
                        .firstName(customer.getFirstName())
                        .lastName(customer.getLastName())
                        .email(customer.getEmail())
                        .mobileNo(customer.getMobileNo())
                        .aadhaarNo(customer.getAadhaarNo())
                        .panCard(customer.getPanCard())
                        .employmentType(customer.getEmploymentType())
                        .build();

        ApiResponse<CustomerRegistrationResponse> response =
                ApiResponse.<CustomerRegistrationResponse>builder()
                        .success(true)
                        .message("Customer registered successfully")
                        .data(registrationResponse)
                        .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyOtp(
            @Valid @RequestBody OtpVerificationRequest request) {

        authService.verifyOtp(request);

        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .message("Email verified successfully")
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse loginResponse = authService.login(request);

        ApiResponse<LoginResponse> response =
                ApiResponse.<LoginResponse>builder()
                        .success(true)
                        .message("Login successful")
                        .data(loginResponse)
                        .build();

        return ResponseEntity.ok(response);
    }
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(
            @RequestBody Map<String, String> request) {

        System.out.println("Google login controller reached");

        String idToken = request.get("idToken");

        if (idToken == null || idToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Google ID token is required"
            );
        }

        LoginResponse loginResponse = authService.googleLogin(idToken);

        // Keep your existing response-building code here

        ApiResponse<LoginResponse> response =
                ApiResponse.<LoginResponse>builder()
                        .success(true)
                        .message("Google login successful")
                        .data(loginResponse)
                        .build();

        return ResponseEntity.ok(response);
    }

}