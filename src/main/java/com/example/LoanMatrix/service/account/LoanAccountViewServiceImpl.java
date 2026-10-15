
        package com.example.LoanMatrix.service.account;

import com.example.LoanMatrix.dto.account.LoanAccountProfileDto;
import com.example.LoanMatrix.dto.account.LoanAccountSummaryDto;
import com.example.LoanMatrix.entity.Customer;
import com.example.LoanMatrix.entity.LoanAccount;
import com.example.LoanMatrix.exception.ResourceNotFoundException;
import com.example.LoanMatrix.repository.account.LoanAccountQueryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanAccountViewServiceImpl implements LoanAccountViewService {

    private static final Logger logger =
            LoggerFactory.getLogger(LoanAccountViewServiceImpl.class);

    private final LoanAccountQueryRepository loanAccountQueryRepository;

    @Override
    public List<LoanAccountSummaryDto> getAllAccountsForOfficer(String statusFilter) {

        logger.info("Fetching loan accounts for officer. Status filter: {}", statusFilter);

        List<LoanAccount> accounts;

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"All Status".equalsIgnoreCase(statusFilter)) {
            accounts = loanAccountQueryRepository.findByLoanStatusOrderByCreatedAtDesc(statusFilter);
        } else {
            accounts = loanAccountQueryRepository.findAllByOrderByCreatedAtDesc();
        }

        logger.info("Retrieved {} loan accounts for officer", accounts.size());

        return accounts.stream()
                .map(this::mapToSummaryDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<LoanAccountSummaryDto> getAccountsForCustomer(Long customerId) {

        logger.info("Fetching loan accounts for customer ID: {}", customerId);

        List<LoanAccountSummaryDto> accounts =
                loanAccountQueryRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId)
                        .stream()
                        .map(this::mapToSummaryDto)
                        .collect(Collectors.toList());

        logger.info("Retrieved {} loan accounts for customer ID: {}",
                accounts.size(), customerId);

        return accounts;
    }

    @Override
    public LoanAccountProfileDto getAccountDetailsByAccountNo(String loanAccountNo) {

        logger.info("Fetching loan account details");

        LoanAccount account = loanAccountQueryRepository.findByLoanAccountNo(loanAccountNo)
                .orElseThrow(() -> {
                    logger.warn("Loan account not found");
                    return new ResourceNotFoundException(
                            "Loan account not found with account no: " + loanAccountNo);
                });

        LoanAccountProfileDto profile = LoanAccountProfileDto.builder()
                .id(account.getId())
                .loanAccountNo(account.getLoanAccountNo())
                .customerName(formatCustomerName(account.getCustomer()))
                .loanAmount(account.getLoanAmount())
                .outstandingPrincipal(account.getOutstandingPrincipal())
                .loanStatus(account.getLoanStatus())
                .interestRate(account.getInterestRate())
                .tenureMonths(account.getTenureMonths())
                .emiAmount(account.getEmiAmount())
                .totalPaidAmount(account.getTotalPaidAmount())
                .disbursementDate(account.getDisbursementDate())
                .createdAt(account.getCreatedAt())
                .build();

        logger.info("Loan account details retrieved successfully. Account ID: {}",
                account.getId());

        return profile;
    }

    private LoanAccountSummaryDto mapToSummaryDto(LoanAccount acc) {
        return LoanAccountSummaryDto.builder()
                .id(acc.getId())
                .loanAccountNo(acc.getLoanAccountNo())
                .customerName(formatCustomerName(acc.getCustomer()))
                .loanAmount(acc.getLoanAmount())
                .outstandingPrincipal(acc.getOutstandingPrincipal())
                .status(acc.getLoanStatus())
                .interestRate(acc.getInterestRate())
                .tenureMonths(acc.getTenureMonths())
                .emiAmount(acc.getEmiAmount())
                .disbursementDate(acc.getDisbursementDate())
                .totalPaid(acc.getTotalPaidAmount())
                .build();
    }

    private String formatCustomerName(Customer customer) {
        if (customer == null) return "N/A";
        String first = customer.getFirstName() != null ? customer.getFirstName() : "";
        String last = customer.getLastName() != null ? customer.getLastName() : "";
        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? "N/A" : fullName;
    }
}
