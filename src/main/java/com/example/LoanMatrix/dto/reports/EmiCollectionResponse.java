
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
public class EmiCollectionResponse {

    private long totalInstallments;

    private BigDecimal totalScheduledAmount;

    private BigDecimal totalCollectedAmount;

    private long paidInstallments;

    private long pendingInstallments;

    private BigDecimal pendingAmount;
}
