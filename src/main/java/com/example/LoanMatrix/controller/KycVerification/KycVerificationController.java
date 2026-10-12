package com.example.LoanMatrix.controller.KycVerification;


import com.example.LoanMatrix.dto.KycVerification.KycDocumentResponse;
import com.example.LoanMatrix.dto.KycVerification.KycUploadRequest;
import com.example.LoanMatrix.entity.KycDocument;
import com.example.LoanMatrix.response.ApiResponse;
import com.example.LoanMatrix.service.KycVerification.KycVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/kyc")
public class KycVerificationController {

    private final KycVerificationService kycVerificationService;

    public KycVerificationController(KycVerificationService kycVerificationService) {
        this.kycVerificationService = kycVerificationService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<KycDocumentResponse>> uploadDocument(
            @ModelAttribute KycUploadRequest request) {

        KycDocument document = kycVerificationService.uploadDocument(
                request.getCustomerId(),
                request.getDocumentType(),
                request.getFile());

        KycDocumentResponse dto = convertToResponse(document);

        ApiResponse<KycDocumentResponse> response =
                ApiResponse.<KycDocumentResponse>builder()
                        .success(true)
                        .message("KYC document uploaded successfully")
                        .data(dto)
                        .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<KycDocumentResponse>>> getCustomerDocuments(
            @PathVariable Long customerId) {

        List<KycDocument> documents =
                kycVerificationService.getCustomerDocuments(customerId);

        List<KycDocumentResponse> responseList = new ArrayList<>();

        for (KycDocument document : documents) {
            responseList.add(convertToResponse(document));
        }

        ApiResponse<List<KycDocumentResponse>> response =
                ApiResponse.<List<KycDocumentResponse>>builder()
                        .success(true)
                        .message("KYC documents fetched successfully")
                        .data(responseList)
                        .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<KycDocumentResponse>>> getPendingDocuments() {

        List<KycDocument> documents =
                kycVerificationService.getPendingDocuments();

        List<KycDocumentResponse> responseList = new ArrayList<>();

        for (KycDocument document : documents) {
            responseList.add(convertToResponse(document));
        }

        ApiResponse<List<KycDocumentResponse>> response =
                ApiResponse.<List<KycDocumentResponse>>builder()
                        .success(true)
                        .message("Pending KYC documents fetched successfully")
                        .data(responseList)
                        .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{documentId}/approve")
    public ResponseEntity<ApiResponse<KycDocumentResponse>> approveDocument(
            @PathVariable Long documentId) {

        KycDocument document =
                kycVerificationService.approveDocument(documentId);

        KycDocumentResponse dto = convertToResponse(document);

        ApiResponse<KycDocumentResponse> response =
                ApiResponse.<KycDocumentResponse>builder()
                        .success(true)
                        .message("KYC document approved successfully")
                        .data(dto)
                        .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{documentId}/reject")
    public ResponseEntity<ApiResponse<KycDocumentResponse>> rejectDocument(
            @PathVariable Long documentId) {

        KycDocument document =
                kycVerificationService.rejectDocument(documentId);

        KycDocumentResponse dto = convertToResponse(document);

        ApiResponse<KycDocumentResponse> response =
                ApiResponse.<KycDocumentResponse>builder()
                        .success(true)
                        .message("KYC document rejected successfully")
                        .data(dto)
                        .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/document/{documentId}")
    public ResponseEntity<byte[]> viewDocument(
            @PathVariable Long documentId) {
        byte[] file = kycVerificationService.viewDocument(documentId);
        return ResponseEntity.ok(file);
    }

    private KycDocumentResponse convertToResponse(KycDocument document) {

        return new KycDocumentResponse(
                document.getId(),
                document.getDocumentType(),
                document.getVerificationStatus()
        );
    }
}