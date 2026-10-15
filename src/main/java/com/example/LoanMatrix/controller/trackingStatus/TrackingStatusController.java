
package com.example.LoanMatrix.controller.trackingStatus;

import com.example.LoanMatrix.dto.trackingStatus.TrackingStatusResponse;
import com.example.LoanMatrix.service.trackingStatus.TrackingStatusService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class TrackingStatusController {

    private final TrackingStatusService trackingStatusService;

    public TrackingStatusController(
            TrackingStatusService trackingStatusService) {
        this.trackingStatusService = trackingStatusService;
    }

    @GetMapping("/tracking-status")
    public ResponseEntity<TrackingStatusResponse> getTrackingStatus(
            @RequestParam Long customerId) {

        TrackingStatusResponse response =
                trackingStatusService.getTrackingStatus(customerId);

        return ResponseEntity.ok(response);
    }
}
