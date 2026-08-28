package com.email.writer;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * Response DTO for delivery status of a scheduled email.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatusResponse {

    private Long id;
    private String recipientEmail;
    private String subject;
    private String status;
    private Integer priority;
    private String scheduledFor;
    private String deliveredAt;
    private Integer attemptCount;
    private Integer maxRetries;
    private String nextRetryAt;
    private String lastErrorMessage;
    private String provider;
    private String batchId;
    private Boolean recurring;
    private List<ScheduledEmailLog> recentAttempts;
}
