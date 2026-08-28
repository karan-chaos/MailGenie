package com.email.writer;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;

/**
 * Response DTO for system-wide scheduling statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleStats {

    private long totalScheduled;
    private long pendingCount;
    private long deliveredCount;
    private long failedCount;
    private long cancelledCount;
    private long retryingCount;
    private long overdueCount;
    private long totalDeliveryAttempts;
    private Double avgResponseTimeMs;
    private Double deliverySuccessRate;
    private Map<String, Long> statusBreakdown;
    private Map<String, Long> providerBreakdown;
    private Map<String, Long> priorityBreakdown;
    private Map<String, Long> hourlyDistribution;
    private long totalBatches;
}
