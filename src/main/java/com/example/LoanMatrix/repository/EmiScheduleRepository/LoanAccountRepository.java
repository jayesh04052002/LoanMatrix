package com.example.LoanMatrix.repository.EmiScheduleRepository;

import com.example.LoanMatrix.entity.LoanAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanAccountRepository extends JpaRepository<LoanAccount, Long> {
}
