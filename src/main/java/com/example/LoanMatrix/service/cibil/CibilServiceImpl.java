package com.example.LoanMatrix.service.cibil;

import com.example.LoanMatrix.dto.cibilDtos.CibilResponse;
import com.example.LoanMatrix.entity.CibilReport;
import com.example.LoanMatrix.entity.Customer;
import com.example.LoanMatrix.exception.CibilAlreadyGeneratedException;
import com.example.LoanMatrix.exception.CustomerNotFoundException;
import com.example.LoanMatrix.repository.cibil.CibilReportRepository;
import com.example.LoanMatrix.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class CibilServiceImpl implements CibilService {

    private static final Logger logger =
            LoggerFactory.getLogger(CibilServiceImpl.class);

    private final CustomerRepository customerRepository;
    private final CibilReportRepository cibilReportRepository;

    public CibilServiceImpl(CustomerRepository customerRepository,
                            CibilReportRepository cibilReportRepository) {
        this.customerRepository = customerRepository;
        this.cibilReportRepository = cibilReportRepository;
    }

    @Override
    public CibilResponse getCibilDetails(Long customerId) {

        logger.info("Fetching CIBIL details for customer ID: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    logger.warn("Customer not found while fetching CIBIL details. ID: {}",
                            customerId);
                    return new CustomerNotFoundException(
                            "Customer with ID " + customerId + " not found"
                    );
                });

        logger.info("CIBIL details retrieved successfully for customer ID: {}", customerId);

        return CibilResponse.builder()
                .customerName(customer.getFirstName() + " " + customer.getLastName())
                .panCard(customer.getPanCard())
                .build();
    }

    @Override
    public CibilResponse generateCibilScore(Long customerId) {

        logger.info("Starting CIBIL score generation for customer ID: {}", customerId);


        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    logger.warn("Customer not found for CIBIL score generation. ID: {}",
                            customerId);
                    return new CustomerNotFoundException(
                            "Customer with ID " + customerId + " not found"
                    );
                });

        boolean alreadyGenerated =
                cibilReportRepository.existsByCustomer(customer);

        if (alreadyGenerated) {
            logger.warn("CIBIL score has already been generated for customer ID: {}",
                    customerId);
            throw new CibilAlreadyGeneratedException(
                    "CIBIL score has already been generated for this customer."
            );
        }


        BigDecimal monthlyIncome = customer.getMonthlyIncome();
        BigDecimal monthlyInvestment = customer.getMonthlyInvestmentAmount();


        LocalDate today = LocalDate.now();
        int age = today.getYear() - customer.getDateOfBirth().getYear();
        int incomeScore;

        if (monthlyIncome.compareTo(BigDecimal.valueOf(25000)) < 0) {
            incomeScore = 100;
        } else if (monthlyIncome.compareTo(BigDecimal.valueOf(50000)) < 0) {
            incomeScore = 200;
        } else if (monthlyIncome.compareTo(BigDecimal.valueOf(100000)) <= 0) {
            incomeScore = 300;
        } else {
            incomeScore = 400;
        }


        int employmentScore;

        String employmentType = customer.getEmploymentType();

        if ("Government".equalsIgnoreCase(employmentType)) {
            employmentScore = 200;
        } else if ("Private".equalsIgnoreCase(employmentType)) {
            employmentScore = 150;
        } else if ("Self-Employed".equalsIgnoreCase(employmentType)
                || "Business".equalsIgnoreCase(employmentType)) {
            employmentScore = 100;
        } else {
            employmentScore = 0;
        }

        // 7. Calculate Age Score
        int ageScore;

        if (age >= 21 && age <= 24) {
            ageScore = 50;
        } else if (age >= 25 && age <= 45) {
            ageScore = 150;
        } else if (age >= 46 && age <= 60) {
            ageScore = 100;
        } else {
            ageScore = 0;
        }

        double foir = (monthlyInvestment.doubleValue()
                / monthlyIncome.doubleValue()) * 100;


        int foirScore;

        if (foir < 30) {
            foirScore = 250;
        } else if (foir <= 50) {
            foirScore = 150;
        } else if (foir <= 60) {
            foirScore = 75;
        } else {
            foirScore = 0;
        }

        // 10. Calculate final CIBIL score
        int cibilScore = incomeScore
                + employmentScore
                + ageScore
                + foirScore;

        CibilReport cibilReport = new CibilReport();

        cibilReport.setCustomer(customer);
        cibilReport.setPanNo(customer.getPanCard());
        cibilReport.setCibilScore(cibilScore);
        cibilReport.setCheckDate(today);

        cibilReportRepository.save(cibilReport);

        logger.info("CIBIL report generated successfully for customer ID: {}", customerId);


        return CibilResponse.builder()
                .customerName(customer.getFirstName() + " " + customer.getLastName())
                .panCard(customer.getPanCard())
                .cibilScore(cibilScore)
                .checkDate(today)
                .build();
    }
}
