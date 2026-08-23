package com.email.writer;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/telemetry")
@CrossOrigin(origins = "*") // Allows local React app to fetch telemetry data
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryService telemetryService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getEnterpriseStats() {
        Map<String, Object> stats = telemetryService.getEnterpriseTelemetry();
        return ResponseEntity.ok(stats);
    }
}
