package com.example.LoanMatrix.service.scorecard;

import com.example.LoanMatrix.dto.scorecardDtos.ScoreCardResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ScoreCardService {

    Page<ScoreCardResponse> getEligibleCustomers(int page);

    ScoreCardResponse getScoreCard(Long customerId);

    void saveOrUpdateFromEligibility(
            com.example.LoanMatrix.entity.EligibilityResult result
    );
}
