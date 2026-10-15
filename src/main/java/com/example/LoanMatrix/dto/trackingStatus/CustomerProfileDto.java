package com.example.LoanMatrix.dto.trackingStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerProfileDto {

    private Long customerId;
    private String customerCode;
    private String fullName;
    private String email;
    private boolean kycVerified;
}
