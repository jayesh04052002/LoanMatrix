package com.example.LoanMatrix.repository;

import com.example.LoanMatrix.entity.ForeClosureRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ForeClosureRequestRepository extends JpaRepository<ForeClosureRequest, Long> {

    // Checks if any active (unpaid) request exists in specified statuses
    boolean existsByLoanAccount_IdAndStatusInAndPaidFalse(Long loanAccountId, Collection<String> statuses);

    List<ForeClosureRequest> findByStatusOrderByRequestedDateDesc(String status);

    List<ForeClosureRequest> findByLoanAccount_IdOrderByRequestedDateDesc(Long loanAccountId);

    long countByStatus(String status);
}