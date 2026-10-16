
package com.example.LoanMatrix.dto.trackingStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoanProgressDto {

    private Long dealId;
    private String applicationId;
    private String overallStatus;
    private int currentStepIndex;
    private String bannerMessage;
    private List<ProgressStepDto> steps;
}
