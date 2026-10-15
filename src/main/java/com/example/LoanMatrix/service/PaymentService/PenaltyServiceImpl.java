package com.example.LoanMatrix.service.PaymentService;

import com.example.LoanMatrix.entity.EmiSchedule;
import com.example.LoanMatrix.entity.PenaltyCharge;
import com.example.LoanMatrix.repository.Payment.PenaltyChargeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PenaltyServiceImpl implements PenaltyService {

    private final PenaltyChargeRepository penaltyChargeRepository;

    @Override
    @Transactional
    public BigDecimal calculateAndUpdatePenalty(EmiSchedule emiSchedule) {

        LocalDate today = LocalDate.now();
        LocalDate dueDate = emiSchedule.getDueDate();

        // No penalty if EMI is not overdue or is already paid
        if (dueDate == null
                || !today.isAfter(dueDate)
                || "PAID".equalsIgnoreCase(emiSchedule.getPaymentStatus())) {
            return BigDecimal.ZERO;
        }

        // Calculate overdue days
        long overdueDays = ChronoUnit.DAYS.between(dueDate, today);

        // First overdue day = 500; every subsequent day adds 1
        BigDecimal penaltyAmount = BigDecimal.valueOf(500)
                .add(BigDecimal.valueOf(Math.max(0, overdueDays - 1)));

        List<PenaltyCharge> penalties =
                penaltyChargeRepository.findByEmiSchedule_Id(emiSchedule.getId());

        PenaltyCharge pendingPenalty = null;

        for (int i = 0; i < penalties.size(); i++) {
            PenaltyCharge penalty = penalties.get(i);

            if (!"PAID".equalsIgnoreCase(penalty.getStatus())) {
                pendingPenalty = penalty;
                break;
            }
        }

        // Create a record if no pending penalty exists
        if (pendingPenalty == null) {
            pendingPenalty = new PenaltyCharge();
            pendingPenalty.setEmiSchedule(emiSchedule);
            pendingPenalty.setLoanAccount(emiSchedule.getLoanAccount());
            pendingPenalty.setCreatedAt(LocalDateTime.now());
        }

        // Update the existing penalty instead of adding the daily charge repeatedly
        pendingPenalty.setPenaltyAmount(penaltyAmount);
        pendingPenalty.setReason("Overdue EMI penalty");
        pendingPenalty.setStatus("PENDING");

        penaltyChargeRepository.save(pendingPenalty);

        return penaltyAmount;
    }

    @Override
    @Transactional
    public void markPenaltyAsPaid(Long emiScheduleId) {

        List<PenaltyCharge> penalties =
                penaltyChargeRepository.findByEmiSchedule_Id(emiScheduleId);

        for (int i = 0; i < penalties.size(); i++) {
            PenaltyCharge penalty = penalties.get(i);

            if (!"PAID".equalsIgnoreCase(penalty.getStatus())) {
                penalty.setStatus("PAID");
                penaltyChargeRepository.save(penalty);
            }
        }
    }
}
