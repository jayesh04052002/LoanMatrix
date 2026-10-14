
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
public class ProductWiseResponse {

    private String loanType;
    private long totalLoans;
    private BigDecimal totalLoanAmount;
}
