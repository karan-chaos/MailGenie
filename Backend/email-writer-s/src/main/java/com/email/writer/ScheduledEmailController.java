package com.email.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the Email Scheduling & Delivery Queue system.
 * Provides endpoints for scheduling, processing, cancelling, querying,
 * and analytics on scheduled emails.
 */
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "*"})
@Slf4j
public class ScheduledEmailController {

    private final ScheduledEmailService scheduledEmailService;

    // ─── Scheduling ────────────────────────────────────────────────────

    /**
     * Schedule a single email for future delivery.
     */
    @PostMapping
    public ResponseEntity<ScheduledEmail> scheduleEmail(@RequestBody ScheduleEmailRequest request) {
        ScheduledEmail scheduled = scheduledEmailService.scheduleEmail(request);
        return ResponseEntity.ok(scheduled);
    }

    /**
     * Schedule a batch of emails with a shared batch ID.
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> scheduleBatch(@RequestBody List<ScheduleEmailRequest> requests) {
        List<ScheduledEmail> scheduled = scheduledEmailService.scheduleBatch(requests);
        String batchId = scheduled.isEmpty() ? null : scheduled.get(0).getBatchId();
        return ResponseEntity.ok(Map.of(
                "batchId", batchId != null ? batchId : "",
                "scheduledCount", scheduled.size(),
                "emails", scheduled
        ));
    }

    // ─── Processing ────────────────────────────────────────────────────

    /**
     * Manually trigger processing of all due emails.
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processDueEmails() {
        return ResponseEntity.ok(scheduledEmailService.processDueEmails());
    }

    /**
     * Manually trigger processing of retryable emails.
     */
    @PostMapping("/process/retries")
    public ResponseEntity<Map<String, Object>> processRetries() {
        return ResponseEntity.ok(scheduledEmailService.processRetryableEmails());
    }

    /**
     * Force-process a specific email immediately regardless of schedule time.
     */
    @PostMapping("/{id}/force")
    public ResponseEntity<Map<String, Boolean>> forceProcess(@PathVariable Long id) {
        boolean result = scheduledEmailService.forceProcess(id);
        return ResponseEntity.ok(Map.of("processed", result));
    }

    // ─── Cancellation ──────────────────────────────────────────────────

    /**
     * Cancel a scheduled email before delivery.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Boolean>> cancelEmail(@PathVariable Long id) {
        boolean result = scheduledEmailService.cancelEmail(id);
        return ResponseEntity.ok(Map.of("cancelled", result));
    }

    /**
     * Cancel all pending emails in a batch.
     */
    @PostMapping("/batch/{batchId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBatch(@PathVariable String batchId) {
        int cancelled = scheduledEmailService.cancelBatch(batchId);
        return ResponseEntity.ok(Map.of(
                "batchId", batchId,
                "cancelledCount", cancelled
        ));
    }

    // ─── Queries ───────────────────────────────────────────────────────

    /**
     * Get a specific scheduled email by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScheduledEmail> getEmail(@PathVariable Long id) {
        return scheduledEmailService.getEmailById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get delivery status with full attempt history for a scheduled email.
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<DeliveryStatusResponse> getDeliveryStatus(@PathVariable Long id) {
        return ResponseEntity.ok(scheduledEmailService.getDeliveryStatus(id));
    }

    /**
     * List all scheduled emails, optionally filtered by status.
     */
    @GetMapping
    public ResponseEntity<List<ScheduledEmail>> listEmails(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(scheduledEmailService.getEmails(status));
    }

    /**
     * List emails for a specific recipient.
     */
    @GetMapping("/recipient/{email}")
    public ResponseEntity<List<ScheduledEmail>> listByRecipient(@PathVariable String email) {
        return ResponseEntity.ok(scheduledEmailService.getEmailsByRecipient(email));
    }

    /**
     * List emails in a specific batch.
     */
    @GetMapping("/batch/{batchId}")
    public ResponseEntity<List<ScheduledEmail>> listBatch(@PathVariable String batchId) {
        return ResponseEntity.ok(scheduledEmailService.getBatchEmails(batchId));
    }

    /**
     * List all overdue (pending but past scheduled time) emails.
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<ScheduledEmail>> listOverdue() {
        return ResponseEntity.ok(scheduledEmailService.getOverdueEmails());
    }

    // ─── Analytics ─────────────────────────────────────────────────────

    /**
     * Get comprehensive scheduling statistics and analytics.
     */
    @GetMapping("/stats")
    public ResponseEntity<ScheduleStats> getStats() {
        return ResponseEntity.ok(scheduledEmailService.getSchedulingStats());
    }
}
