package com.example.LoanMatrix.repository.KycDocumentRepository;



import com.example.LoanMatrix.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    List<KycDocument> findByCustomerId(Long customerId);

    List<KycDocument> findByVerificationStatus(String verificationStatus);
}