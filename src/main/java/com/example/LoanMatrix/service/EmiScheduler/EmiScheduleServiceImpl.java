package com.example.LoanMatrix.service.EmiScheduler;

import com.example.LoanMatrix.entity.EmiSchedule;
import com.example.LoanMatrix.entity.LoanAccount;

import com.example.LoanMatrix.exception.InvalidLoanStatusException;
import com.example.LoanMatrix.exception.ResourceNotFoundException;
import com.example.LoanMatrix.exception.ScheduleAlreadyExistsException;
import com.example.LoanMatrix.repository.EmiScheduleRepository.EmiScheduleRepository;
import com.example.LoanMatrix.repository.EmiScheduleRepository.LoanAccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmiScheduleServiceImpl implements EmiSchedulerService {

    private final EmiScheduleRepository emiScheduleRepository;
    private final LoanAccountRepository loanAccountRepository;

    public EmiScheduleServiceImpl(EmiScheduleRepository emiScheduleRepository,
                                  LoanAccountRepository loanAccountRepository) {
        this.emiScheduleRepository = emiScheduleRepository;
        this.loanAccountRepository = loanAccountRepository;
    }


    @Override
    public List<EmiSchedule> generateSchedule(Long loanAccountId) {

        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan account not found"));

        if (emiScheduleRepository.existsByLoanAccountId(loanAccountId)) {
            throw new ScheduleAlreadyExistsException("EMI schedule already exists for this loan account");
        }

        BigDecimal outstandingPrincipal = loanAccount.getOutstandingPrincipal();
        BigDecimal emi = loanAccount.getEmiAmount();
        BigDecimal annualInterestRate = loanAccount.getInterestRate();

        int tenure = loanAccount.getTenureMonths();

        BigDecimal monthlyInterestRate = annualInterestRate
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        Integer emiDay = loanAccount.getLoanDeal().getEmiDay();

        LocalDate firstDueDate = loanAccount.getDisbursementDate()
                .withDayOfMonth(emiDay);

        if (!firstDueDate.isAfter(loanAccount.getDisbursementDate())) {
            firstDueDate = firstDueDate.plusMonths(1);
        }

        List<EmiSchedule> schedules = new ArrayList<>();

        for (int i = 1; i <= tenure; i++) {

            LocalDate dueDate = firstDueDate.plusMonths(i - 1);

            BigDecimal openingBalance = outstandingPrincipal;

            BigDecimal interestAmount = openingBalance
                    .multiply(monthlyInterestRate)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal principalAmount = emi.subtract(interestAmount);

            if (principalAmount.compareTo(openingBalance) > 0) {
                principalAmount = openingBalance;
            }

            BigDecimal actualEmi = principalAmount.add(interestAmount);

            BigDecimal closingBalance = openingBalance.subtract(principalAmount)
                    .setScale(2, RoundingMode.HALF_UP);

            EmiSchedule schedule = new EmiSchedule();

            schedule.setLoanAccount(loanAccount);
            schedule.setInstallmentNo(i);
            schedule.setDueDate(dueDate);
            schedule.setOpeningBalance(openingBalance);
            schedule.setInterestAmount(interestAmount);
            schedule.setPrincipalAmount(principalAmount);
            schedule.setEmi(actualEmi);
            schedule.setClosingBalance(closingBalance);
            schedule.setPaymentStatus("PENDING");

            schedules.add(schedule);

            outstandingPrincipal = closingBalance;

            if (outstandingPrincipal.compareTo(BigDecimal.ZERO) == 0) {
                break;
            }
        }

        return emiScheduleRepository.saveAll(schedules);
    }

    @Override
    public List<EmiSchedule> getSchedule(Long loanAccountId) {

        return emiScheduleRepository
                .findByLoanAccountIdOrderByInstallmentNoAsc(loanAccountId);
    }


    @Override
    public void recalculateSchedule(Long loanAccountId) {

        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan account not found"));

        List<EmiSchedule> schedules =
                emiScheduleRepository.findByLoanAccountIdOrderByInstallmentNoAsc(
                        loanAccountId
                );

        BigDecimal outstandingPrincipal = loanAccount.getOutstandingPrincipal();
        BigDecimal annualInterestRate = loanAccount.getInterestRate();

        BigDecimal monthlyInterestRate = annualInterestRate
                .divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);

        int lastPaidInstallment = 0;

        for (EmiSchedule schedule : schedules) {
            if ("PAID".equals(schedule.getPaymentStatus())) {
                lastPaidInstallment = Math.max(
                        lastPaidInstallment,
                        schedule.getInstallmentNo()
                );
            }
        }

        int remainingMonths = 0;

        for (EmiSchedule schedule : schedules) {
            if (schedule.getInstallmentNo() > lastPaidInstallment
                    && "PENDING".equals(schedule.getPaymentStatus())) {
                remainingMonths++;
            }
        }

        if (remainingMonths == 0) {
            return;
        }

        if (outstandingPrincipal.compareTo(BigDecimal.ZERO) <= 0) {
            for (EmiSchedule schedule : schedules) {
                if (schedule.getInstallmentNo() > lastPaidInstallment
                        && "PENDING".equals(schedule.getPaymentStatus())) {
                    schedule.setPaymentStatus("CANCELLED");
                    schedule.setCancellationReason("Loan balance is fully repaid");
                    schedule.setOpeningBalance(BigDecimal.ZERO);
                    schedule.setInterestAmount(BigDecimal.ZERO);
                    schedule.setPrincipalAmount(BigDecimal.ZERO);
                    schedule.setEmi(BigDecimal.ZERO);
                    schedule.setClosingBalance(BigDecimal.ZERO);
                }
            }

            emiScheduleRepository.saveAll(schedules);
            return;
        }

        BigDecimal newEmi;

        if (monthlyInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            newEmi = outstandingPrincipal.divide(
                    BigDecimal.valueOf(remainingMonths),
                    2,
                    RoundingMode.HALF_UP
            );
        } else {
            BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyInterestRate);
            BigDecimal power = onePlusRate.pow(remainingMonths);

            newEmi = outstandingPrincipal
                    .multiply(monthlyInterestRate)
                    .multiply(power)
                    .divide(
                            power.subtract(BigDecimal.ONE),
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        for (EmiSchedule schedule : schedules) {

            if (schedule.getInstallmentNo() <= lastPaidInstallment
                    || !"PENDING".equals(schedule.getPaymentStatus())) {
                continue;
            }

            BigDecimal openingBalance = outstandingPrincipal;

            BigDecimal interestAmount = openingBalance
                    .multiply(monthlyInterestRate)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal principalAmount = newEmi.subtract(interestAmount);

            if (principalAmount.compareTo(openingBalance) >= 0) {
                principalAmount = openingBalance;
            }

            BigDecimal actualEmi = principalAmount.add(interestAmount);

            BigDecimal closingBalance = openingBalance
                    .subtract(principalAmount)
                    .setScale(2, RoundingMode.HALF_UP);

            schedule.setOpeningBalance(openingBalance);
            schedule.setInterestAmount(interestAmount);
            schedule.setPrincipalAmount(principalAmount);
            schedule.setEmi(actualEmi);
            schedule.setClosingBalance(closingBalance);

            outstandingPrincipal = closingBalance;
        }

        emiScheduleRepository.saveAll(schedules);
    }


    @Override
    public void cancelFutureEmis(Long loanAccountId) {

        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan account not found"));

        if (!"CLOSED".equalsIgnoreCase(loanAccount.getLoanStatus())) {
            throw new InvalidLoanStatusException("Loan account is not closed");
        }

        List<EmiSchedule> schedules =
                emiScheduleRepository.findByLoanAccountIdOrderByInstallmentNoAsc(
                        loanAccountId
                );

        for (EmiSchedule schedule : schedules) {

            if ("PENDING".equals(schedule.getPaymentStatus())) {
                schedule.setPaymentStatus("CANCELLED");
                schedule.setCancellationReason("Loan foreclosed");
            }
        }

        emiScheduleRepository.saveAll(schedules);
    }

}