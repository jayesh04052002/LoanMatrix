package com.example.LoanMatrix.service.officerDashboard;

import com.example.LoanMatrix.dto.officerDashboardDtos.OfficerDashboardResponse;
import com.example.LoanMatrix.entity.LoanAccount;
import com.example.LoanMatrix.entity.LoanPayment;
import com.example.LoanMatrix.repository.dashboard.*;
import com.example.LoanMatrix.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
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
    }

    @Override
    public OfficerDashboardResponse getDashboard() {

        // Total Customers
        long totalCustomers = customerRepository.count();

        // Customers having a Loan Account
        long loanCustomers =
                loanAccountRepository.countDistinctCustomerBy();

        // Approved KYC Customers
        long approvedKyc =
                kycDocumentRepository
                        .countDistinctCustomerByVerificationStatus("APPROVED");

        // Loan Accounts
        List<LoanAccount> loanAccounts =
                loanAccountRepository.findAll();

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

        // Active Loans
        long activeLoans =
                loanAccountRepository.countByLoanStatus("ACTIVE");

        // Today's Collection
        List<LoanPayment> payments =
                loanPaymentRepository.findAll();

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

        // Pending EMI
        long pendingEmi =
                emiScheduleRepository.countByPaymentStatus("PENDING");

        // Pending Foreclosure Requests
        long foreclosureRequests =
                foreClosureRequestRepository.countByStatus("PENDING");

        return new OfficerDashboardResponse(
                totalCustomers,
                loanCustomers,
                approvedKyc,
                totalLoanAmount,
                activeLoans,
                todaysCollection,
                pendingEmi,
                foreclosureRequests
        );
    }
}
