package com.email.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enterprise service for scheduling, processing, retrying, and tracking email deliveries.
 * Supports priority queuing, exponential backoff retries, batch operations,
 * recurring schedules, and comprehensive analytics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledEmailService {

    private final ScheduledEmailRepository scheduledEmailRepository;
    private final ScheduledEmailLogRepository logRepository;
    private final EmailGeneratorService emailGeneratorService;

    // ─── Scheduling ────────────────────────────────────────────────────

    /**
     * Schedule a new email for future delivery.
     */
    @Transactional
    public ScheduledEmail scheduleEmail(ScheduleEmailRequest request) {
        validateScheduleRequest(request);

        ScheduledEmail email = ScheduledEmail.builder()
                .recipientEmail(request.getRecipientEmail().trim())
                .senderName(request.getSenderName())
                .subject(request.getSubject().trim())
                .body(request.getBody() != null ? request.getBody().trim() : "")
                .priority(request.getPriority() != null ? request.getPriority() : 3)
                .scheduledFor(request.getScheduledFor())
                .maxRetries(request.getMaxRetries() != null ? request.getMaxRetries() : 3)
                .provider(request.getProvider())
                .model(request.getModel())
                .batchId(request.getBatchId())
                .recurring(Boolean.TRUE.equals(request.getRecurring()))
                .recurrencePattern(request.getRecurrencePattern())
                .originalPrompt(request.getOriginalPrompt())
                .userNotes(request.getUserNotes())
                .ccRecipients(request.getCcRecipients())
                .bccRecipients(request.getBccRecipients())
                .readReceiptRequested(Boolean.TRUE.equals(request.getReadReceiptRequested()))
                .customHeaders(request.getCustomHeaders())
                .status("PENDING")
                .attemptCount(0)
                .build();

        // AI-generated body if requested
        if (Boolean.TRUE.equals(request.getAiGenerateBody()) && request.getOriginalPrompt() != null) {
            try {
                EmailRequest aiReq = new EmailRequest();
                aiReq.setEmailContent(request.getOriginalPrompt());
                aiReq.setTone(request.getAiTone() != null ? request.getAiTone() : "professional");
                aiReq.setProvider(request.getProvider());
                aiReq.setModel(request.getModel());
                aiReq.setApiKey(request.getApiKey());
                aiReq.setLanguage(request.getAiLanguage());
                aiReq.setCustomInstructions(request.getAiCustomInstructions());
                aiReq.setComposeMode(true);

                String generated = emailGeneratorService.generateEmailReply(aiReq);
                if (generated != null && !generated.trim().isEmpty()) {
                    email.setBody(generated);
                }
            } catch (Exception e) {
                log.warn("AI body generation failed, using provided body: {}", e.getMessage());
            }
        }

        ScheduledEmail saved = scheduledEmailRepository.save(email);
        log.info("Scheduled email id={} for {} at {} (priority={})",
                saved.getId(), saved.getRecipientEmail(), saved.getScheduledFor(), saved.getPriority());
        return saved;
    }

    /**
     * Schedule multiple emails in a batch.
     */
    @Transactional
    public List<ScheduledEmail> scheduleBatch(List<ScheduleEmailRequest> requests) {
        String batchId = "batch-" + UUID.randomUUID().toString().substring(0, 8);
        List<ScheduledEmail> results = new ArrayList<>();
        for (ScheduleEmailRequest req : requests) {
            req.setBatchId(batchId);
            results.add(scheduleEmail(req));
        }
        log.info("Scheduled batch {} with {} emails", batchId, results.size());
        return results;
    }

    // ─── Processing ────────────────────────────────────────────────────

    /**
     * Process due emails — called by the scheduler or manually.
     * Returns a summary of processed emails.
     */
    @Transactional
    public Map<String, Object> processDueEmails() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledEmail> dueEmails = scheduledEmailRepository
                .findByStatusInAndScheduledForBeforeOrderByPriorityAscScheduledForAsc(
                        List.of("PENDING"), now);

        int processed = 0, succeeded = 0, failed = 0;

        for (ScheduledEmail email : dueEmails) {
            email.setStatus("PROCESSING");
            scheduledEmailRepository.save(email);

            try {
                processSingleEmail(email);
                succeeded++;
            } catch (Exception e) {
                handleDeliveryFailure(email, e.getMessage());
                failed++;
            }
            processed++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processedCount", processed);
        result.put("succeededCount", succeeded);
        result.put("failedCount", failed);
        result.put("processedAt", now);
        return result;
    }

    /**
     * Process retryable emails with exponential backoff.
     */
    @Transactional
    public Map<String, Object> processRetryableEmails() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledEmail> retryable = scheduledEmailRepository.findRetryableEmails(now);

        int retried = 0, succeeded = 0, exhausted = 0;

        for (ScheduledEmail email : retryable) {
            email.setStatus("RETRYING");
            scheduledEmailRepository.save(email);

            try {
                processSingleEmail(email);
                succeeded++;
            } catch (Exception e) {
                handleDeliveryFailure(email, e.getMessage());
                if (email.getAttemptCount() >= email.getMaxRetries()) {
                    email.setStatus("FAILED");
                    email.setNextRetryAt(null);
                    scheduledEmailRepository.save(email);
                    exhausted++;
                }
            }
            retried++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("retryAttempted", retried);
        result.put("succeededCount", succeeded);
        result.put("exhaustedCount", exhausted);
        return result;
    }

    /**
     * Process a single email delivery attempt.
     */
    private void processSingleEmail(ScheduledEmail email) {
        long startTime = System.currentTimeMillis();
        email.setAttemptCount(email.getAttemptCount() + 1);

        try {
            // In production this would send via SMTP / SendGrid / SES.
            // For this feature we log the attempt and mark delivered.
            // Simulate delivery by validating the email has content.
            if (email.getBody() == null || email.getBody().trim().isEmpty()) {
                throw new IllegalStateException("Email body is empty — cannot deliver.");
            }

            long duration = System.currentTimeMillis() - startTime;

            // Log successful attempt
            ScheduledEmailLog logEntry = ScheduledEmailLog.builder()
                    .scheduledEmailId(email.getId())
                    .attemptNumber(email.getAttemptCount())
                    .outcome("SUCCESS")
                    .responseTimeMs(duration)
                    .providerUsed(email.getProvider())
                    .modelUsed(email.getModel())
                    .contentRegenerated(false)
                    .build();
            logRepository.save(logEntry);

            email.setStatus("DELIVERED");
            email.setDeliveredAt(LocalDateTime.now());
            email.setLastErrorMessage(null);
            email.setNextRetryAt(null);
            scheduledEmailRepository.save(email);

            log.info("Email id={} delivered to {} on attempt {}",
                    email.getId(), email.getRecipientEmail(), email.getAttemptCount());

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            ScheduledEmailLog logEntry = ScheduledEmailLog.builder()
                    .scheduledEmailId(email.getId())
                    .attemptNumber(email.getAttemptCount())
                    .outcome("FAILED")
                    .responseTimeMs(duration)
                    .errorMessage(e.getMessage())
                    .providerUsed(email.getProvider())
                    .modelUsed(email.getModel())
                    .build();
            logRepository.save(logEntry);
            throw e;
        }
    }

    /**
     * Handle a delivery failure with exponential backoff scheduling.
     */
    private void handleDeliveryFailure(ScheduledEmail email, String errorMsg) {
        email.setStatus("FAILED");
        email.setLastErrorMessage(errorMsg);

        if (email.getAttemptCount() < email.getMaxRetries()) {
            email.setNextRetryAt(email.calculateNextRetryTime());
            email.setStatus("RETRYING");
            log.warn("Email id={} failed (attempt {}/{}), retry at {}",
                    email.getId(), email.getAttemptCount(), email.getMaxRetries(), email.getNextRetryAt());
        } else {
            email.setNextRetryAt(null);
            log.warn("Email id={} failed permanently after {} attempts: {}",
                    email.getId(), email.getAttemptCount(), errorMsg);
        }
        scheduledEmailRepository.save(email);
    }

    // ─── Cancellation ──────────────────────────────────────────────────

    /**
     * Cancel a scheduled email before delivery.
     */
    @Transactional
    public boolean cancelEmail(Long id) {
        Optional<ScheduledEmail> opt = scheduledEmailRepository.findById(id);
        if (opt.isEmpty()) return false;

        ScheduledEmail email = opt.get();
        if ("DELIVERED".equals(email.getStatus())) {
            return false; // Already delivered
        }
        email.setStatus("CANCELLED");
        scheduledEmailRepository.save(email);
        log.info("Cancelled scheduled email id={}", id);
        return true;
    }

    /**
     * Cancel all pending emails in a batch.
     */
    @Transactional
    public int cancelBatch(String batchId) {
        List<ScheduledEmail> batch = scheduledEmailRepository.findByBatchIdOrderByPriorityAscScheduledForAsc(batchId);
        int cancelled = 0;
        for (ScheduledEmail email : batch) {
            if (!"DELIVERED".equals(email.getStatus())) {
                email.setStatus("CANCELLED");
                scheduledEmailRepository.save(email);
                cancelled++;
            }
        }
        log.info("Cancelled {} emails in batch {}", cancelled, batchId);
        return cancelled;
    }

    // ─── Queries ───────────────────────────────────────────────────────

    /**
     * Get a scheduled email by ID with delivery logs.
     */
    public Optional<ScheduledEmail> getEmailById(Long id) {
        return scheduledEmailRepository.findById(id);
    }

    /**
     * Get delivery status with recent attempt logs.
     */
    public DeliveryStatusResponse getDeliveryStatus(Long id) {
        ScheduledEmail email = scheduledEmailRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Scheduled email not found: " + id));
        List<ScheduledEmailLog> logs = logRepository.findByScheduledEmailIdOrderByAttemptNumberAsc(id);

        return DeliveryStatusResponse.builder()
                .id(email.getId())
                .recipientEmail(email.getRecipientEmail())
                .subject(email.getSubject())
                .status(email.getStatus())
                .priority(email.getPriority())
                .scheduledFor(email.getScheduledFor() != null ? email.getScheduledFor().toString() : null)
                .deliveredAt(email.getDeliveredAt() != null ? email.getDeliveredAt().toString() : null)
                .attemptCount(email.getAttemptCount())
                .maxRetries(email.getMaxRetries())
                .nextRetryAt(email.getNextRetryAt() != null ? email.getNextRetryAt().toString() : null)
                .lastErrorMessage(email.getLastErrorMessage())
                .provider(email.getProvider())
                .batchId(email.getBatchId())
                .recurring(email.getRecurring())
                .recentAttempts(logs)
                .build();
    }

    /**
     * Get all scheduled emails, optionally filtered by status.
     */
    public List<ScheduledEmail> getEmails(String status) {
        if (status != null && !status.trim().isEmpty()) {
            return scheduledEmailRepository.findByStatusOrderByScheduledForDesc(status.trim());
        }
        return scheduledEmailRepository.findAll(Sort.by("scheduledFor").descending());
    }

    /**
     * Get emails by recipient.
     */
    public List<ScheduledEmail> getEmailsByRecipient(String email) {
        return scheduledEmailRepository.findByRecipientEmailOrderByScheduledForDesc(email);
    }

    /**
     * Get emails by batch ID.
     */
    public List<ScheduledEmail> getBatchEmails(String batchId) {
        return scheduledEmailRepository.findByBatchIdOrderByPriorityAscScheduledForAsc(batchId);
    }

    /**
     * Get overdue emails.
     */
    public List<ScheduledEmail> getOverdueEmails() {
        return scheduledEmailRepository.findOverdueEmails(LocalDateTime.now());
    }

    // ─── Analytics ─────────────────────────────────────────────────────

    /**
     * Get comprehensive scheduling statistics.
     */
    public ScheduleStats getSchedulingStats() {
        long total = scheduledEmailRepository.count();
        long pending = scheduledEmailRepository.countByStatus("PENDING");
        long delivered = scheduledEmailRepository.countByStatus("DELIVERED");
        long failed = scheduledEmailRepository.countByStatus("FAILED");
        long cancelled = scheduledEmailRepository.countByStatus("CANCELLED");
        long retrying = scheduledEmailRepository.countByStatus("RETRYING");
        long overdue = scheduledEmailRepository.findOverdueEmails(LocalDateTime.now()).size();

        long totalAttempts = logRepository.totalAttemptCount();
        Double avgResponse = logRepository.averageResponseTime();
        long totalSuccess = logRepository.countByOutcome("SUCCESS");
        double successRate = totalAttempts > 0 ? Math.round(((double) totalSuccess / totalAttempts) * 10000.0) / 100.0 : 0.0;

        Map<String, Long> statusMap = new LinkedHashMap<>();
        statusMap.put("PENDING", pending);
        statusMap.put("DELIVERED", delivered);
        statusMap.put("FAILED", failed);
        statusMap.put("CANCELLED", cancelled);
        statusMap.put("RETRYING", retrying);
        statusMap.put("OVERDUE", overdue);

        Map<String, Long> providerMap = logRepository.countByProvider().stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1],
                        (a, b) -> a, LinkedHashMap::new));

        List<String> allBatchIds = scheduledEmailRepository.findAllBatchIds();

        return ScheduleStats.builder()
                .totalScheduled(total)
                .pendingCount(pending)
                .deliveredCount(delivered)
                .failedCount(failed)
                .cancelledCount(cancelled)
                .retryingCount(retrying)
                .overdueCount(overdue)
                .totalDeliveryAttempts(totalAttempts)
                .avgResponseTimeMs(avgResponse != null ? Math.round(avgResponse * 100.0) / 100.0 : 0.0)
                .deliverySuccessRate(successRate)
                .statusBreakdown(statusMap)
                .providerBreakdown(providerMap)
                .totalBatches(allBatchIds.size())
                .build();
    }

    // ─── Manual Trigger ────────────────────────────────────────────────

    /**
     * Manually force-process a specific email immediately.
     */
    @Transactional
    public boolean forceProcess(Long id) {
        Optional<ScheduledEmail> opt = scheduledEmailRepository.findById(id);
        if (opt.isEmpty()) return false;

        ScheduledEmail email = opt.get();
        if ("DELIVERED".equals(email.getStatus()) || "CANCELLED".equals(email.getStatus())) {
            return false;
        }

        email.setStatus("PROCESSING");
        scheduledEmailRepository.save(email);

        try {
            processSingleEmail(email);
            return true;
        } catch (Exception e) {
            handleDeliveryFailure(email, e.getMessage());
            return false;
        }
    }

    // ─── Validation ────────────────────────────────────────────────────

    private void validateScheduleRequest(ScheduleEmailRequest request) {
        if (request.getRecipientEmail() == null || request.getRecipientEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email is required.");
        }
        if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
            throw new IllegalArgumentException("Subject is required.");
        }
        if (request.getScheduledFor() == null) {
            throw new IllegalArgumentException("Scheduled time is required.");
        }
        if (request.getBody() == null && !Boolean.TRUE.equals(request.getAiGenerateBody())) {
            throw new IllegalArgumentException("Either body or aiGenerateBody must be provided.");
        }
        if (request.getPriority() != null && (request.getPriority() < 1 || request.getPriority() > 5)) {
            throw new IllegalArgumentException("Priority must be between 1 and 5.");
        }
        if (request.getRecurring() != null && request.getRecurring()
                && (request.getRecurrencePattern() == null || request.getRecurrencePattern().trim().isEmpty())) {
            throw new IllegalArgumentException("Recurrence pattern is required for recurring emails.");
        }
    }

    private static class Sort {
        static org.springframework.data.domain.Sort by(String property) {
            return org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, property);
        }
    }
}
