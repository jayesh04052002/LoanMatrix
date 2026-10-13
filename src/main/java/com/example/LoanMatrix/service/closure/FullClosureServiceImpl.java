package com.example.LoanMatrix.service.closure;

import com.example.LoanMatrix.dto.closure.FullClosureRequestDto;
import com.example.LoanMatrix.dto.closure.FullClosureResponseDto;
import com.example.LoanMatrix.entity.*;
import com.example.LoanMatrix.exception.InvalidLoanOperationException;
import com.example.LoanMatrix.exception.ResourceNotFoundException;
import com.example.LoanMatrix.repository.LoanPaymentRepository;
import com.example.LoanMatrix.repository.UserRepository;
import com.example.LoanMatrix.repository.ForeClosureRequestRepository;
import com.example.LoanMatrix.repository.closure.LoanClosureRepository;
import com.example.LoanMatrix.repository.disbursement.LoanAccountDisbursementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FullClosureServiceImpl implements FullClosureService {

    private final ForeClosureRequestRepository foreClosureRequestRepository;
    private final LoanAccountDisbursementRepository loanAccountRepository;
    private final LoanClosureRepository loanClosureRepository;
    private final LoanPaymentRepository loanPaymentRepository;
    private final UserRepository userRepository;

    private static final BigDecimal PENALTY_RATE = new BigDecimal("0.02");       // 2% penalty
    private static final BigDecimal THRESHOLD_RATIO = new BigDecimal("0.30");     // 30% remaining threshold
    private static final List<String> ACTIVE_REQUEST_STATUSES = List.of("Pending", "Approved");

    @Override
    @Transactional
    public FullClosureResponseDto submitFullRequest(FullClosureRequestDto requestDto) {
        log.info("Initiating Full Foreclosure for Loan Account ID: {}", requestDto.getLoanAccountId());

        LoanAccount account = loanAccountRepository.findById(requestDto.getLoanAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan account not found with ID: " + requestDto.getLoanAccountId()));

        if (!"ACTIVE".equalsIgnoreCase(account.getLoanStatus())) {
            throw new InvalidLoanOperationException("Cannot request foreclosure. Account status is: " + account.getLoanStatus());
        }

        // Guard against any request currently in progress (Pending or Approved)
        if (foreClosureRequestRepository.existsByLoanAccount_IdAndStatusInAndPaidFalse(account.getId(), ACTIVE_REQUEST_STATUSES)) {
            throw new InvalidLoanOperationException("A closure request is already in progress for this loan account.");
        }

        BigDecimal originalLoan = account.getLoanAmount();
        BigDecimal outstanding = account.getOutstandingPrincipal();

        if (outstanding == null || outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidLoanOperationException("Account has no outstanding principal to foreclose.");
        }

        // Rule: If outstanding / originalLoan > 0.30, charge 2% penalty
        BigDecimal remainingRatio = outstanding.divide(originalLoan, 4, RoundingMode.HALF_UP);
        BigDecimal penalty = BigDecimal.ZERO;
        if (remainingRatio.compareTo(THRESHOLD_RATIO) > 0) {
            penalty = outstanding.multiply(PENALTY_RATE).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalSettlement = outstanding.add(penalty);

        ForeClosureRequest request = new ForeClosureRequest();
        request.setLoanAccount(account);
        request.setForeClosureType("Full");
        request.setForeClosureAmount(totalSettlement);
        request.setPartialAmount(null);
        request.setRequestedDate(LocalDate.now());
        request.setExpectedClosureDate(requestDto.getExpectedClosureDate() != null ? requestDto.getExpectedClosureDate() : LocalDate.now());
        request.setReason(requestDto.getReason());
        request.setStatus("Pending");
        request.setPaid(false);

        ForeClosureRequest saved = foreClosureRequestRepository.save(request);
        log.info("Full Foreclosure request saved with ID: {}, Total Amount: {}", saved.getId(), totalSettlement);

        return mapToDto(saved, penalty, outstanding);
    }

    @Override
    @Transactional
    public FullClosureResponseDto approveFullRequest(Long requestId, Long officerId) {
        log.info("Officer ID: {} approving Full Foreclosure Request ID: {}", officerId, requestId);

        ForeClosureRequest request = foreClosureRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Foreclosure request not found with ID: " + requestId));

        if (!"Pending".equalsIgnoreCase(request.getStatus())) {
            throw new InvalidLoanOperationException("Only Pending requests can be approved. Current status: " + request.getStatus());
        }

        User officer = userRepository.findById(officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Officer not found with ID: " + officerId));

        request.setStatus("Approved");
        request.setApprovedDate(LocalDate.now());
        request.setClosedBy(officer);

        ForeClosureRequest updated = foreClosureRequestRepository.save(request);

        LoanAccount account = updated.getLoanAccount();
        BigDecimal penalty = calculatePenaltyFromAccount(account, updated.getForeClosureAmount());
        return mapToDto(updated, penalty, account.getOutstandingPrincipal());
    }

    @Override
    @Transactional
    public FullClosureResponseDto processFullPayment(Long requestId) {
        log.info("Settling payment for Full Foreclosure Request ID: {}", requestId);

        ForeClosureRequest request = foreClosureRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Foreclosure request not found with ID: " + requestId));

        if (!"Approved".equalsIgnoreCase(request.getStatus())) {
            throw new InvalidLoanOperationException("Payment can only be processed for Approved requests. Current status: " + request.getStatus());
        }

        if (Boolean.TRUE.equals(request.getPaid())) {
            throw new InvalidLoanOperationException("This full foreclosure payment has already been completed.");
        }

        LoanAccount account = request.getLoanAccount();
        if (!"ACTIVE".equalsIgnoreCase(account.getLoanStatus())) {
            throw new InvalidLoanOperationException("Cannot process payment. Loan account status is already: " + account.getLoanStatus());
        }

        BigDecimal settlementAmount = request.getForeClosureAmount();

        // 1. Log Payment Entry
        LoanPayment payment = new LoanPayment();
        payment.setLoanAccount(account);
        payment.setPaymentAmount(settlementAmount);
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentStatus("SUCCESS");
        payment.setPaymentName("FULL_FORECLOSURE");
        loanPaymentRepository.save(payment);

        // 2. Terminate Account in loan_accounts
        BigDecimal currentPaid = account.getTotalPaidAmount() != null ? account.getTotalPaidAmount() : BigDecimal.ZERO;
        account.setTotalPaidAmount(currentPaid.add(settlementAmount));
        account.setOutstandingPrincipal(BigDecimal.ZERO);
        account.setLoanStatus("CLOSED");
        loanAccountRepository.save(account);

        // 3. Insert Permanent Record into loan_closures
        LoanClosure closure = new LoanClosure();
        closure.setLoanAccount(account);
        closure.setClosureType("FORECLOSURE");
        closure.setFinalSettlementAmount(settlementAmount);
        closure.setClosureDate(LocalDate.now());
        closure.setClosedBy(request.getClosedBy());
        closure.setRemarks(request.getReason() != null ? request.getReason() : "Full settlement processed via foreclosure");
        closure.setClosureStatus("CLOSED");
        loanClosureRepository.save(closure);

        // 4. Mark Request as Completed
        request.setPaid(true);
        request.setPaidDate(LocalDate.now());
        request.setStatus("Completed");
        ForeClosureRequest completedRequest = foreClosureRequestRepository.save(request);

        log.info("Loan Account ID: {} successfully CLOSED. Balance: 0.00", account.getId());

        return mapToDto(completedRequest, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FullClosureResponseDto> getPendingRequests() {
        return foreClosureRequestRepository.findByStatusOrderByRequestedDateDesc("Pending")
                .stream()
                .filter(req -> "Full".equalsIgnoreCase(req.getForeClosureType()))
                .map(req -> {
                    BigDecimal penalty = calculatePenaltyFromAccount(req.getLoanAccount(), req.getForeClosureAmount());
                    BigDecimal principal = req.getLoanAccount() != null ? req.getLoanAccount().getOutstandingPrincipal() : BigDecimal.ZERO;
                    return mapToDto(req, penalty, principal);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FullClosureResponseDto> getPayableRequestsForCustomer(Long loanAccountId) {
        return foreClosureRequestRepository.findByLoanAccount_IdOrderByRequestedDateDesc(loanAccountId)
                .stream()
                .filter(req -> "Full".equalsIgnoreCase(req.getForeClosureType())
                        && "Approved".equalsIgnoreCase(req.getStatus())
                        && !Boolean.TRUE.equals(req.getPaid()))
                .map(req -> {
                    BigDecimal penalty = calculatePenaltyFromAccount(req.getLoanAccount(), req.getForeClosureAmount());
                    BigDecimal principal = req.getLoanAccount() != null ? req.getLoanAccount().getOutstandingPrincipal() : BigDecimal.ZERO;
                    return mapToDto(req, penalty, principal);
                })
                .collect(Collectors.toList());
    }

    private BigDecimal calculatePenaltyFromAccount(LoanAccount account, BigDecimal totalSettlement) {
        if (account == null || totalSettlement == null || account.getOutstandingPrincipal() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal penalty = totalSettlement.subtract(account.getOutstandingPrincipal());
        return penalty.compareTo(BigDecimal.ZERO) > 0 ? penalty : BigDecimal.ZERO;
    }

    private FullClosureResponseDto mapToDto(ForeClosureRequest req, BigDecimal penalty, BigDecimal outstanding) {
        LoanAccount acc = req.getLoanAccount();
        Customer c = acc != null ? acc.getCustomer() : null;
        String customerName = "N/A";
        if (c != null) {
            String first = c.getFirstName() != null ? c.getFirstName() : "";
            String last = c.getLastName() != null ? c.getLastName() : "";
            customerName = (first + " " + last).trim();
        }

        return FullClosureResponseDto.builder()
                .requestId(req.getId())
                .loanAccountId(acc != null ? acc.getId() : null)
                .loanAccountNo(acc != null ? acc.getLoanAccountNo() : "N/A")
                .customerName(customerName)
                .foreClosureType(req.getForeClosureType())
                .outstandingPrincipal(outstanding)
                .penaltyAmount(penalty)
                .totalSettlementAmount(req.getForeClosureAmount())
                .status(req.getStatus())
                .isPaid(req.getPaid())
                .requestedDate(req.getRequestedDate())
                .approvedDate(req.getApprovedDate())
                .reason(req.getReason())
                .build();
    }
}