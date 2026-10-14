 package com.example.LoanMatrix.service.deal;

import com.example.LoanMatrix.dto.deal.ApplyLoanRequestDto;
import com.example.LoanMatrix.dto.deal.ApplyLoanResponseDto;
import com.example.LoanMatrix.dto.deal.LoanTypeOptionDto;
import com.example.LoanMatrix.entity.Customer;
import com.example.LoanMatrix.entity.DealReview;
import com.example.LoanMatrix.entity.LoanDeal;
import com.example.LoanMatrix.entity.User;
import com.example.LoanMatrix.exception.InvalidLoanOperationException;
import com.example.LoanMatrix.exception.ResourceNotFoundException;
import com.example.LoanMatrix.repository.CustomerRepository;
import com.example.LoanMatrix.repository.UserRepository;
import com.example.LoanMatrix.repository.deal.DealReviewRepository;
import com.example.LoanMatrix.repository.sanction.LoanDealSanctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplyLoanServiceImpl implements ApplyLoanService {

    private final LoanDealSanctionRepository loanDealRepository;
    private final CustomerRepository customerRepository;
    private final DealReviewRepository dealReviewRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ApplyLoanResponseDto applyForLoan(ApplyLoanRequestDto requestDto) {
        log.info("Processing loan application for Customer ID: {}", requestDto.getCustomerId());

        Customer customer = customerRepository.findById(requestDto.getCustomerId())
                .orElseThrow(() -> {
                    log.warn("Customer not found for loan application. Customer ID: {}",
                            requestDto.getCustomerId());
                    return new ResourceNotFoundException(
                            "Customer not found with ID: " + requestDto.getCustomerId());
                });

        log.debug("Customer found. Determining interest rate for loan type: {}",
                requestDto.getLoanType());

        BigDecimal interestRate = determineInterestRate(requestDto.getLoanType());

        log.debug("Interest rate determined successfully.");

        BigDecimal calculatedEmi = calculateMonthlyEmi(
                requestDto.getLoanAmount(),
                interestRate,
                requestDto.getTenureMonths()
        );

        log.debug("Monthly EMI calculated successfully.");

        LoanDeal deal = new LoanDeal();
        deal.setCustomer(customer);
        deal.setLoanType(requestDto.getLoanType());
        deal.setLoanAmount(requestDto.getLoanAmount());
        deal.setTenureMonths(requestDto.getTenureMonths());
        deal.setInterestRate(interestRate);
        deal.setEmiAmount(calculatedEmi);
        deal.setBankName(requestDto.getBankName());
        deal.setBankAccountNumber(requestDto.getBankAccountNumber());
        deal.setIfscCode(requestDto.getIfscCode());
        deal.setEmiDay(requestDto.getEmiDay());
        deal.setApprovedAmount(requestDto.getLoanAmount());

        LoanDeal savedDeal = loanDealRepository.save(deal);
        log.info("Loan deal saved successfully. Deal ID: {}", savedDeal.getId());

        return mapToDto(savedDeal, "PENDING");
    }

    @Override
    public List<ApplyLoanResponseDto> getCustomerApplications(Long customerId) {
        log.info("Fetching applications for Customer ID: {}", customerId);

        List<ApplyLoanResponseDto> applications = loanDealRepository.findByCustomer_Id(customerId)
                .stream()
                .map(this::deriveStatusAndMap)
                .collect(Collectors.toList());

        log.info("Customer applications fetched successfully. Customer ID: {}, Count: {}",
                customerId, applications.size());

        return applications;
    }

    @Override
    public List<ApplyLoanResponseDto> getPendingDeals() {
        log.info("Fetching deals not yet reviewed by an officer");

        List<ApplyLoanResponseDto> pendingDeals = loanDealRepository.findAll()
                .stream()
                .filter(deal -> !dealReviewRepository.existsByLoanDeal_Id(deal.getId()))
                .map(deal -> mapToDto(deal, "PENDING"))
                .collect(Collectors.toList());

        log.info("Pending deals fetched successfully. Count: {}", pendingDeals.size());

        return pendingDeals;
    }

    @Override
    @Transactional
    public ApplyLoanResponseDto reviewDeal(Long dealId, Long officerId, String status) {
        log.info("Officer ID {} reviewing Deal ID {} with decision: {}",
                officerId, dealId, status);

        LoanDeal deal = loanDealRepository.findById(dealId)
                .orElseThrow(() -> {
                    log.warn("Deal not found for review. Deal ID: {}", dealId);
                    return new ResourceNotFoundException("Deal not found with ID: " + dealId);
                });

        User officer = userRepository.findById(officerId)
                .orElseThrow(() -> {
                    log.warn("Officer not found. Officer ID: {}", officerId);
                    return new ResourceNotFoundException("Officer not found with ID: " + officerId);
                });

        String finalStatus = status.toUpperCase();

        DealReview review = dealReviewRepository.findTopByLoanDeal_IdOrderByIdDesc(dealId)
                .orElseGet(DealReview::new);

        review.setLoanDeal(deal);
        review.setOfficer(officer);
        review.setStatus(finalStatus);

        dealReviewRepository.save(review);
        log.info("Saved review decision [{}] to deal_reviews for Deal ID {}",
                finalStatus, dealId);

        return mapToDto(deal, finalStatus);
    }

    private BigDecimal determineInterestRate(String loanType) {
        log.debug("Determining interest rate for loan type: {}", loanType);

        if (loanType == null || loanType.isBlank()) {
            log.warn("Loan application rejected because loan type was not specified.");
            throw new InvalidLoanOperationException("Loan type must be specified.");
        }

        String normalized = loanType.trim().replace(" ", "_").toUpperCase();

        if (normalized.contains("HOME")) {
            return BigDecimal.valueOf(8.50);
        } else if (normalized.contains("CAR")) {
            return BigDecimal.valueOf(10.50);
        } else {
            log.warn("Unsupported loan type received: {}", loanType);
            throw new InvalidLoanOperationException(
                    "Unsupported loan type: " + loanType + ". Allowed: HOME_LOAN, CAR_LOAN");
        }
    }

    private BigDecimal calculateMonthlyEmi(
            BigDecimal principal,
            BigDecimal annualRate,
            Integer tenureMonths) {

        log.debug("Starting monthly EMI calculation.");

        if (principal == null || annualRate == null || tenureMonths == null || tenureMonths <= 0) {
            log.warn("EMI calculation rejected because one or more parameters are invalid.");
            throw new InvalidLoanOperationException("Invalid parameters for EMI calculation.");
        }

        double p = principal.doubleValue();
        double r = (annualRate.doubleValue() / 100.0) / 12.0;
        int n = tenureMonths;

        double emi = (p * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);

        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }

    public ApplyLoanResponseDto deriveStatusAndMap(LoanDeal deal) {
        log.debug("Deriving current status for Deal ID: {}", deal.getId());

        Optional<DealReview> latestReview =
                dealReviewRepository.findTopByLoanDeal_IdOrderByIdDesc(deal.getId());

        String currentStatus = latestReview.map(DealReview::getStatus).orElse("PENDING");

        log.debug("Current status derived for Deal ID: {}", deal.getId());

        return mapToDto(deal, currentStatus);
    }

    @Override
    public List<LoanTypeOptionDto> getAvailableLoanTypes() {
        log.debug("Fetching available loan types.");

        return List.of(
                LoanTypeOptionDto.builder()
                        .typeKey("HOME_LOAN")
                        .label("Home Loan")
                        .interestRate(BigDecimal.valueOf(8.50))
                        .build(),
                LoanTypeOptionDto.builder()
                        .typeKey("CAR_LOAN")
                        .label("Car Loan")
                        .interestRate(BigDecimal.valueOf(10.50))
                        .build()
        );
    }

    private ApplyLoanResponseDto mapToDto(LoanDeal deal, String status) {
        Customer c = deal.getCustomer();
        String customerName = "N/A";

        if (c != null) {
            String first = c.getFirstName() != null ? c.getFirstName() : "";
            String last = c.getLastName() != null ? c.getLastName() : "";
            customerName = (first + " " + last).trim();
        }

        return ApplyLoanResponseDto.builder()
                .dealId(deal.getId())
                .customerId(c != null ? c.getId() : null)
                .customerName(customerName)
                .loanType(deal.getLoanType())
                .loanAmount(deal.getLoanAmount())
                .approvedAmount(deal.getApprovedAmount())
                .interestRate(deal.getInterestRate())
                .tenureMonths(deal.getTenureMonths())
                .emiAmount(deal.getEmiAmount())
                .bankName(deal.getBankName())
                .bankAccountNumber(deal.getBankAccountNumber())
                .ifscCode(deal.getIfscCode())
                .emiDay(deal.getEmiDay())
                .status(status)
                .appliedDate(LocalDate.now())
                .rejectionReason(null)
                .build();
    }
}
