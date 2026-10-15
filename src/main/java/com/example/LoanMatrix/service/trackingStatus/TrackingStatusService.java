
package com.example.LoanMatrix.service.trackingStatus;

import com.example.LoanMatrix.dto.trackingStatus.TrackingStatusResponse;

public interface TrackingStatusService {

    TrackingStatusResponse getTrackingStatus(Long customerId);
}
