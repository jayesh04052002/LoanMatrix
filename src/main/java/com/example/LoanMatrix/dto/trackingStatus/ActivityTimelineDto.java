
package com.example.LoanMatrix.dto.trackingStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityTimelineDto {

    private String title;
    private String description;
    private LocalDateTime timestamp;
}
