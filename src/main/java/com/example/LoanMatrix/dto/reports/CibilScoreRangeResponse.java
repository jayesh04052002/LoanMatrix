package com.example.LoanMatrix.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CibilScoreRangeResponse {

    private String scoreRange;
    private String category;
    private long customerCount;
}
