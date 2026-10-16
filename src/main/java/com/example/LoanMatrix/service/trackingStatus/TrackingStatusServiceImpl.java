
package com.example.LoanMatrix.service.trackingStatus;

import com.example.LoanMatrix.dto.trackingStatus.*;
import com.example.LoanMatrix.entity.*;
import com.example.LoanMatrix.repository.CustomerRepository;
import com.example.LoanMatrix.repository.EmiScheduleRepository;
import com.example.LoanMatrix.repository.KycDocumentRepository;
import com.example.LoanMatrix.repository.LoanAccountRepository;
import com.example.LoanMatrix.repository.deal.DealReviewRepository;
import com.example.LoanMatrix.repository.disbursement.DisbursementRepository;
import com.example.LoanMatrix.repository.sanction.SanctionLetterRepository;
import com.example.LoanMatrix.repository.trackingStatus.TrackingStatusRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TrackingStatusServiceImpl implements TrackingStatusService {

    private final TrackingStatusRepository trackingStatusRepository;
    private final CustomerRepository customerRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final EmiScheduleRepository emiScheduleRepository;
    private final DealReviewRepository dealReviewRepository;
    private final SanctionLetterRepository sanctionLetterRepository;
    private final DisbursementRepository disbursementRepository;

    public TrackingStatusServiceImpl(
            TrackingStatusRepository trackingStatusRepository,
            CustomerRepository customerRepository,
            KycDocumentRepository kycDocumentRepository,
            LoanAccountRepository loanAccountRepository,
            EmiScheduleRepository emiScheduleRepository,
            DealReviewRepository dealReviewRepository,
            SanctionLetterRepository sanctionLetterRepository,
            DisbursementRepository disbursementRepository) {

        this.trackingStatusRepository = trackingStatusRepository;
        this.customerRepository = customerRepository;
        this.kycDocumentRepository = kycDocumentRepository;
        this.loanAccountRepository = loanAccountRepository;
        this.emiScheduleRepository = emiScheduleRepository;
        this.dealReviewRepository = dealReviewRepository;
        this.sanctionLetterRepository = sanctionLetterRepository;
        this.disbursementRepository = disbursementRepository;
    }

    @Override
    public TrackingStatusResponse getTrackingStatus(Long customerId) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer not found: " + customerId));

        List<LoanDeal> deals =
                trackingStatusRepository.findByCustomer_Id(customerId);

        List<KycDocument> documents =
                kycDocumentRepository.findByCustomerId(customerId);

        boolean kycVerified = !documents.isEmpty()
                && documents.stream().allMatch(document ->
                "VERIFIED".equalsIgnoreCase(
                        document.getVerificationStatus()));

        CustomerProfileDto profile = new CustomerProfileDto(
                customer.getId(),
                "CUST" + customer.getId(),
                customer.getFirstName() + " " + customer.getLastName(),
                customer.getEmail(),
                kycVerified
        );

        List<LoanProgressDto> progressList = new ArrayList<>();

        for (LoanDeal deal : deals) {
            progressList.add(buildLoanProgress(deal, documents));
        }

        EmiSummaryDto emiSummary = buildEmiSummary(customer);

        List<ActivityTimelineDto> activities =
                buildRecentActivities(deals, documents, customerId);

        return new TrackingStatusResponse(
                profile,
                (long) deals.size(),
                progressList,
                emiSummary,
                activities
        );
    }

    private LoanProgressDto buildLoanProgress(
            LoanDeal deal,
            List<KycDocument> documents) {

        boolean submitted = true;

        boolean kycCompleted = !documents.isEmpty()
                && documents.stream().allMatch(document ->
                "VERIFIED".equalsIgnoreCase(
                        document.getVerificationStatus()));

        boolean underwritingCompleted =
                dealReviewRepository.findTopByLoanDeal_IdOrderByIdDesc(
                                deal.getId())
                        .map(review ->
                                "APPROVED".equalsIgnoreCase(
                                        review.getStatus()))
                        .orElse(false);

        boolean sanctioned =
                sanctionLetterRepository.existsByLoanDeal_Id(
                        deal.getId());

        boolean disbursed =
                disbursementRepository.findByLoanDeal_Id(deal.getId())
                        .map(disbursement ->
                                "COMPLETED".equalsIgnoreCase(
                                        disbursement.getStatus()))
                        .orElse(false);

        List<ProgressStepDto> steps = new ArrayList<>();

        steps.add(createStep(
                1, "Submitted", submitted, LocalDate.now()));

        steps.add(createStep(
                2, "KYC", kycCompleted, null));

        steps.add(createStep(
                3, "Underwriting", underwritingCompleted, null));

        steps.add(createStep(
                4, "Sanctioned", sanctioned, null));

        steps.add(createStep(
                5, "Disbursed", disbursed, null));

        // The guide keeps the Closed step pending for now.
        steps.add(createStep(
                6, "Closed", false, null));

        int currentStepIndex = 1;

        for (ProgressStepDto step : steps) {
            if ("COMPLETED".equals(step.getStatus())) {
                currentStepIndex = step.getStepNumber();
            }
        }

        String overallStatus = steps.get(currentStepIndex - 1)
                .getStepName();

        String bannerMessage = "Your application is currently at the "
                + overallStatus + " stage.";

        return new LoanProgressDto(
                deal.getId(),
                "APP" + deal.getId(),
                overallStatus,
                currentStepIndex,
                bannerMessage,
                steps
        );
    }

    private ProgressStepDto createStep(
            int stepNumber,
            String stepName,
            boolean completed,
            LocalDate completedDate) {

        return new ProgressStepDto(
                stepNumber,
                stepName,
                completed ? "COMPLETED" : "PENDING",
                completed ? completedDate : null
        );
    }

    private EmiSummaryDto buildEmiSummary(Customer customer) {

        List<EmiSchedule> schedules =
                emiScheduleRepository.findByLoanAccount_Customer(customer);

        LocalDate today = LocalDate.now();

        long paidEmis = schedules.stream()
                .filter(emi -> isPaid(emi))
                .count();

        BigDecimal paidAmount = schedules.stream()
                .filter(emi -> isPaid(emi))
                .map(EmiSchedule::getEmi)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<EmiSchedule> upcoming = schedules.stream()
                .filter(emi -> !isPaid(emi))
                .filter(emi -> emi.getDueDate() != null)
                .filter(emi -> !emi.getDueDate().isBefore(today))
                .toList();

        BigDecimal upcomingAmount = upcoming.stream()
                .map(EmiSchedule::getEmi)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long remainingEmis = schedules.stream()
                .filter(emi -> !isPaid(emi))
                .count();

        BigDecimal remainingAmount = schedules.stream()
                .filter(emi -> !isPaid(emi))
                .map(EmiSchedule::getEmi)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new EmiSummaryDto(
                (long) schedules.size(),
                paidEmis,
                paidAmount,
                (long) upcoming.size(),
                upcomingAmount,
                remainingEmis,
                remainingAmount
        );
    }

    private boolean isPaid(EmiSchedule emi) {
        return "PAID".equalsIgnoreCase(emi.getPaymentStatus());
    }

    private List<ActivityTimelineDto> buildRecentActivities(
            List<LoanDeal> deals,
            List<KycDocument> documents,
            Long customerId) {

        List<ActivityTimelineDto> activities = new ArrayList<>();

        if (!deals.isEmpty()) {
            activities.add(new ActivityTimelineDto(
                    "Loan Application Submitted",
                    "Your loan application has been recorded.",
                    LocalDateTime.now()
            ));
        }

        if (!documents.isEmpty()
                && documents.stream().allMatch(document ->
                "VERIFIED".equalsIgnoreCase(
                        document.getVerificationStatus()))) {

            activities.add(new ActivityTimelineDto(
                    "KYC Verified",
                    "Your KYC documents have been verified.",
                    LocalDateTime.now()
            ));
        }

        for (LoanDeal deal : deals) {

            sanctionLetterRepository.findByLoanDeal_Id(deal.getId())
                    .ifPresent(sanction -> activities.add(
                            new ActivityTimelineDto(
                                    "Loan Sanctioned",
                                    "Your loan application has been sanctioned.",
                                    sanction.getCreatedAt() != null
                                            ? sanction.getCreatedAt()
                                            : LocalDateTime.now()
                            )
                    ));

            disbursementRepository.findByLoanDeal_Id(deal.getId())
                    .filter(disbursement ->
                            "COMPLETED".equalsIgnoreCase(
                                    disbursement.getStatus()))
                    .ifPresent(disbursement -> activities.add(
                            new ActivityTimelineDto(
                                    "Loan Disbursed",
                                    "Your loan amount has been disbursed.",
                                    disbursement.getDisbursementDate() != null
                                            ? disbursement.getDisbursementDate()
                                            .atStartOfDay()
                                            : LocalDateTime.now()
                            )
                    ));
        }

        activities.sort(
                Comparator.comparing(
                        ActivityTimelineDto::getTimestamp,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );

        return activities.stream()
                .limit(5)
                .toList();
    }
}
