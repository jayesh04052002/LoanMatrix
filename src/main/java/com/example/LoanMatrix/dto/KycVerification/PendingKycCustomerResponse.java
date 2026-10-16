package com.example.LoanMatrix.dto.KycVerification;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PendingKycCustomerResponse {

    private Long customerId;
    private String customerName;
    private String email;
    private String mobileNo;
}
