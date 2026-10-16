package com.example.LoanMatrix.service.scorecard;

import com.example.LoanMatrix.dto.scorecardDtos.ScoreCardResponse;
import com.example.LoanMatrix.entity.EligibilityResult;
import com.example.LoanMatrix.repository.eligibility.EligibilityResultRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.example.LoanMatrix.entity.ScoreCard;
import com.example.LoanMatrix.repository.scorecard.ScoreCardRepository;
import java.time.LocalDate;

@Service
@Slf4j
public class ScoreCardServiceImpl implements ScoreCardService {

    private final EligibilityResultRepository eligibilityResultRepository;
    private final ScoreCardRepository scoreCardRepository;

    public ScoreCardServiceImpl(
            EligibilityResultRepository eligibilityResultRepository, ScoreCardRepository scoreCardRepository) {
        this.eligibilityResultRepository = eligibilityResultRepository;
        this.scoreCardRepository = scoreCardRepository;
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

        log.info("Fetching scorecard for Customer ID: {}", customerId);

        ScoreCard scoreCard = scoreCardRepository
                .findByCustomer_Id(customerId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Scorecard not found for Customer ID: " + customerId
                        )
                );

        ScoreCardResponse response = new ScoreCardResponse();

        response.setCustomerId(scoreCard.getCustomer().getId());

        response.setCustomerName(
                scoreCard.getCustomer().getFirstName()
                        + " "
                        + scoreCard.getCustomer().getLastName()
        );

        response.setPanCard(scoreCard.getCustomer().getPanCard());
        response.setCibilScore(scoreCard.getCibilScore());
        response.setEligibleLoanAmount(scoreCard.getEligibleLoanAmount());

        response.setEligible(
                "APPROVED".equalsIgnoreCase(scoreCard.getCurrentStatus())
        );

        response.setRiskCategory(scoreCard.getRiskCategory());

        return response;
    }


    @Override
    public void saveOrUpdateFromEligibility(EligibilityResult result) {

        Long customerId = result.getCustomer().getId();

        ScoreCard scoreCard = scoreCardRepository
                .findByCustomer_Id(customerId)
                .orElseGet(ScoreCard::new);

        scoreCard.setCustomer(result.getCustomer());
        scoreCard.setCibilScore(result.getCibilScore());
        scoreCard.setEligibleLoanAmount(result.getEligibleLoanAmount());
        scoreCard.setRejectionReason(result.getRejectionReason());

        // Determine the customer's current status.
        if (result.getRejectionReason() != null) {
            scoreCard.setCurrentStatus("REJECTED");
        } else if (Boolean.TRUE.equals(result.getEligible())) {
            scoreCard.setCurrentStatus("APPROVED");
        } else {
            scoreCard.setCurrentStatus("PENDING");
        }

        // Preserve the original application date when updating.
        if (scoreCard.getAppliedDate() == null) {
            scoreCard.setAppliedDate(LocalDate.now());
        }

        // Determine risk category from the CIBIL score.
        Integer score = result.getCibilScore();
        String riskCategory;

        if (score == null) {
            riskCategory = "UNKNOWN";
        } else if (score >= 900) {
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

        scoreCard.setRiskCategory(riskCategory);

        scoreCardRepository.save(scoreCard);

        log.info("Scorecard saved for Customer ID: {}", customerId);
    }

}
