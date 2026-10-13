package com.example.LoanMatrix.repository;

import com.example.LoanMatrix.entity.Customer;
import com.example.LoanMatrix.entity.LoanAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanAccountRepository extends JpaRepository<LoanAccount, Long> {

    Optional<LoanAccount> findByIdAndCustomer_Id(
            Long id,
            Long customerId
    );

    long countDistinctCustomerBy();

    long countByLoanStatus(String loanStatus);

    List<LoanAccount> findByCustomerAndLoanStatus(
            Customer customer,
            String loanStatus
    );

    List<LoanAccount> findByCustomer(Customer customer);
}
