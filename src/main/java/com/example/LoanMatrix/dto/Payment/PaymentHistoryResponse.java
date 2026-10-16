
package com.example.LoanMatrix.dto.Payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentHistoryResponse {

    private Long id;
    private BigDecimal paymentAmount;
    private LocalDate paymentDate;
    private String paymentStatus;
    private String paymentName;

}