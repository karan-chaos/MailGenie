package com.email.writer;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * Entity tracking each individual delivery attempt for a scheduled email.
 * Provides a full audit trail of attempts, retries, and outcomes.
 */
@Entity
@Table(name = "scheduled_email_logs", indexes = {
        @Index(name = "idx_sel_scheduled_id", columnList = "scheduledEmailId"),
        @Index(name = "idx_sel_attempted_at", columnList = "attemptedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledEmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Foreign key to the parent ScheduledEmail.
     */
    @Column(nullable = false)
    private Long scheduledEmailId;

    /**
     * Sequential attempt number (1, 2, 3, ...).
     */
    @Column(nullable = false)
    private Integer attemptNumber;

    /**
     * Outcome: SUCCESS, FAILED, TIMEOUT, RATE_LIMITED, PROVIDER_ERROR.
     */
    @Column(nullable = false, length = 20)
    private String outcome;

    /**
     * HTTP status code from the provider (if applicable).
     */
    private Integer httpStatusCode;

    /**
     * Provider response time in milliseconds.
     */
    private Long responseTimeMs;

    /**
     * Error message if the attempt failed.
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Error code or category for classification.
     */
    @Column(length = 50)
    private String errorCode;

    /**
     * Whether the email content was regenerated before this attempt.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean contentRegenerated = false;

    /**
     * The provider used for this specific attempt.
     */
    @Column(length = 30)
    private String providerUsed;

    /**
     * The model used for this specific attempt.
     */
    @Column(length = 60)
    private String modelUsed;

    /**
     * Timestamp when this attempt was made.
     */
    @Column(nullable = false)
    private LocalDateTime attemptedAt;

    @PrePersist
    protected void onCreate() {
        attemptedAt = LocalDateTime.now();
    }
}
