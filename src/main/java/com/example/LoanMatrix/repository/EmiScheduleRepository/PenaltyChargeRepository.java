package com.example.LoanMatrix.repository.EmiScheduleRepository;


import com.example.LoanMatrix.entity.PenaltyCharge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PenaltyChargeRepository extends JpaRepository<PenaltyCharge, Long> {

    boolean existsByEmiScheduleId(Long emiScheduleId);
}