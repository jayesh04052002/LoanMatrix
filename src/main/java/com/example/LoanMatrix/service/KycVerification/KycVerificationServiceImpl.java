package com.example.LoanMatrix.service.KycVerification;

import com.example.LoanMatrix.entity.Customer;
import com.example.LoanMatrix.entity.KycDocument;
import com.example.LoanMatrix.exception.CustomerNotFoundException;
import com.example.LoanMatrix.exception.ResourceNotFoundException;
import com.example.LoanMatrix.exception.FileStorageException;

import com.example.LoanMatrix.repository.KycDocumentRepository.KycDocumentRepository;
import com.example.LoanMatrix.repository.customer.CustomerRepository;
import com.example.LoanMatrix.service.Email.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class KycVerificationServiceImpl implements KycVerificationService {

    private final KycDocumentRepository kycDocumentRepository;
    private final CustomerRepository customerRepository;
    private final EmailService emailService;

    public KycVerificationServiceImpl(KycDocumentRepository kycDocumentRepository,
                                      CustomerRepository customerRepository,
                                      EmailService emailService) {
        this.kycDocumentRepository = kycDocumentRepository;
        this.customerRepository = customerRepository;
        this.emailService = emailService;
    }

    @Override
    public KycDocument uploadDocument(Long customerId, String documentType, MultipartFile file) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

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

            return kycDocumentRepository.save(document);

        } catch (IOException e) {
            throw new FileStorageException("Failed to upload document");
        }
    }

    @Override
    public List<KycDocument> getCustomerDocuments(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

        return kycDocumentRepository.findByCustomerId(customer.getId());
    }

    @Override
    public List<KycDocument> getPendingDocuments() {
        return kycDocumentRepository.findByVerificationStatus("PENDING");
    }

    @Override
    public KycDocument approveDocument(Long documentId) {

        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC document not found"));

        document.setVerificationStatus("APPROVED");
        KycDocument savedDocument = kycDocumentRepository.save(document);

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

        return savedDocument;
    }

    @Override
    public KycDocument rejectDocument(Long documentId) {

        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC document not found"));

        document.setVerificationStatus("REJECTED");
        KycDocument savedDocument = kycDocumentRepository.save(document);

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

        return savedDocument;
    }

    @Override
    public byte[] viewDocument(Long documentId) {

        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC document not found"));

        try {
            Path path = Paths.get(document.getFilePath());
            return Files.readAllBytes(path);

        } catch (IOException e) {
            throw new FileStorageException("Unable to view document");
        }
    }
}