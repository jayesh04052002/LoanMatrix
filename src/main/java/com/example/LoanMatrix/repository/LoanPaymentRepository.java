package com.example.LoanMatrix.repository;

import com.example.LoanMatrix.entity.Customer;
import com.example.LoanMatrix.entity.LoanPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanPaymentRepository extends JpaRepository<LoanPayment, Long> {

    List<LoanPayment> findByLoanAccount_Id(Long loanAccountId);

    List<LoanPayment> findByLoanAccount_CustomerOrderByPaymentDateDesc(Customer customer);

    List<LoanPayment> findByLoanAccount_IdOrderByPaymentDateDesc(Long loanAccountId);
}
