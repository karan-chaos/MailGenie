package com.email.writer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enterprise Service aggregating system telemetry such as latency SLA and provider distribution.
 */
@Service
@RequiredArgsConstructor
public class TelemetryService {

    private final TelemetryLogRepository telemetryRepository;

    @Transactional
    public void logTelemetry(String provider, String endpoint, long durationMs, String status, String errorCode, int inputLength) {
        TelemetryLog log = TelemetryLog.builder()
                .provider(provider)
                .endpoint(endpoint)
                .durationMs(durationMs)
                .status(status)
                .errorCode(errorCode)
                .inputLength(inputLength)
                .timestamp(LocalDateTime.now())
                .build();
        telemetryRepository.save(log);
    }

    public Map<String, Object> getEnterpriseTelemetry() {
        List<TelemetryLog> logs = telemetryRepository.findAll();
        Map<String, Object> dashboard = new HashMap<>();

        long totalRequests = logs.size();
        dashboard.put("totalRequests", totalRequests);

        if (totalRequests == 0) {
            dashboard.put("averageLatencyMs", 0.0);
            dashboard.put("p95LatencyMs", 0.0);
            dashboard.put("successRate", 0.0);
            dashboard.put("dailyActivity", List.of());
            dashboard.put("providerDistribution", new HashMap<>());
            return dashboard;
        }

        // Latency
        double avgLatency = logs.stream().mapToDouble(TelemetryLog::getDurationMs).average().orElse(0.0);
        dashboard.put("averageLatencyMs", Math.round(avgLatency * 100.0) / 100.0);

        List<Long> sortedLatencies = logs.stream()
                .map(TelemetryLog::getDurationMs)
                .sorted()
                .collect(Collectors.toList());
        int p95Index = (int) Math.ceil(95.0 / 100.0 * sortedLatencies.size()) - 1;
        dashboard.put("p95LatencyMs", sortedLatencies.get(Math.max(0, p95Index)));

        // Success Rate
        long successCount = logs.stream().filter(l -> "SUCCESS".equalsIgnoreCase(l.getStatus())).count();
        dashboard.put("successRate", Math.round(((double) successCount / totalRequests) * 100.0));

        // Provider Distribution
        Map<String, Long> providers = logs.stream()
                .collect(Collectors.groupingBy(TelemetryLog::getProvider, Collectors.counting()));
        dashboard.put("providerDistribution", providers);

        // Daily Activity (last 7 days grouped)
        Map<String, Long> activity = logs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getTimestamp().toLocalDate().toString(),
                        Collectors.counting()
                ));
        dashboard.put("dailyActivity", activity);
        
        return dashboard;
    }
}
