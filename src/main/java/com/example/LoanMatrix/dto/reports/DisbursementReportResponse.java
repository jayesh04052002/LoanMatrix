package com.example.LoanMatrix.dto.reports;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisbursementReportResponse {

    private long totalDisbursedLoans;

    private BigDecimal totalDisbursedAmount;

    private BigDecimal averageDisbursedAmount;

    private LocalDate latestDisbursementDate;
}