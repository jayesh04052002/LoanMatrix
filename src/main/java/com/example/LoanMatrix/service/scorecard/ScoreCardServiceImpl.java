package com.example.LoanMatrix.service.scorecard;

import com.example.LoanMatrix.dto.scorecardDtos.ScoreCardResponse;
import com.example.LoanMatrix.entity.EligibilityResult;
import com.example.LoanMatrix.repository.eligibility.EligibilityResultRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ScoreCardServiceImpl implements ScoreCardService {

    private final EligibilityResultRepository eligibilityResultRepository;

    public ScoreCardServiceImpl(
            EligibilityResultRepository eligibilityResultRepository) {
        this.eligibilityResultRepository = eligibilityResultRepository;
        log.info("ScoreCardServiceImpl initialized successfully");
    }

    @Override
    public Page<ScoreCardResponse> getEligibleCustomers(int page) {

        log.info("Fetching eligible customers for page: {}", page);

        Page<EligibilityResult> results =
                eligibilityResultRepository.findByEligibleTrue(
                        PageRequest.of(page, 5)
                );

        log.debug("Eligible customer records fetched: {}", results.getNumberOfElements());

        Page<ScoreCardResponse> responsePage = results.map(result -> {

            ScoreCardResponse response =
                    new ScoreCardResponse();

            response.setCustomerId(
                    result.getCustomer().getId()
            );

            response.setCustomerName(
                    result.getCustomer().getFirstName()
                            + " "
                            + result.getCustomer().getLastName()
            );

            response.setPanCard(
                    result.getCustomer().getPanCard()
            );

            response.setCibilScore(
                    result.getCibilScore()
            );

            response.setEligibleLoanAmount(
                    result.getEligibleLoanAmount()
            );

            response.setEligible(
                    result.getEligible()
            );

            int score = result.getCibilScore();

            String riskCategory;

            if (score >= 900) {
                riskCategory = "Excellent";
            } else if (score >= 800) {
                riskCategory = "Very Good";
            } else if (score >= 750) {
                riskCategory = "Good";
            } else if (score >= 700) {
                riskCategory = "Average";
            } else if (score >= 650) {
                riskCategory = "Risky";
            } else {
                riskCategory = "Reject";
            }

            response.setRiskCategory(riskCategory);

            return response;
        });

        log.info("Eligible customer scorecards fetched successfully");

        return responsePage;
    }

    @Override
    public ScoreCardResponse getScoreCard(Long customerId) {

        log.info("Fetching scorecard for customer ID: {}", customerId);

        EligibilityResult result =
                eligibilityResultRepository.findById(customerId)
                        .orElseThrow(() -> {
                            log.warn("Eligibility result not found for ID: {}", customerId);
                            return new RuntimeException(
                                    "Eligibility result not found"
                            );
                        });

        if (!Boolean.TRUE.equals(result.getEligible())) {
            log.warn("Customer is not eligible for scorecard. Customer ID: {}", customerId);
            throw new RuntimeException(
                    "Customer is not eligible for score card"
            );
        }

        ScoreCardResponse response =
                new ScoreCardResponse();

        response.setCustomerId(
                result.getCustomer().getId()
        );

        response.setCustomerName(
                result.getCustomer().getFirstName()
                        + " "
                        + result.getCustomer().getLastName()
        );

        response.setPanCard(
                result.getCustomer().getPanCard()
        );

        response.setCibilScore(
                result.getCibilScore()
        );

        response.setEligibleLoanAmount(
                result.getEligibleLoanAmount()
        );

        response.setEligible(
                result.getEligible()
        );

        int score = result.getCibilScore();

        String riskCategory;

        if (score >= 900) {
            riskCategory = "Excellent";
        } else if (score >= 800) {
            riskCategory = "Very Good";
        } else if (score >= 750) {
            riskCategory = "Good";
        } else if (score >= 700) {
            riskCategory = "Average";
        } else if (score >= 650) {
            riskCategory = "Risky";
        } else {
            riskCategory = "Reject";
        }

        response.setRiskCategory(riskCategory);

        log.info("Scorecard fetched successfully for customer ID: {}", customerId);

        return response;
    }
}
