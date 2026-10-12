package com.example.LoanMatrix.dto.KycVerification;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class KycUploadRequest {

    private Long customerId;
    private String documentType;
    private MultipartFile file;
}