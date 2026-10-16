package com.example.LoanMatrix.service.KycVerification;

import com.example.LoanMatrix.dto.KycVerification.PendingKycCustomerResponse;
import com.example.LoanMatrix.entity.Customer;
import com.example.LoanMatrix.entity.KycDocument;
import com.example.LoanMatrix.exception.CustomerNotFoundException;
import com.example.LoanMatrix.exception.ResourceNotFoundException;
import com.example.LoanMatrix.exception.FileStorageException;

import com.example.LoanMatrix.repository.CustomerRepository;
import com.example.LoanMatrix.repository.KycDocumentRepository;
import com.example.LoanMatrix.service.Email.EmailService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class KycVerificationServiceImpl implements KycVerificationService {

    private final KycDocumentRepository kycDocumentRepository;
    private final CustomerRepository customerRepository;
    private final EmailService emailService;

    public KycVerificationServiceImpl(
            KycDocumentRepository kycDocumentRepository,
            CustomerRepository customerRepository,
            EmailService emailService) {

        this.kycDocumentRepository = kycDocumentRepository;
        this.customerRepository = customerRepository;
        this.emailService = emailService;
    }

    @Override
    public KycDocument uploadDocument(
            Long customerId, String documentType, MultipartFile file) {

        log.info("Starting KYC document upload for Customer ID: {}, Document Type: {}",
                customerId, documentType);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.warn("Customer not found for KYC document upload. Customer ID: {}",
                            customerId);
                    return new CustomerNotFoundException("Customer not found");
                });

        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

            Path uploadPath = Paths.get("src/main/resources/uploads");
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, file.getBytes());

            KycDocument document = new KycDocument();
            document.setCustomer(customer);
            document.setDocumentType(documentType);
            document.setFilePath(filePath.toString());
            document.setVerificationStatus("PENDING");

            KycDocument savedDocument = kycDocumentRepository.save(document);

            log.info("KYC document uploaded successfully. Document ID: {}, Customer ID: {}",
                    savedDocument.getId(), customerId);

            return savedDocument;

        } catch (IOException e) {
            log.error("Failed to store KYC document for Customer ID: {}", customerId, e);
            throw new FileStorageException("Failed to upload document");
        }
    }

    @Override
    public List<KycDocument> getCustomerDocuments(Long customerId) {
        log.info("Fetching KYC documents for Customer ID: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.warn("Customer not found while fetching KYC documents. Customer ID: {}",
                            customerId);
                    return new CustomerNotFoundException("Customer not found");
                });

        List<KycDocument> documents =
                kycDocumentRepository.findByCustomerId(customer.getId());

        log.info("KYC documents fetched for Customer ID: {}. Count: {}",
                customerId, documents.size());

        return documents;
    }

    @Override
    public List<KycDocument> getPendingDocuments() {
        log.info("Fetching pending KYC documents");

        List<KycDocument> documents =
                kycDocumentRepository.findByVerificationStatus("PENDING");

        log.info("Pending KYC documents fetched. Count: {}", documents.size());

        return documents;
    }

    @Override
    public KycDocument approveDocument(Long documentId) {
        log.info("Approving KYC document. Document ID: {}", documentId);

        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> {
                    log.warn("KYC document not found for approval. Document ID: {}",
                            documentId);
                    return new ResourceNotFoundException("KYC document not found");
                });

        document.setVerificationStatus("APPROVED");
        KycDocument savedDocument = kycDocumentRepository.save(document);

        log.info("KYC document approved and saved. Document ID: {}", documentId);

        emailService.sendKycStatusEmail(
                document.getCustomer().getEmail(),
                "KYC Document Approved",
                """
                <h2 style="color:green;">KYC Document Approved</h2>
                <p>Hello,</p>
                <p>Your KYC document has been <b>approved successfully.</b></p>
                <p>Thank you!</p>
                """
        );

        log.info("KYC approval email request completed. Document ID: {}", documentId);

        return savedDocument;
    }

    @Override
    public KycDocument rejectDocument(Long documentId) {
        log.info("Rejecting KYC document. Document ID: {}", documentId);

        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> {
                    log.warn("KYC document not found for rejection. Document ID: {}",
                            documentId);
                    return new ResourceNotFoundException("KYC document not found");
                });

        document.setVerificationStatus("REJECTED");
        KycDocument savedDocument = kycDocumentRepository.save(document);

        log.info("KYC document rejected and saved. Document ID: {}", documentId);

        emailService.sendKycStatusEmail(
                document.getCustomer().getEmail(),
                "KYC Document Rejected",
                """
                <h2 style="color:red;">KYC Document Rejected</h2>
                <p>Hello,</p>
                <p>Your KYC document has been <b>rejected.</b></p>
                <p>Please upload a valid document.</p>
                <p>Thank you!</p>
                """
        );

        log.info("KYC rejection email request completed. Document ID: {}", documentId);

        return savedDocument;
    }

    @Override
    public byte[] viewDocument(Long documentId) {
        log.info("Request to view KYC document. Document ID: {}", documentId);

        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> {
                    log.warn("KYC document not found for viewing. Document ID: {}",
                            documentId);
                    return new ResourceNotFoundException("KYC document not found");
                });

        try {
            Path path = Paths.get(document.getFilePath());
            byte[] documentContent = Files.readAllBytes(path);

            log.info("KYC document read successfully. Document ID: {}", documentId);

            return documentContent;

        } catch (IOException e) {
            log.error("Unable to read KYC document. Document ID: {}", documentId, e);
            throw new FileStorageException("Unable to view document");
        }
    }


    @Override
    public List<PendingKycCustomerResponse> getCustomersWithPendingKyc() {

        List<KycDocument> documents =
                kycDocumentRepository.findByVerificationStatus("PENDING");

        List<PendingKycCustomerResponse> customers = new ArrayList<>();

        for (KycDocument document : documents) {

            Customer customer = document.getCustomer();

            boolean alreadyAdded = false;

            for (PendingKycCustomerResponse existing : customers) {
                if (existing.getCustomerId().equals(customer.getId())) {
                    alreadyAdded = true;
                    break;
                }
            }

            if (!alreadyAdded) {
                customers.add(new PendingKycCustomerResponse(
                        customer.getId(),
                        customer.getFirstName() + " " + customer.getLastName(),
                        customer.getEmail(),
                        customer.getMobileNo()
                ));
            }
        }

        return customers;
    }
}
