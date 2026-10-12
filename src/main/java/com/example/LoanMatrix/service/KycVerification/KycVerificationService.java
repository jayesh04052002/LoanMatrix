package com.example.LoanMatrix.service.KycVerification;

import com.example.LoanMatrix.entity.KycDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KycVerificationService {


    KycDocument uploadDocument(Long customerId, String documentType, MultipartFile file);

    List<KycDocument> getCustomerDocuments(Long customerId);

    List<KycDocument> getPendingDocuments();

    KycDocument approveDocument(Long documentId);

    KycDocument rejectDocument(Long documentId);

    byte[] viewDocument(Long documentId);
}