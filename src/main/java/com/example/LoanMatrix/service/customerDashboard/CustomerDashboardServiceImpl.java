package com.example.LoanMatrix.service.customerDashboard;

import com.example.LoanMatrix.dto.customerDashboardDtos.CustomerDashboardResponse;
import com.example.LoanMatrix.dto.customerDashboardDtos.CustomerLoanAccountResponse;
import com.example.LoanMatrix.dto.customerDashboardDtos.CustomerPaymentResponse;
import com.example.LoanMatrix.dto.customerDashboardDtos.CustomerDocumentResponse;
import com.example.LoanMatrix.entity.Customer;
import com.example.LoanMatrix.entity.LoanAccount;
import com.example.LoanMatrix.entity.EmiSchedule;
import com.example.LoanMatrix.entity.LoanPayment;
import com.example.LoanMatrix.entity.KycDocument;
import com.example.LoanMatrix.exception.CustomerNotFoundException;
import com.example.LoanMatrix.repository.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CustomerDashboardServiceImpl
        implements CustomerDashboardService {

    private final CustomerRepository customerRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final EmiScheduleRepository emiScheduleRepository;
    private final LoanPaymentRepository loanPaymentRepository;
    private final KycDocumentRepository kycDocumentRepository;

    public CustomerDashboardServiceImpl(
            CustomerRepository customerRepository,
            LoanAccountRepository loanAccountRepository,
            EmiScheduleRepository emiScheduleRepository,
            LoanPaymentRepository loanPaymentRepository,
            KycDocumentRepository kycDocumentRepository) {

        this.customerRepository = customerRepository;
        this.loanAccountRepository = loanAccountRepository;
        this.emiScheduleRepository = emiScheduleRepository;
        this.loanPaymentRepository = loanPaymentRepository;
        this.kycDocumentRepository = kycDocumentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDashboardResponse getDashboard(Long customerId) {

        log.info("Fetching dashboard for customer ID: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.warn("Customer not found with ID: {}", customerId);
                    return new CustomerNotFoundException(
                            "Customer with ID " + customerId + " not found"
                    );
                });

        log.debug("Customer found. Fetching loan account details for customer ID: {}", customerId);

        List<LoanAccount> allLoanAccounts =
                loanAccountRepository.findByCustomer(customer);

        List<LoanAccount> activeLoanAccounts =
                loanAccountRepository.findByCustomerAndLoanStatus(
                        customer, "ACTIVE"
                );

        log.info("Found {} total loan accounts and {} active loan accounts for customer ID: {}",
                allLoanAccounts.size(), activeLoanAccounts.size(), customerId);

        List<CustomerLoanAccountResponse> loanAccountResponses =
                new ArrayList<>();

        BigDecimal totalLoanAmount = BigDecimal.ZERO;
        BigDecimal totalOutstandingPrincipal = BigDecimal.ZERO;

        for (LoanAccount loanAccount : activeLoanAccounts) {

            CustomerLoanAccountResponse response =
                    new CustomerLoanAccountResponse();

            response.setLoanAccountId(loanAccount.getId());
            response.setLoanAccountNo(loanAccount.getLoanAccountNo());
            response.setLoanAmount(loanAccount.getLoanAmount());
            response.setOutstandingPrincipal(
                    loanAccount.getOutstandingPrincipal()
            );
            response.setInterestRate(loanAccount.getInterestRate());
            response.setTenureMonths(loanAccount.getTenureMonths());
            response.setEmiAmount(loanAccount.getEmiAmount());
            response.setTotalPaidAmount(loanAccount.getTotalPaidAmount());
            response.setLoanStatus(loanAccount.getLoanStatus());
            response.setDisbursementDate(loanAccount.getDisbursementDate());

            loanAccountResponses.add(response);

            if (loanAccount.getLoanAmount() != null) {
                totalLoanAmount =
                        totalLoanAmount.add(loanAccount.getLoanAmount());
            }

            if (loanAccount.getOutstandingPrincipal() != null) {
                totalOutstandingPrincipal =
                        totalOutstandingPrincipal.add(
                                loanAccount.getOutstandingPrincipal()
                        );
            }
        }

        log.debug("Prepared active loan account responses for customer ID: {}", customerId);

        List<EmiSchedule> emiSchedules =
                emiScheduleRepository.findByLoanAccount_Customer(customer);

        log.info("Found {} EMI schedule records for customer ID: {}",
                emiSchedules.size(), customerId);

        long emisPaid = 0;
        long emisPending = 0;

        EmiSchedule upcomingEmi = null;
        LocalDate today = LocalDate.now();

        for (EmiSchedule emiSchedule : emiSchedules) {

            String paymentStatus = emiSchedule.getPaymentStatus();

            if ("PAID".equalsIgnoreCase(paymentStatus)) {
                emisPaid++;
            }

            if ("PENDING".equalsIgnoreCase(paymentStatus)) {
                emisPending++;

                LoanAccount loanAccount = emiSchedule.getLoanAccount();

                if (loanAccount != null
                        && "ACTIVE".equalsIgnoreCase(
                        loanAccount.getLoanStatus())
                        && emiSchedule.getDueDate() != null
                        && !emiSchedule.getDueDate().isBefore(today)) {

                    if (upcomingEmi == null
                            || emiSchedule.getDueDate().isBefore(
                            upcomingEmi.getDueDate())) {

                        upcomingEmi = emiSchedule;
                    }
                }
            }
        }

        BigDecimal upcomingEmiAmount = null;
        LocalDate upcomingEmiDueDate = null;

        if (upcomingEmi != null) {
            upcomingEmiAmount = upcomingEmi.getEmi();
            upcomingEmiDueDate = upcomingEmi.getDueDate();
        }

        log.info("EMI summary for customer ID {}: paid={}, pending={}, upcoming EMI available={}",
                customerId, emisPaid, emisPending, upcomingEmi != null);

        List<LoanPayment> payments =
                loanPaymentRepository
                        .findByLoanAccount_CustomerOrderByPaymentDateDesc(
                                customer
                        );

        log.debug("Found {} payment records for customer ID: {}",
                payments.size(), customerId);

        BigDecimal totalAmountPaid = BigDecimal.ZERO;

        List<CustomerPaymentResponse> recentPayments =
                new ArrayList<>();

        for (LoanPayment payment : payments) {

            if ("PAID".equalsIgnoreCase(payment.getPaymentStatus())
                    && payment.getPaymentAmount() != null) {

                totalAmountPaid =
                        totalAmountPaid.add(payment.getPaymentAmount());
            }

            if (recentPayments.size() < 5) {

                CustomerPaymentResponse response =
                        new CustomerPaymentResponse();

                response.setPaymentId(payment.getId());
                response.setLoanAccountNo(
                        payment.getLoanAccount() != null
                                ? payment.getLoanAccount().getLoanAccountNo()
                                : null
                );
                response.setPaymentName(payment.getPaymentName());
                response.setPaymentAmount(payment.getPaymentAmount());
                response.setPaymentDate(payment.getPaymentDate());
                response.setPaymentStatus(payment.getPaymentStatus());

                recentPayments.add(response);
            }
        }

        log.debug("Prepared {} recent payment entries for customer ID: {}",
                recentPayments.size(), customerId);

        List<KycDocument> kycDocuments =
                kycDocumentRepository.findByCustomer(customer);

        List<CustomerDocumentResponse> documents =
                new ArrayList<>();

        for (KycDocument kycDocument : kycDocuments) {

            CustomerDocumentResponse response =
                    new CustomerDocumentResponse();

            response.setDocumentId(kycDocument.getId());
            response.setDocumentType(kycDocument.getDocumentType());
            response.setVerificationStatus(
                    kycDocument.getVerificationStatus()
            );

            documents.add(response);
        }

        log.debug("Prepared {} KYC document entries for customer ID: {}",
                documents.size(), customerId);

        CustomerDashboardResponse dashboard =
                new CustomerDashboardResponse();

        dashboard.setTotalActiveLoanAccounts(
                (long) activeLoanAccounts.size()
        );
        dashboard.setLoanAccounts(loanAccountResponses);
        dashboard.setTotalLoanAmount(totalLoanAmount);
        dashboard.setTotalOutstandingPrincipal(
                totalOutstandingPrincipal
        );
        dashboard.setTotalAmountPaid(totalAmountPaid);
        dashboard.setEmisPaid(emisPaid);
        dashboard.setEmisPending(emisPending);
        dashboard.setUpcomingEmiAmount(upcomingEmiAmount);
        dashboard.setUpcomingEmiDueDate(upcomingEmiDueDate);
        dashboard.setRecentPayments(recentPayments);
        dashboard.setDocuments(documents);

        log.info("Customer dashboard prepared successfully for customer ID: {}", customerId);

        return dashboard;
    }
}
