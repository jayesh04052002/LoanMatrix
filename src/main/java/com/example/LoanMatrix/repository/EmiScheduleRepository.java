package com.example.LoanMatrix.repository;

import com.example.LoanMatrix.entity.Customer;
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

    Optional<EmiSchedule> findByIdAndLoanAccount_Id(Long emiScheduleId, Long loanAccountId);

    List<EmiSchedule> findByLoanAccount_Id(Long loanAccountId);

    long countByPaymentStatus(String paymentStatus);

    List<EmiSchedule> findByLoanAccount_Customer(Customer customer);
}