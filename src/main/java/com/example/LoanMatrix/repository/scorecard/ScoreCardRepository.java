
package com.example.LoanMatrix.repository.scorecard;

import com.example.LoanMatrix.entity.ScoreCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScoreCardRepository extends JpaRepository<ScoreCard, Long> {

    Optional<ScoreCard> findByCustomer_Id(Long customerId);
}
