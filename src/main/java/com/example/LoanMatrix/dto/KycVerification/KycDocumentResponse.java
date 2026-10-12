package com.example.LoanMatrix.dto.KycVerification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KycDocumentResponse {

    private Long id;
    private String documentType;
    private String verificationStatus;
}