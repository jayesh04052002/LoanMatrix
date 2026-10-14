package com.example.LoanMatrix.service.reports;

import com.example.LoanMatrix.dto.reports.*;
import com.example.LoanMatrix.entity.CibilReport;
import com.example.LoanMatrix.entity.LoanAccount;
import com.example.LoanMatrix.entity.LoanDeal;
import com.example.LoanMatrix.entity.ScoreCard;
import com.example.LoanMatrix.repository.Payment.PenaltyChargeRepository;
import com.example.LoanMatrix.repository.cibil.CibilReportRepository;
import com.example.LoanMatrix.repository.loandeal.LoanDealRepository;
import com.example.LoanMatrix.repository.scorecard.ScoreCardRepository;
import com.example.LoanMatrix.entity.EmiSchedule;
import com.example.LoanMatrix.repository.EmiScheduleRepository;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.example.LoanMatrix.entity.PenaltyCharge;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.LoanMatrix.repository.LoanAccountRepository;
import com.example.LoanMatrix.dto.reports.PrepaymentReportResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportsServiceImpl implements ReportsService {

    private final CibilReportRepository cibilReportRepository;
    private final ScoreCardRepository scoreCardRepository;
    private final LoanDealRepository loanDealRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final EmiScheduleRepository emiScheduleRepository;
    private final PenaltyChargeRepository penaltyChargeRepository;

    public ReportsServiceImpl(
            CibilReportRepository cibilReportRepository,
            ScoreCardRepository scoreCardRepository, LoanDealRepository loanDealRepository, LoanAccountRepository loanAccountRepository, EmiScheduleRepository emiScheduleRepository, PenaltyChargeRepository penaltyChargeRepository) {

        this.cibilReportRepository = cibilReportRepository;
        this.scoreCardRepository = scoreCardRepository;
        this.loanDealRepository = loanDealRepository;
        this.loanAccountRepository = loanAccountRepository;
        this.emiScheduleRepository = emiScheduleRepository;
        this.penaltyChargeRepository = penaltyChargeRepository;
    }

    // Report 1: CIBIL Score Range Report
    @Override
    @Transactional(readOnly = true)
    public List<CibilScoreRangeResponse> getCibilScoreRangeReport() {

        List<CibilReport> reports =
                cibilReportRepository.findAll();

        long excellent = 0;
        long veryGood = 0;
        long good = 0;
        long average = 0;
        long risky = 0;
        long reject = 0;

        for (CibilReport report : reports) {

            Integer score = report.getCibilScore();

            if (score == null) {
                continue;
            }

            if (score >= 900) {
                excellent++;
            } else if (score >= 800) {
                veryGood++;
            } else if (score >= 750) {
                good++;
            } else if (score >= 700) {
                average++;
            } else if (score >= 650) {
                risky++;
            } else {
                reject++;
            }
        }

        List<CibilScoreRangeResponse> result =
                new ArrayList<>();

        result.add(new CibilScoreRangeResponse(
                "900 and above", "Excellent", excellent));

        result.add(new CibilScoreRangeResponse(
                "800-899", "Very Good", veryGood));

        result.add(new CibilScoreRangeResponse(
                "750-799", "Good", good));

        result.add(new CibilScoreRangeResponse(
                "700-749", "Average", average));

        result.add(new CibilScoreRangeResponse(
                "650-699", "Risky", risky));

        result.add(new CibilScoreRangeResponse(
                "Below 650", "Reject", reject));

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoreCardWiseResponse> getScoreCardWiseReport() {

        List<ScoreCard> scoreCards =
                scoreCardRepository.findAll();

        List<ScoreCardWiseResponse> result =
                new ArrayList<>();

        for (ScoreCard scoreCard : scoreCards) {

            ScoreCardWiseResponse response =
                    new ScoreCardWiseResponse();

            response.setScoreCardId(scoreCard.getId());

            if (scoreCard.getCustomer() != null) {

                response.setCustomerId(
                        scoreCard.getCustomer().getId());

                String firstName =
                        scoreCard.getCustomer().getFirstName();

                String lastName =
                        scoreCard.getCustomer().getLastName();

                String customerName = "";

                if (firstName != null) {
                    customerName = firstName;
                }

                if (lastName != null) {

                    if (!customerName.isEmpty()) {
                        customerName = customerName + " ";
                    }

                    customerName = customerName + lastName;
                }

                response.setCustomerName(customerName);
            }

            response.setAppliedDate(
                    scoreCard.getAppliedDate());

            response.setCibilScore(
                    scoreCard.getCibilScore());

            response.setRiskCategory(
                    scoreCard.getRiskCategory());

            response.setEligibleLoanAmount(
                    scoreCard.getEligibleLoanAmount());

            response.setCurrentStatus(
                    scoreCard.getCurrentStatus());

            response.setRejectionReason(
                    scoreCard.getRejectionReason());

            result.add(response);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductWiseResponse> getProductWiseReport() {

        List<LoanDeal> loanDeals =
                loanDealRepository.findAll();

        Map<String, ProductWiseResponse> productMap =
                new LinkedHashMap<>();

        for (LoanDeal deal : loanDeals) {

            String loanType = deal.getLoanType();

            if (loanType == null || loanType.trim().isEmpty()) {
                loanType = "Unspecified";
            }

            ProductWiseResponse response =
                    productMap.get(loanType);

            if (response == null) {

                response = new ProductWiseResponse(
                        loanType,
                        0,
                        BigDecimal.ZERO
                );

                productMap.put(loanType, response);
            }

            response.setTotalLoans(
                    response.getTotalLoans() + 1);

            BigDecimal loanAmount = deal.getLoanAmount();

            if (loanAmount != null) {

                response.setTotalLoanAmount(
                        response.getTotalLoanAmount()
                                .add(loanAmount));
            }
        }

        return new ArrayList<>(productMap.values());
    }

    // Report 4: Loan Portfolio Report
    @Override
    @Transactional(readOnly = true)
    public LoanPortfolioResponse getLoanPortfolioReport() {

        List<LoanAccount> accounts =
                loanAccountRepository.findAll();

        long totalLoanAccounts = accounts.size();

        BigDecimal totalLoanAmount = BigDecimal.ZERO;
        BigDecimal totalOutstandingPrincipal = BigDecimal.ZERO;

        long activeLoans = 0;
        long closedLoans = 0;
        long otherStatusLoans = 0;

        for (LoanAccount account : accounts) {

            // Calculate total loan amount
            if (account.getLoanAmount() != null) {
                totalLoanAmount = totalLoanAmount.add(
                        account.getLoanAmount()
                );
            }

            // Calculate total outstanding principal
            if (account.getOutstandingPrincipal() != null) {
                totalOutstandingPrincipal =
                        totalOutstandingPrincipal.add(
                                account.getOutstandingPrincipal()
                        );
            }

            // Count loans according to their status
            String status = account.getLoanStatus();

            if (status == null) {
                otherStatusLoans++;

            } else if (status.equalsIgnoreCase("ACTIVE")) {
                activeLoans++;

            } else if (status.equalsIgnoreCase("CLOSED")) {
                closedLoans++;

            } else {
                otherStatusLoans++;
            }
        }

        return new LoanPortfolioResponse(
                totalLoanAccounts,
                totalLoanAmount,
                totalOutstandingPrincipal,
                activeLoans,
                closedLoans,
                otherStatusLoans
        );
    }

// Report 5: EMI Collection Report
    @Override
    @Transactional(readOnly = true)
    public EmiCollectionResponse getEmiCollectionReport() {

        List<EmiSchedule> schedules =
                emiScheduleRepository.findAll();

        long totalInstallments = schedules.size();

        BigDecimal totalScheduledAmount = BigDecimal.ZERO;
        BigDecimal totalCollectedAmount = BigDecimal.ZERO;
        BigDecimal pendingAmount = BigDecimal.ZERO;

        long paidInstallments = 0;
        long pendingInstallments = 0;

        for (EmiSchedule schedule : schedules) {

            BigDecimal emi = schedule.getEmi();

            if (emi == null) {
                emi = BigDecimal.ZERO;
            }

            totalScheduledAmount =
                    totalScheduledAmount.add(emi);

            String status = schedule.getPaymentStatus();

            if (status != null
                    && status.equalsIgnoreCase("PAID")) {

                paidInstallments++;

                totalCollectedAmount =
                        totalCollectedAmount.add(emi);

            } else if (status != null
                    && status.equalsIgnoreCase("PENDING")) {

                pendingInstallments++;

                pendingAmount =
                        pendingAmount.add(emi);
            }
        }

        return new EmiCollectionResponse(
                totalInstallments,
                totalScheduledAmount,
                totalCollectedAmount,
                paidInstallments,
                pendingInstallments,
                pendingAmount
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OverdueReportResponse getOverdueReport() {

        List<EmiSchedule> schedules = emiScheduleRepository.findAll();

        List<PenaltyCharge> penalties = penaltyChargeRepository.findAll();

        BigDecimal totalOverdueEmiAmount = BigDecimal.ZERO;
        BigDecimal totalPenaltyAmount = BigDecimal.ZERO;

        long totalOverdueInstallments = 0;

        Set<Long> overdueLoanAccountIds = new HashSet<>();

        LocalDate today = LocalDate.now();

        for (EmiSchedule schedule : schedules) {

            if (schedule.getDueDate() == null) {
                continue;
            }

            String status = schedule.getPaymentStatus();

            boolean unpaid = status == null
                    || (!status.equalsIgnoreCase("PAID")
                    && !status.equalsIgnoreCase("CANCELLED"));

            boolean overdue = schedule.getDueDate().isBefore(today);

            if (overdue && unpaid) {

                totalOverdueInstallments++;

                if (schedule.getEmi() != null) {
                    totalOverdueEmiAmount =
                            totalOverdueEmiAmount.add(schedule.getEmi());
                }

                if (schedule.getLoanAccount() != null
                        && schedule.getLoanAccount().getId() != null) {

                    overdueLoanAccountIds.add(
                            schedule.getLoanAccount().getId());
                }

                for (PenaltyCharge penalty : penalties) {

                    if (penalty.getEmiSchedule() != null
                            && penalty.getEmiSchedule().getId()
                            .equals(schedule.getId())) {

                        String penaltyStatus = penalty.getStatus();

                        if (penaltyStatus != null
                                && penaltyStatus.equalsIgnoreCase("CHARGED")
                                && penalty.getPenaltyAmount() != null) {

                            totalPenaltyAmount = totalPenaltyAmount.add(
                                    penalty.getPenaltyAmount());
                        }
                    }
                }
            }
        }

        BigDecimal totalOverdueAmount =
                totalOverdueEmiAmount.add(totalPenaltyAmount);

        return OverdueReportResponse.builder()
                .totalOverdueInstallments(totalOverdueInstallments)
                .totalOverdueEmiAmount(totalOverdueEmiAmount)
                .totalPenaltyAmount(totalPenaltyAmount)
                .totalOverdueAmount(totalOverdueAmount)
                .overdueLoanAccounts(overdueLoanAccountIds.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DisbursementReportResponse getDisbursementReport() {

        List<LoanAccount> accounts = loanAccountRepository.findAll();

        long totalDisbursedLoans = 0;

        BigDecimal totalDisbursedAmount = BigDecimal.ZERO;

        LocalDate latestDisbursementDate = null;

        for (LoanAccount account : accounts) {

            if (account.getDisbursementDate() != null) {

                totalDisbursedLoans++;

                if (account.getLoanAmount() != null) {
                    totalDisbursedAmount =
                            totalDisbursedAmount.add(account.getLoanAmount());
                }

                if (latestDisbursementDate == null
                        || account.getDisbursementDate()
                        .isAfter(latestDisbursementDate)) {

                    latestDisbursementDate = account.getDisbursementDate();
                }
            }
        }

        BigDecimal averageDisbursedAmount = BigDecimal.ZERO;

        if (totalDisbursedLoans > 0) {
            averageDisbursedAmount = totalDisbursedAmount.divide(
                    BigDecimal.valueOf(totalDisbursedLoans),
                    2,
                    java.math.RoundingMode.HALF_UP
            );
        }

        return DisbursementReportResponse.builder()
                .totalDisbursedLoans(totalDisbursedLoans)
                .totalDisbursedAmount(totalDisbursedAmount)
                .averageDisbursedAmount(averageDisbursedAmount)
                .latestDisbursementDate(latestDisbursementDate)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PrepaymentReportResponse getPrepaymentReport() {

        List<LoanAccount> accounts = loanAccountRepository.findAll();

        long totalPrepaidLoans = 0;

        BigDecimal totalPrepaidAmount = BigDecimal.ZERO;

        for (LoanAccount account : accounts) {

            if ("CLOSED".equalsIgnoreCase(account.getLoanStatus())
                    && account.getTotalPaidAmount() != null
                    && account.getTotalPaidAmount().compareTo(BigDecimal.ZERO) > 0) {

                totalPrepaidLoans++;

                totalPrepaidAmount = totalPrepaidAmount.add(
                        account.getTotalPaidAmount());
            }
        }

        BigDecimal averagePrepaidAmount = BigDecimal.ZERO;

        if (totalPrepaidLoans > 0) {
            averagePrepaidAmount = totalPrepaidAmount.divide(
                    BigDecimal.valueOf(totalPrepaidLoans),
                    2,
                    java.math.RoundingMode.HALF_UP
            );
        }

        return PrepaymentReportResponse.builder()
                .totalPrepaidLoans(totalPrepaidLoans)
                .totalPrepaidAmount(totalPrepaidAmount)
                .averagePrepaidAmount(averagePrepaidAmount)
                .build();
    }


}