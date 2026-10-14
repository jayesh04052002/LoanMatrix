package com.example.LoanMatrix.service.eligibility;

import com.example.LoanMatrix.dto.eligibilityDtos.CustomerEligibilityResponse;
import com.example.LoanMatrix.dto.eligibilityDtos.EligibilityResponse;
import com.example.LoanMatrix.dto.eligibilityDtos.PendingCustomerResponse;
import com.example.LoanMatrix.entity.CibilReport;
import com.example.LoanMatrix.entity.Customer;
import com.example.LoanMatrix.entity.EligibilityResult;
import com.example.LoanMatrix.exception.CustomerNotFoundException;
import com.example.LoanMatrix.exception.EligibilityDecisionAlreadyMadeException;
import com.example.LoanMatrix.repository.cibil.CibilReportRepository;
import com.example.LoanMatrix.repository.CustomerRepository;
import com.example.LoanMatrix.repository.eligibility.EligibilityResultRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class EligibilityServiceImpl implements EligibilityService {

    private final CustomerRepository customerRepository;
    private final CibilReportRepository cibilReportRepository;
    private final EligibilityResultRepository eligibilityResultRepository;

    public EligibilityServiceImpl(
            CustomerRepository customerRepository,
            CibilReportRepository cibilReportRepository,
            EligibilityResultRepository eligibilityResultRepository) {

        this.customerRepository = customerRepository;
        this.cibilReportRepository = cibilReportRepository;
        this.eligibilityResultRepository = eligibilityResultRepository;
    }

    @Override
    public List<PendingCustomerResponse> getPendingCustomers() {
        log.info("Fetching customers whose eligibility has not been checked.");

        List<CibilReport> reports = cibilReportRepository.findAll();

        List<PendingCustomerResponse> customers = new ArrayList<>();

        for (CibilReport report : reports) {

            Customer customer = report.getCustomer();

            EligibilityResult result =
                    eligibilityResultRepository.findByCustomer(customer);

            // Only show customers waiting for an officer's decision
            if (result == null
                    || !Boolean.FALSE.equals(result.getEligible())
                    || result.getRejectionReason() != null) {
                continue;
            }

            PendingCustomerResponse response =
                    new PendingCustomerResponse();

            response.setCustomerId(customer.getId());

            response.setCustomerName(
                    customer.getFirstName() + " " + customer.getLastName()
            );

            customers.add(response);
        }

        log.info("Pending eligibility customers fetched. Count: {}", customers.size());

        return customers;
    }

    @Override
    public EligibilityResponse checkEligibility(Long customerId) {
        log.info("Checking loan eligibility for Customer ID: {}", customerId);

        // 1. Find customer
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.warn("Customer not found. Customer ID: {}", customerId);
                    return new CustomerNotFoundException(
                            "Customer with ID " + customerId + " not found"
                    );
                });

        // 2. Find customer's CIBIL report
        CibilReport cibilReport =
                cibilReportRepository.findByCustomer(customer);

        if (cibilReport == null) {
            log.warn("CIBIL report not found for Customer ID: {}", customerId);
            throw new RuntimeException(
                    "CIBIL report not found for this customer"
            );
        }

        // 3. Get CIBIL score
        int cibilScore = cibilReport.getCibilScore();

        log.debug("CIBIL score retrieved for Customer ID: {}", customerId);

        // 4. Find CIBIL range
        String cibilRange;

        if (cibilScore >= 900) {
            cibilRange = "Excellent";
        } else if (cibilScore >= 800) {
            cibilRange = "Very Good";
        } else if (cibilScore >= 750) {
            cibilRange = "Good";
        } else if (cibilScore >= 700) {
            cibilRange = "Average";
        } else if (cibilScore >= 650) {
            cibilRange = "Risky";
        } else {
            cibilRange = "Reject";
        }

        // 5. Calculate Eligible Loan Amount
        BigDecimal eligibleLoanAmount = null;

        if (cibilScore >= 900) {
            // Above 1 Crore
            eligibleLoanAmount = new BigDecimal("10000000");
        } else if (cibilScore >= 800) {
            // 75 Lakh - 1 Crore
            eligibleLoanAmount = new BigDecimal("10000000");
        } else if (cibilScore >= 750) {
            // 50 Lakh - 75 Lakh
            eligibleLoanAmount = new BigDecimal("7500000");
        } else if (cibilScore >= 700) {
            // 25 Lakh - 50 Lakh
            eligibleLoanAmount = new BigDecimal("5000000");
        } else if (cibilScore >= 650) {
            // 10 Lakh - 25 Lakh
            eligibleLoanAmount = new BigDecimal("2500000");
        } else {
            // Below 650
            // Rejected
            eligibleLoanAmount = null;
        }

        // 6. Eligibility decision
        boolean eligible;
        boolean autoApproved;
        boolean officerActionRequired;

        if (cibilScore >= 800) {
            // 800 or above
            // Automatically approved

            eligible = true;
            autoApproved = true;
            officerActionRequired = false;

        } else {
            // Below 800
            // Officer has to decide

            eligible = false;
            autoApproved = false;
            officerActionRequired = true;
        }

        log.info("Eligibility evaluated for Customer ID: {}. CIBIL range: {}",
                customerId, cibilRange);

        // 7. Save eligibility result
        EligibilityResult result =
                eligibilityResultRepository.findByCustomer(customer);

        if (result == null) {
            result = new EligibilityResult();
            result.setCustomer(customer);
        }

        result.setCibilScore(cibilScore);
        result.setEligible(eligible);
        result.setEligibleLoanAmount(eligibleLoanAmount);
        result.setRejectionReason(null);

        eligibilityResultRepository.save(result);

        log.info("Eligibility result saved for Customer ID: {}", customerId);

        // 8. Prepare response
        EligibilityResponse response =
                new EligibilityResponse();

        response.setCustomerId(customer.getId());

        response.setCustomerName(
                customer.getFirstName()
                        + " "
                        + customer.getLastName()
        );

        response.setCibilScore(cibilScore);
        response.setCibilRange(cibilRange);
        response.setEligibleLoanAmount(eligibleLoanAmount);
        response.setEligible(eligible);
        response.setAutoApproved(autoApproved);
        response.setOfficerActionRequired(officerActionRequired);
        response.setPanCard(cibilReport.getPanNo());

        log.info("Eligibility response prepared for Customer ID: {}", customerId);

        return response;
    }

    @Override
    public void approveEligibility(Long customerId) {
        log.info("Processing eligibility approval for Customer ID: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.warn("Customer not found for eligibility approval. Customer ID: {}",
                            customerId);
                    return new CustomerNotFoundException(
                            "Customer with ID " + customerId + " not found"
                    );
                });

        EligibilityResult result =
                eligibilityResultRepository.findByCustomer(customer);

        if (result == null) {
            log.warn("Eligibility result not found for Customer ID: {}", customerId);
            throw new RuntimeException(
                    "Eligibility result not found for this customer"
            );
        }

        // Prevent a second decision
        if (result.getRejectionReason() != null
                || Boolean.TRUE.equals(result.getEligible())) {
            log.warn("Eligibility decision has already been made for Customer ID: {}",
                    customerId);
            throw new EligibilityDecisionAlreadyMadeException(
                    "Eligibility decision has already been made for this customer"
            );
        }

        result.setEligible(true);
        result.setRejectionReason(null);

        eligibilityResultRepository.save(result);

        log.info("Eligibility approved by officer for Customer ID: {}", customerId);
    }

    @Override
    public void rejectEligibility(Long customerId) {
        log.info("Processing eligibility rejection for Customer ID: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.warn("Customer not found for eligibility rejection. Customer ID: {}",
                            customerId);
                    return new CustomerNotFoundException(
                            "Customer with ID " + customerId + " not found"
                    );
                });

        EligibilityResult result =
                eligibilityResultRepository.findByCustomer(customer);

        if (result == null) {
            log.warn("Eligibility result not found for Customer ID: {}", customerId);
            throw new RuntimeException(
                    "Eligibility result not found for this customer"
            );
        }

        // Prevent a second decision
        if (result.getRejectionReason() != null
                || Boolean.TRUE.equals(result.getEligible())) {
            log.warn("Eligibility decision has already been made for Customer ID: {}",
                    customerId);
            throw new EligibilityDecisionAlreadyMadeException(
                    "Eligibility decision has already been made for this customer"
            );
        }

        result.setEligible(false);
        result.setRejectionReason("Rejected by officer");

        eligibilityResultRepository.save(result);

        log.info("Eligibility rejected by officer for Customer ID: {}", customerId);
    }

    @Override
    public CustomerEligibilityResponse getCustomerEligibility(Long customerId) {
        log.info("Fetching eligibility details for Customer ID: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.warn("Customer not found while fetching eligibility. Customer ID: {}",
                            customerId);
                    return new CustomerNotFoundException(
                            "Customer with ID " + customerId + " not found"
                    );
                });

        EligibilityResult result =
                eligibilityResultRepository.findByCustomer(customer);

        if (result == null) {
            log.warn("Eligibility result not found for Customer ID: {}", customerId);
            throw new RuntimeException(
                    "Eligibility result not found for this customer"
            );
        }

        CustomerEligibilityResponse response =
                new CustomerEligibilityResponse();

        response.setCustomerId(customer.getId());

        response.setCustomerName(
                customer.getFirstName() + " " + customer.getLastName()
        );

        response.setCibilScore(result.getCibilScore());
        response.setEligible(result.getEligible());

        // Pending: not eligible yet, and no rejection decision exists
        if (Boolean.FALSE.equals(result.getEligible())
                && result.getRejectionReason() == null) {

            response.setEligibleLoanAmount(null);
            response.setRejectionReason(null);

            return response;
        }

        // Approved
        if (Boolean.TRUE.equals(result.getEligible())) {
            response.setEligibleLoanAmount(
                    result.getEligibleLoanAmount()
            );

            response.setRejectionReason(null);

        } else {

            // Rejected by officer
            response.setEligibleLoanAmount(BigDecimal.ZERO);


            response.setRejectionReason(
                    result.getRejectionReason()
            );
        }

        log.info("Eligibility details fetched successfully for Customer ID: {}",
                customerId);

        return response;
    }
}
