package com.example.LoanMatrix.service.closure;

import com.example.LoanMatrix.dto.closure.PartialClosureRequestDto;
import com.example.LoanMatrix.dto.closure.PartialClosureResponseDto;
import com.example.LoanMatrix.entity.Customer;
import com.example.LoanMatrix.entity.ForeClosureRequest;
import com.example.LoanMatrix.entity.LoanAccount;
import com.example.LoanMatrix.entity.LoanPayment;
import com.example.LoanMatrix.entity.User;
import com.example.LoanMatrix.exception.InvalidLoanOperationException;
import com.example.LoanMatrix.exception.ResourceNotFoundException;
import com.example.LoanMatrix.repository.LoanPaymentRepository;
import com.example.LoanMatrix.repository.UserRepository;
import com.example.LoanMatrix.repository.ForeClosureRequestRepository;
import com.example.LoanMatrix.repository.disbursement.LoanAccountDisbursementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartialClosureServiceImpl implements PartialClosureService {

    private final ForeClosureRequestRepository foreClosureRequestRepository;
    private final LoanAccountDisbursementRepository loanAccountRepository;
    private final LoanPaymentRepository loanPaymentRepository;
    private final UserRepository userRepository;

    private static final List<String> ACTIVE_REQUEST_STATUSES = List.of("Pending", "Approved");

    @Override
    @Transactional
    public PartialClosureResponseDto submitPartialRequest(PartialClosureRequestDto requestDto) {
        log.info("Submitting partial closure request for Loan Account ID: {}", requestDto.getLoanAccountId());

        LoanAccount account = loanAccountRepository.findById(requestDto.getLoanAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan account not found with ID: " + requestDto.getLoanAccountId()));

        if (!"ACTIVE".equalsIgnoreCase(account.getLoanStatus())) {
            throw new InvalidLoanOperationException("Cannot request partial foreclosure. Loan account is " + account.getLoanStatus());
        }

        BigDecimal partialAmount = requestDto.getPartialAmount();
        if (partialAmount == null || partialAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidLoanOperationException("Partial payment amount must be greater than zero.");
        }

        if (partialAmount.compareTo(account.getOutstandingPrincipal()) >= 0) {
            throw new InvalidLoanOperationException("Partial payment cannot equal or exceed total outstanding principal. Use Full Foreclosure instead.");
        }

        // Guard against any request currently in progress (Pending or Approved)
        if (foreClosureRequestRepository.existsByLoanAccount_IdAndStatusInAndPaidFalse(account.getId(), ACTIVE_REQUEST_STATUSES)) {
            throw new InvalidLoanOperationException("A closure request is already in progress for this loan account.");
        }

        ForeClosureRequest request = new ForeClosureRequest();
        request.setLoanAccount(account);
        request.setForeClosureType("Partial");
        request.setPartialAmount(partialAmount);
        request.setForeClosureAmount(partialAmount);
        request.setRequestedDate(LocalDate.now());
        request.setExpectedClosureDate(requestDto.getExpectedClosureDate() != null ? requestDto.getExpectedClosureDate() : LocalDate.now());
        request.setReason(requestDto.getReason());
        request.setStatus("Pending");
        request.setPaid(false);

        ForeClosureRequest saved = foreClosureRequestRepository.save(request);
        log.info("Partial closure request submitted with ID: {}", saved.getId());

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public PartialClosureResponseDto approvePartialRequest(Long requestId, Long officerId) {
        log.info("Officer ID: {} reviewing Partial Request ID: {}", officerId, requestId);

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
        log.info("Partial Request ID: {} approved successfully.", updated.getId());

        return mapToDto(updated);
    }

    @Override
    @Transactional
    public PartialClosureResponseDto processPartialPayment(Long requestId) {
        log.info("Processing payment for Foreclosure Request ID: {}", requestId);

        ForeClosureRequest request = foreClosureRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Foreclosure request not found with ID: " + requestId));

        if (!"Approved".equalsIgnoreCase(request.getStatus())) {
            throw new InvalidLoanOperationException("Payment can only be processed for Approved requests. Current status: " + request.getStatus());
        }

        if (Boolean.TRUE.equals(request.getPaid())) {
            throw new InvalidLoanOperationException("This partial payment has already been settled.");
        }

        LoanAccount account = request.getLoanAccount();
        if (!"ACTIVE".equalsIgnoreCase(account.getLoanStatus())) {
            throw new InvalidLoanOperationException("Cannot apply payment: Loan account status is " + account.getLoanStatus());
        }

        BigDecimal paymentAmount = request.getPartialAmount();

        // 1. Record Loan Payment entry
        LoanPayment payment = new LoanPayment();
        payment.setLoanAccount(account);
        payment.setPaymentAmount(paymentAmount);
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentStatus("SUCCESS");
        payment.setPaymentName("PARTIAL_PREPAYMENT");
        loanPaymentRepository.save(payment);

        // 2. Adjust Loan Account balances
        BigDecimal currentOutstanding = account.getOutstandingPrincipal() != null ? account.getOutstandingPrincipal() : BigDecimal.ZERO;
        BigDecimal updatedOutstanding = currentOutstanding.subtract(paymentAmount);
        account.setOutstandingPrincipal(updatedOutstanding);

        BigDecimal currentPaid = account.getTotalPaidAmount() != null ? account.getTotalPaidAmount() : BigDecimal.ZERO;
        account.setTotalPaidAmount(currentPaid.add(paymentAmount));
        loanAccountRepository.save(account);

        // 3. Mark Request as Completed
        request.setPaid(true);
        request.setPaidDate(LocalDate.now());
        request.setStatus("Completed");
        ForeClosureRequest completedRequest = foreClosureRequestRepository.save(request);

        log.info("Partial payment applied. Updated Outstanding Principal: {}", updatedOutstanding);

        return mapToDto(completedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartialClosureResponseDto> getPendingRequests() {
        return foreClosureRequestRepository.findByStatusOrderByRequestedDateDesc("Pending")
                .stream()
                .filter(req -> "Partial".equalsIgnoreCase(req.getForeClosureType()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartialClosureResponseDto> getRequestsByLoanAccountId(Long loanAccountId) {
        return foreClosureRequestRepository.findByLoanAccount_IdOrderByRequestedDateDesc(loanAccountId)
                .stream()
                .filter(req -> "Partial".equalsIgnoreCase(req.getForeClosureType()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private PartialClosureResponseDto mapToDto(ForeClosureRequest req) {
        LoanAccount acc = req.getLoanAccount();
        Customer c = acc != null ? acc.getCustomer() : null;
        String customerName = "N/A";
        if (c != null) {
            String first = c.getFirstName() != null ? c.getFirstName() : "";
            String last = c.getLastName() != null ? c.getLastName() : "";
            customerName = (first + " " + last).trim();
        }

        return PartialClosureResponseDto.builder()
                .requestId(req.getId())
                .loanAccountId(acc != null ? acc.getId() : null)
                .loanAccountNo(acc != null ? acc.getLoanAccountNo() : "N/A")
                .customerName(customerName)
                .foreClosureType(req.getForeClosureType())
                .partialAmount(req.getPartialAmount())
                .remainingOutstandingPrincipal(acc != null ? acc.getOutstandingPrincipal() : null)
                .totalPaidAmount(acc != null ? acc.getTotalPaidAmount() : null)
                .status(req.getStatus())
                .isPaid(req.getPaid())
                .requestedDate(req.getRequestedDate())
                .approvedDate(req.getApprovedDate())
                .paidDate(req.getPaidDate())
                .reason(req.getReason())
                .build();
    }
}