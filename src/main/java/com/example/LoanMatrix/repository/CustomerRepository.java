package com.example.LoanMatrix.repository;
import com.example.LoanMatrix.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long>
{
    boolean existsByEmail(String email);
    boolean existsByAadhaarNo(String aadhaarNo);

    boolean existsByPanCard(String panCard);

}