
        package com.example.LoanMatrix.dto.reports;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanPortfolioResponse {

    private long totalLoanAccounts;

    private BigDecimal totalLoanAmount;

    private BigDecimal totalOutstandingPrincipal;

    private long activeLoans;

    private long closedLoans;

    private long otherStatusLoans;
}

