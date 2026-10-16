package com.example.LoanMatrix.service.PaymentService;

import com.example.LoanMatrix.entity.EmiSchedule;

import java.math.BigDecimal;

public interface PenaltyService {

    BigDecimal calculateAndUpdatePenalty(EmiSchedule emiSchedule);

    void markPenaltyAsPaid(Long emiScheduleId);
}