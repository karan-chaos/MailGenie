package com.email.writer;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * Entity class representing an advanced enterprise telemetry log for API calls.
 */
@Entity
@Table(name = "telemetry_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelemetryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String provider; // groq, openai, gemini

    @Column(nullable = false)
    private String endpoint; // /email/generate, /templates/recommend
    
    @Column(nullable = false)
    private Long durationMs;

    @Column(nullable = false)
    private String status; // SUCCESS, ERROR

    @Column
    private String errorCode; 
    
    @Column(nullable = false)
    private Integer inputLength; // context token size approximation

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
