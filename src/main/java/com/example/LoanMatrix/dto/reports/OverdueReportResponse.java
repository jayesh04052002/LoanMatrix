package com.example.LoanMatrix.dto.reports;

import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OverdueReportResponse {

    private long totalOverdueInstallments;

    private BigDecimal totalOverdueEmiAmount;

    private BigDecimal totalPenaltyAmount;

    private BigDecimal totalOverdueAmount;

    private long overdueLoanAccounts;
}
