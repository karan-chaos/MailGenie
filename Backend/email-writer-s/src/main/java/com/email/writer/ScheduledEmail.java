package com.email.writer;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * Entity representing an email scheduled for future delivery.
 * Supports priority queuing, automatic retries with exponential backoff,
 * delivery status tracking, and recurrence patterns.
 */
@Entity
@Table(name = "scheduled_emails", indexes = {
        @Index(name = "idx_sched_status", columnList = "status"),
        @Index(name = "idx_sched_scheduled_for", columnList = "scheduledFor"),
        @Index(name = "idx_sched_priority", columnList = "priority DESC"),
        @Index(name = "idx_sched_recipient", columnList = "recipientEmail")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Recipient email address.
     */
    @Column(nullable = false, length = 255)
    private String recipientEmail;

    /**
     * Sender display name or email.
     */
    @Column(length = 255)
    private String senderName;

    /**
     * Email subject line.
     */
    @Column(nullable = false, length = 500)
    private String subject;

    /**
     * Email body content (HTML or plain text).
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    /**
     * Priority level: 1 (highest) to 5 (lowest). Default is 3.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 3;

    /**
     * Scheduled delivery time.
     */
    @Column(nullable = false)
    private LocalDateTime scheduledFor;

    /**
     * When the email was actually delivered (or last attempt).
     */
    private LocalDateTime deliveredAt;

    /**
     * Current status: PENDING, PROCESSING, DELIVERED, FAILED, CANCELLED, RETRYING.
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    /**
     * Number of delivery attempts made so far.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    /**
     * Maximum number of retry attempts allowed.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * Next scheduled retry time (for exponential backoff).
     */
    private LocalDateTime nextRetryAt;

    /**
     * Error message from the last failed attempt.
     */
    @Column(columnDefinition = "TEXT")
    private String lastErrorMessage;

    /**
     * LLM provider used to generate the email body (if AI-generated).
     */
    @Column(length = 30)
    private String provider;

    /**
     * LLM model used for generation.
     */
    @Column(length = 60)
    private String model;

    /**
     * Optional correlation/group ID to track batch-scheduled emails.
     */
    @Column(length = 100)
    private String batchId;

    /**
     * Whether this is a recurring email schedule.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean recurring = false;

    /**
     * Recurrence pattern: DAILY, WEEKLY, MONTHLY, or null for one-shot.
     */
    @Column(length = 20)
    private String recurrencePattern;

    /**
     * The original prompt/content used to generate this email (for audit).
     */
    @Column(columnDefinition = "TEXT")
    private String originalPrompt;

    /**
     * User-provided notes about this scheduled email.
     */
    @Column(columnDefinition = "TEXT")
    private String userNotes;

    /**
     * CC recipients (comma-separated).
     */
    @Column(length = 1000)
    private String ccRecipients;

    /**
     * BCC recipients (comma-separated).
     */
    @Column(length = 1000)
    private String bccRecipients;

    /**
     * Whether read receipt was requested.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean readReceiptRequested = false;

    /**
     * Custom headers as JSON string.
     */
    @Column(columnDefinition = "TEXT")
    private String customHeaders;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if this email is overdue (scheduled time has passed but not delivered).
     */
    @Transient
    public boolean isOverdue() {
        return "PENDING".equals(status) && scheduledFor != null && scheduledFor.isBefore(LocalDateTime.now());
    }

    /**
     * Check if this email is eligible for retry.
     */
    @Transient
    public boolean isRetryable() {
        return ("FAILED".equals(status) || "RETRYING".equals(status))
                && attemptCount < maxRetries
                && nextRetryAt != null
                && nextRetryAt.isBefore(LocalDateTime.now());
    }

    /**
     * Calculate the next retry time using exponential backoff.
     * Base delay: 1 minute, doubles each attempt.
     */
    @Transient
    public LocalDateTime calculateNextRetryTime() {
        long baseDelayMinutes = 1L;
        long delay = baseDelayMinutes * (1L << Math.min(attemptCount, 10));
        return LocalDateTime.now().plusMinutes(delay);
    }
}
