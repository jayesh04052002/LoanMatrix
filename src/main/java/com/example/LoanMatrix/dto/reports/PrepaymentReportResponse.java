package com.example.LoanMatrix.dto.reports;

import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrepaymentReportResponse {

    private long totalPrepaidLoans;

    private BigDecimal totalPrepaidAmount;

    private BigDecimal averagePrepaidAmount;
}
