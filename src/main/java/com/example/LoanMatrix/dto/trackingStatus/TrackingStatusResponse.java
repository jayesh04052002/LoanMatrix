
package com.example.LoanMatrix.dto.trackingStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrackingStatusResponse {

    private CustomerProfileDto profile;
    private Long totalApplications;
    private List<LoanProgressDto> applicationProgress;
    private EmiSummaryDto emiSummary;
    private List<ActivityTimelineDto> recentActivities;
}
