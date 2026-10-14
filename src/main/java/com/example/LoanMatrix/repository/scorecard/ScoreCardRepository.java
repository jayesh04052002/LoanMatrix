package com.example.LoanMatrix.repository.scorecard;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.LoanMatrix.entity.ScoreCard;

@Repository
public interface ScoreCardRepository
        extends JpaRepository<ScoreCard, Long> {

}