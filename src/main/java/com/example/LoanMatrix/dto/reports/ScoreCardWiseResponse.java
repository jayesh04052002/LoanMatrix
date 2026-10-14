package com.example.LoanMatrix.dto.reports;


import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScoreCardWiseResponse {

    private Long scoreCardId;
    private Long customerId;
    private String customerName;
    private LocalDate appliedDate;
    private Integer cibilScore;
    private String riskCategory;
    private BigDecimal eligibleLoanAmount;
    private String currentStatus;
    private String rejectionReason;
}
