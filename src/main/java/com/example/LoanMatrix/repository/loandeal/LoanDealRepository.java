
package com.example.LoanMatrix.repository.loandeal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.LoanMatrix.entity.LoanDeal;

@Repository
public interface LoanDealRepository
        extends JpaRepository<LoanDeal, Long> {

}