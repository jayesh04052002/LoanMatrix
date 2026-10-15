
package com.example.LoanMatrix.dto.trackingStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmiSummaryDto {

    private Long totalEmis;
    private Long paidEmis;
    private BigDecimal paidAmount;
    private Long upcomingEmis;
    private BigDecimal upcomingAmount;
    private Long remainingEmis;
    private BigDecimal remainingAmount;
}
