package com.example.LoanMatrix.service.disbursement;

import com.example.LoanMatrix.dto.disbursement.DisbursementRequestDto;
import com.example.LoanMatrix.dto.disbursement.DisbursementResponseDto;
import com.example.LoanMatrix.entity.Customer;
import com.example.LoanMatrix.entity.Disbursement;
import com.example.LoanMatrix.entity.LoanAccount;
import com.example.LoanMatrix.entity.LoanDeal;
import com.example.LoanMatrix.exception.InvalidLoanOperationException;
import com.example.LoanMatrix.exception.ResourceNotFoundException;
import com.example.LoanMatrix.repository.disbursement.DisbursementRepository;
import com.example.LoanMatrix.repository.disbursement.LoanAccountDisbursementRepository;
import com.example.LoanMatrix.repository.sanction.LoanDealSanctionRepository;
import com.example.LoanMatrix.repository.sanction.SanctionLetterRepository;
import com.example.LoanMatrix.service.EmiScheduler.EmiSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisbursementServiceImpl implements DisbursementService {

    private final DisbursementRepository disbursementRepository;
    private final LoanAccountDisbursementRepository loanAccountRepository;
    private final LoanDealSanctionRepository loanDealRepository;
    private final SanctionLetterRepository sanctionLetterRepository;
    private final EmiSchedulerService emiSchedulerService;

    @Override
    @Transactional
    public DisbursementResponseDto processDisbursement(DisbursementRequestDto requestDto) {
        log.info("Processing loan disbursement for Deal ID: {}", requestDto.getDealId());

        // 1. Verify Deal exists
        LoanDeal loanDeal = loanDealRepository.findById(requestDto.getDealId())
                .orElseThrow(() -> {
                    log.warn("Loan deal not found. Deal ID: {}", requestDto.getDealId());
                    return new ResourceNotFoundException(
                            "Loan deal not found with ID: " + requestDto.getDealId());
                });

        log.debug("Loan deal found. Checking whether it has already been disbursed.");

        // 2. Guard: Prevent double disbursement
        if (disbursementRepository.existsByLoanDeal_Id(requestDto.getDealId())) {
            log.warn("Deal ID {} is already disbursed.", requestDto.getDealId());
            throw new InvalidLoanOperationException(
                    "Loan has already been disbursed for this deal.");
        }

        log.debug("Checking sanction letter for Deal ID: {}", requestDto.getDealId());

        // 3. Guard: Prevent disbursement without an approved Sanction Letter
        if (!sanctionLetterRepository.existsByLoanDeal_Id(requestDto.getDealId())) {
            log.warn("Deal ID {} does not have a sanction letter generated.",
                    requestDto.getDealId());
            throw new InvalidLoanOperationException(
                    "Cannot disburse loan: Sanction letter has not been generated for Deal ID: "
                            + requestDto.getDealId());
        }

        BigDecimal disburseAmount = requestDto.getDisbursementAmount() != null
                ? requestDto.getDisbursementAmount()
                : (loanDeal.getApprovedAmount() != null
                   ? loanDeal.getApprovedAmount()
                   : loanDeal.getLoanAmount());

        // 4. Save Disbursement record
        Disbursement disbursement = new Disbursement();
        disbursement.setLoanDeal(loanDeal);
        disbursement.setDisburseAmount(disburseAmount);
        disbursement.setBankPartner(requestDto.getBankPartner());
        disbursement.setDisbursementDate(requestDto.getDisbursementDate());
        disbursement.setStatus("DISBURSED");

        Disbursement savedDisbursement = disbursementRepository.save(disbursement);
        log.info("Disbursement record saved successfully. Disbursement ID: {}",
                savedDisbursement.getId());

        // 5. Create LMS Loan Account
        log.debug("Creating LMS loan account for Deal ID: {}", requestDto.getDealId());

        String generatedAccountNo = "LA" + UUID.randomUUID()
                .toString().replace("-", "").substring(0, 10).toUpperCase();

        LoanAccount loanAccount = new LoanAccount();
        loanAccount.setCustomer(loanDeal.getCustomer());
        loanAccount.setLoanDeal(loanDeal);
        loanAccount.setLoanAccountNo(generatedAccountNo);
        loanAccount.setLoanAmount(disburseAmount);
        loanAccount.setOutstandingPrincipal(disburseAmount);
        loanAccount.setLoanStatus("ACTIVE");
        loanAccount.setInterestRate(loanDeal.getInterestRate());
        loanAccount.setTenureMonths(loanDeal.getTenureMonths());
        loanAccount.setEmiAmount(loanDeal.getEmiAmount());
        loanAccount.setDisbursementDate(requestDto.getDisbursementDate());
        loanAccount.setTotalPaidAmount(BigDecimal.ZERO);
        loanAccount.setCreatedAt(LocalDateTime.now());

        LoanAccount savedAccount = loanAccountRepository.save(loanAccount);
        log.info("LMS loan account created successfully. Account ID: {}",
                savedAccount.getId());

        log.info("Loan disbursement process completed successfully for Deal ID: {}",
                requestDto.getDealId());

        // Generate the initial EMI schedule for the newly created loan account
        emiSchedulerService.generateSchedule(savedAccount.getId());

        return mapToResponseDto(savedDisbursement, savedAccount);
    }

    @Override
    public List<DisbursementResponseDto> getAllDisbursements() {
        log.info("Fetching all disbursements");

        List<DisbursementResponseDto> disbursements =
                disbursementRepository.findAllByOrderByDisbursementDateDesc()
                        .stream()
                        .map(d -> {
                            LoanAccount acc = (d.getLoanDeal() != null)
                                    ? loanAccountRepository
                                    .findByLoanDeal_Id(d.getLoanDeal().getId())
                                    .orElse(null)
                                    : null;
                            return mapToResponseDto(d, acc);
                        })
                        .collect(Collectors.toList());

        log.info("Fetched all disbursements successfully. Count: {}",
                disbursements.size());

        return disbursements;
    }

    @Override
    public DisbursementResponseDto getDisbursementByDealId(Long dealId) {
        log.info("Fetching disbursement for Deal ID: {}", dealId);

        Disbursement disbursement = disbursementRepository.findByLoanDeal_Id(dealId)
                .orElseThrow(() -> {
                    log.warn("Disbursement not found for Deal ID: {}", dealId);
                    return new ResourceNotFoundException(
                            "Disbursement not found for Deal ID: " + dealId);
                });

        LoanAccount acc = loanAccountRepository.findByLoanDeal_Id(dealId).orElse(null);

        log.debug("Disbursement fetched successfully for Deal ID: {}", dealId);

        return mapToResponseDto(disbursement, acc);
    }

    @Override
    public DisbursementResponseDto getDisbursementById(Long id) {
        log.info("Fetching disbursement by ID: {}", id);

        Disbursement disbursement = disbursementRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Disbursement not found with ID: {}", id);
                    return new ResourceNotFoundException(
                            "Disbursement not found with ID: " + id);
                });

        LoanAccount acc = (disbursement.getLoanDeal() != null)
                ? loanAccountRepository
                .findByLoanDeal_Id(disbursement.getLoanDeal().getId())
                .orElse(null)
                : null;

        log.debug("Disbursement fetched successfully. Disbursement ID: {}", id);

        return mapToResponseDto(disbursement, acc);
    }

    @Override
    public List<DisbursementResponseDto> getDisbursementsByCustomerId(Long customerId) {
        log.info("Fetching disbursements for Customer ID: {}", customerId);

        List<DisbursementResponseDto> disbursements =
                disbursementRepository.findByLoanDeal_Customer_Id(customerId)
                        .stream()
                        .map(d -> {
                            LoanAccount acc = (d.getLoanDeal() != null)
                                    ? loanAccountRepository
                                    .findByLoanDeal_Id(d.getLoanDeal().getId())
                                    .orElse(null)
                                    : null;
                            return mapToResponseDto(d, acc);
                        })
                        .collect(Collectors.toList());

        log.info("Fetched disbursements for Customer ID: {}. Count: {}",
                customerId, disbursements.size());

        return disbursements;
    }

    private DisbursementResponseDto mapToResponseDto(
            Disbursement disbursement, LoanAccount account) {

        LoanDeal deal = disbursement.getLoanDeal();
        Customer customer = deal != null ? deal.getCustomer() : null;

        String customerFullName = "N/A";
        if (customer != null) {
            String first = customer.getFirstName() != null
                    ? customer.getFirstName() : "";
            String last = customer.getLastName() != null
                    ? customer.getLastName() : "";
            customerFullName = (first + " " + last).trim();
        }

        return DisbursementResponseDto.builder()
                .disbursementId(disbursement.getId())
                .dealId(deal != null ? deal.getId() : null)
                .loanAccountId(account != null ? account.getId() : null)
                .loanAccountNo(account != null ? account.getLoanAccountNo() : "N/A")
                .customerName(customerFullName)
                .disburseAmount(disbursement.getDisburseAmount())
                .bankPartner(disbursement.getBankPartner())
                .disbursementDate(disbursement.getDisbursementDate())
                .status(disbursement.getStatus())
                .build();
    }
}
