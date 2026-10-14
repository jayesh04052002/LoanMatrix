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

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
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

        return customers;
    }


    @Override
    public EligibilityResponse checkEligibility(Long customerId) {

        // 1. Find customer
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new CustomerNotFoundException(
                                "Customer with ID "
                                        + customerId
                                        + " not found"
                        )
                );

        // 2. Find customer's CIBIL report
        CibilReport cibilReport =
                cibilReportRepository.findByCustomer(customer);

        if (cibilReport == null) {
            throw new RuntimeException(
                    "CIBIL report not found for this customer"
            );
        }

        // 3. Get CIBIL score
        int cibilScore = cibilReport.getCibilScore();


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

        response.setOfficerActionRequired(
                officerActionRequired
        );
        response.setPanCard(cibilReport.getPanNo());

        return response;
    }



    @Override
    public void approveEligibility(Long customerId) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new CustomerNotFoundException(
                                "Customer with ID " + customerId + " not found"
                        )
                );

        EligibilityResult result =
                eligibilityResultRepository.findByCustomer(customer);

        if (result == null) {
            throw new RuntimeException(
                    "Eligibility result not found for this customer"
            );
        }

        // Prevent a second decision
        if (result.getRejectionReason() != null
                || Boolean.TRUE.equals(result.getEligible())) {
            throw new EligibilityDecisionAlreadyMadeException(
                    "Eligibility decision has already been made for this customer"
            );
        }

        result.setEligible(true);
        result.setRejectionReason(null);

        eligibilityResultRepository.save(result);
    }




    @Override
    public void rejectEligibility(Long customerId) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new CustomerNotFoundException(
                                "Customer with ID " + customerId + " not found"
                        )
                );

        EligibilityResult result =
                eligibilityResultRepository.findByCustomer(customer);

        if (result == null) {
            throw new RuntimeException(
                    "Eligibility result not found for this customer"
            );
        }

        // Prevent a second decision
        if (result.getRejectionReason() != null
                || Boolean.TRUE.equals(result.getEligible())) {
            throw new EligibilityDecisionAlreadyMadeException(
                    "Eligibility decision has already been made for this customer"
            );
        }

        result.setEligible(false);
        result.setRejectionReason("Rejected by officer");

        eligibilityResultRepository.save(result);
    }


    @Override
    public CustomerEligibilityResponse getCustomerEligibility(Long customerId) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new CustomerNotFoundException(
                                "Customer with ID " + customerId + " not found"
                        )
                );

        EligibilityResult result =
                eligibilityResultRepository.findByCustomer(customer);

        if (result == null) {
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

        return response;
    }
}
