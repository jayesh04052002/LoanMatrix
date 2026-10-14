package com.example.LoanMatrix.service.officerDashboard;

import com.example.LoanMatrix.dto.officerDashboardDtos.OfficerDashboardResponse;
import com.example.LoanMatrix.entity.LoanAccount;
import com.example.LoanMatrix.entity.LoanPayment;
import com.example.LoanMatrix.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class OfficerDashboardServiceImpl implements OfficerDashboardService {

    private final CustomerRepository customerRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final LoanPaymentRepository loanPaymentRepository;
    private final EmiScheduleRepository emiScheduleRepository;
    private final ForeClosureRequestRepository foreClosureRequestRepository;

    public OfficerDashboardServiceImpl(
            CustomerRepository customerRepository,
            KycDocumentRepository kycDocumentRepository,
            LoanAccountRepository loanAccountRepository,
            LoanPaymentRepository loanPaymentRepository,
            EmiScheduleRepository emiScheduleRepository,
            ForeClosureRequestRepository foreClosureRequestRepository) {

        this.customerRepository = customerRepository;
        this.kycDocumentRepository = kycDocumentRepository;
        this.loanAccountRepository = loanAccountRepository;
        this.loanPaymentRepository = loanPaymentRepository;
        this.emiScheduleRepository = emiScheduleRepository;
        this.foreClosureRequestRepository = foreClosureRequestRepository;

        log.info("OfficerDashboardServiceImpl initialized successfully");
    }

    @Override
    public OfficerDashboardResponse getDashboard() {

        log.info("Fetching officer dashboard data");

        // Total Customers
        long totalCustomers = customerRepository.count();
        log.debug("Total customers fetched: {}", totalCustomers);

        // Customers having a Loan Account
        long loanCustomers =
                loanAccountRepository.countDistinctCustomerBy();
        log.debug("Customers having loan accounts: {}", loanCustomers);

        // Approved KYC Customers
        long approvedKyc =
                kycDocumentRepository
                        .countDistinctCustomerByVerificationStatus("APPROVED");
        log.debug("Customers with approved KYC: {}", approvedKyc);

        // Loan Accounts
        List<LoanAccount> loanAccounts =
                loanAccountRepository.findAll();
        log.debug("Total loan accounts fetched: {}", loanAccounts.size());

        // Total Loan Amount
        BigDecimal totalLoanAmount = BigDecimal.ZERO;

        for (LoanAccount loanAccount : loanAccounts) {

            if (loanAccount.getLoanAmount() != null) {

                totalLoanAmount =
                        totalLoanAmount.add(
                                loanAccount.getLoanAmount()
                        );
            }
        }

        log.debug("Total loan amount calculation completed");

        // Active Loans
        long activeLoans =
                loanAccountRepository.countByLoanStatus("ACTIVE");
        log.debug("Active loans count: {}", activeLoans);

        // Today's Collection
        List<LoanPayment> payments =
                loanPaymentRepository.findAll();

        log.debug("Total loan payment records fetched: {}", payments.size());

        BigDecimal todaysCollection = BigDecimal.ZERO;

        for (LoanPayment payment : payments) {

            if (payment.getPaymentDate() != null
                    && payment.getPaymentDate().equals(LocalDate.now())
                    && payment.getPaymentAmount() != null) {

                todaysCollection =
                        todaysCollection.add(
                                payment.getPaymentAmount()
                        );
            }
        }

        log.debug("Today's collection calculation completed");

        // Pending EMI
        long pendingEmi =
                emiScheduleRepository.countByPaymentStatus("PENDING");
        log.debug("Pending EMI count: {}", pendingEmi);

        // Pending Foreclosure Requests
        long foreclosureRequests =
                foreClosureRequestRepository.countByStatus("PENDING");
        log.debug("Pending foreclosure requests count: {}", foreclosureRequests);

        OfficerDashboardResponse response = new OfficerDashboardResponse(
                totalCustomers,
                loanCustomers,
                approvedKyc,
                totalLoanAmount,
                activeLoans,
                todaysCollection,
                pendingEmi,
                foreclosureRequests
        );

        log.info("Officer dashboard data fetched successfully");

        return response;
    }
}
