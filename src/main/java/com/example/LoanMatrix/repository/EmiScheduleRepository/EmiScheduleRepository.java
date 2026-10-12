package com.example.LoanMatrix.repository.EmiScheduleRepository;

import com.example.LoanMatrix.entity.EmiSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmiScheduleRepository extends JpaRepository<EmiSchedule, Long> {

    List<EmiSchedule> findByLoanAccountIdOrderByInstallmentNoAsc(Long loanAccountId);

    boolean existsByLoanAccountId(Long loanAccountId);

    Optional<EmiSchedule> findByLoanAccountIdAndInstallmentNo(
            Long loanAccountId,
            Integer installmentNo
    );
}