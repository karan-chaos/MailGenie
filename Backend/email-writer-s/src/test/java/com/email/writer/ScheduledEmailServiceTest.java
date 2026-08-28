package com.email.writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledEmailServiceTest {

    @Mock
    private ScheduledEmailRepository scheduledEmailRepository;

    @Mock
    private ScheduledEmailLogRepository logRepository;

    @Mock
    private EmailGeneratorService emailGeneratorService;

    @InjectMocks
    private ScheduledEmailService scheduledEmailService;

    private ScheduledEmail sampleEmail;
    private ScheduleEmailRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleEmail = ScheduledEmail.builder()
                .id(1L)
                .recipientEmail("alice@example.com")
                .senderName("Bob")
                .subject("Project Update")
                .body("Hi Alice,\n\nHere is the latest update.")
                .priority(3)
                .scheduledFor(LocalDateTime.now().plusHours(1))
                .status("PENDING")
                .attemptCount(0)
                .maxRetries(3)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleRequest = new ScheduleEmailRequest();
        sampleRequest.setRecipientEmail("alice@example.com");
        sampleRequest.setSenderName("Bob");
        sampleRequest.setSubject("Project Update");
        sampleRequest.setBody("Hi Alice,\n\nHere is the latest update.");
        sampleRequest.setPriority(3);
        sampleRequest.setScheduledFor(LocalDateTime.now().plusHours(1));
    }

    // ─── Scheduling Tests ──────────────────────────────────────────────

    @Test
    void testScheduleEmail_success() {
        when(scheduledEmailRepository.save(any(ScheduledEmail.class))).thenAnswer(inv -> {
            ScheduledEmail e = inv.getArgument(0);
            e.setId(10L);
            return e;
        });

        ScheduledEmail result = scheduledEmailService.scheduleEmail(sampleRequest);

        assertNotNull(result);
        assertEquals("alice@example.com", result.getRecipientEmail());
        assertEquals("PENDING", result.getStatus());
        assertEquals(Integer.valueOf(0), result.getAttemptCount());
        verify(scheduledEmailRepository).save(any(ScheduledEmail.class));
    }

    @Test
    void testScheduleEmail_setsDefaults() {
        when(scheduledEmailRepository.save(any(ScheduledEmail.class))).thenAnswer(inv -> inv.getArgument(0));

        ScheduleEmailRequest req = new ScheduleEmailRequest();
        req.setRecipientEmail("test@example.com");
        req.setSubject("Test");
        req.setBody("Body");
        req.setScheduledFor(LocalDateTime.now().plusHours(1));

        ScheduledEmail result = scheduledEmailService.scheduleEmail(req);

        assertEquals(Integer.valueOf(3), result.getPriority());
        assertEquals(Integer.valueOf(3), result.getMaxRetries());
    }

    @Test
    void testScheduleEmail_throwsOnMissingRecipient() {
        ScheduleEmailRequest req = new ScheduleEmailRequest();
        req.setSubject("Test");
        req.setBody("Body");
        req.setScheduledFor(LocalDateTime.now());

        assertThrows(IllegalArgumentException.class, () -> scheduledEmailService.scheduleEmail(req));
    }

    @Test
    void testScheduleEmail_throwsOnMissingSubject() {
        ScheduleEmailRequest req = new ScheduleEmailRequest();
        req.setRecipientEmail("test@example.com");
        req.setBody("Body");
        req.setScheduledFor(LocalDateTime.now());

        assertThrows(IllegalArgumentException.class, () -> scheduledEmailService.scheduleEmail(req));
    }

    @Test
    void testScheduleEmail_throwsOnMissingScheduleTime() {
        ScheduleEmailRequest req = new ScheduleEmailRequest();
        req.setRecipientEmail("test@example.com");
        req.setSubject("Test");
        req.setBody("Body");

        assertThrows(IllegalArgumentException.class, () -> scheduledEmailService.scheduleEmail(req));
    }

    @Test
    void testScheduleEmail_throwsOnInvalidPriority() {
        ScheduleEmailRequest req = new ScheduleEmailRequest();
        req.setRecipientEmail("test@example.com");
        req.setSubject("Test");
        req.setBody("Body");
        req.setScheduledFor(LocalDateTime.now());
        req.setPriority(6);

        assertThrows(IllegalArgumentException.class, () -> scheduledEmailService.scheduleEmail(req));
    }

    @Test
    void testScheduleEmail_throwsOnRecurringWithoutPattern() {
        ScheduleEmailRequest req = new ScheduleEmailRequest();
        req.setRecipientEmail("test@example.com");
        req.setSubject("Test");
        req.setBody("Body");
        req.setScheduledFor(LocalDateTime.now());
        req.setRecurring(true);

        assertThrows(IllegalArgumentException.class, () -> scheduledEmailService.scheduleEmail(req));
    }

    @Test
    void testScheduleEmail_aiGeneration() {
        when(scheduledEmailRepository.save(any(ScheduledEmail.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emailGeneratorService.generateEmailReply(any(EmailRequest.class))).thenReturn("AI generated email body");

        ScheduleEmailRequest req = new ScheduleEmailRequest();
        req.setRecipientEmail("test@example.com");
        req.setSubject("Test");
        req.setScheduledFor(LocalDateTime.now().plusHours(1));
        req.setAiGenerateBody(true);
        req.setOriginalPrompt("Write a meeting follow-up");
        req.setProvider("groq");

        ScheduledEmail result = scheduledEmailService.scheduleEmail(req);

        assertEquals("AI generated email body", result.getBody());
        verify(emailGeneratorService).generateEmailReply(any(EmailRequest.class));
    }

    @Test
    void testScheduleEmail_aiGenerationFallback() {
        when(scheduledEmailRepository.save(any(ScheduledEmail.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emailGeneratorService.generateEmailReply(any(EmailRequest.class)))
                .thenThrow(new RuntimeException("API error"));

        ScheduleEmailRequest req = new ScheduleEmailRequest();
        req.setRecipientEmail("test@example.com");
        req.setSubject("Test");
        req.setBody("Fallback body");
        req.setScheduledFor(LocalDateTime.now().plusHours(1));
        req.setAiGenerateBody(true);
        req.setOriginalPrompt("Write something");

        ScheduledEmail result = scheduledEmailService.scheduleEmail(req);

        assertEquals("Fallback body", result.getBody());
    }

    // ─── Batch Scheduling ──────────────────────────────────────────────

    @Test
    void testScheduleBatch() {
        when(scheduledEmailRepository.save(any(ScheduledEmail.class))).thenAnswer(inv -> {
            ScheduledEmail e = inv.getArgument(0);
            e.setId(10L);
            return e;
        });

        ScheduleEmailRequest req1 = new ScheduleEmailRequest();
        req1.setRecipientEmail("a@example.com");
        req1.setSubject("S1");
        req1.setBody("Body1");
        req1.setScheduledFor(LocalDateTime.now().plusHours(1));

        ScheduleEmailRequest req2 = new ScheduleEmailRequest();
        req2.setRecipientEmail("b@example.com");
        req2.setSubject("S2");
        req2.setBody("Body2");
        req2.setScheduledFor(LocalDateTime.now().plusHours(2));

        List<ScheduledEmail> result = scheduledEmailService.scheduleBatch(List.of(req1, req2));

        assertEquals(2, result.size());
        // Both should share the same batch ID
        assertEquals(result.get(0).getBatchId(), result.get(1).getBatchId());
    }

    // ─── Processing Tests ──────────────────────────────────────────────

    @Test
    void testProcessDueEmails_nothingDue() {
        when(scheduledEmailRepository.findByStatusInAndScheduledForBeforeOrderByPriorityAscScheduledForAsc(
                anyList(), any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        Map<String, Object> result = scheduledEmailService.processDueEmails();

        assertEquals(0, result.get("processedCount"));
        assertEquals(0, result.get("succeededCount"));
    }

    @Test
    void testForceProcess_success() {
        when(scheduledEmailRepository.findById(1L)).thenReturn(Optional.of(sampleEmail));
        when(scheduledEmailRepository.save(any(ScheduledEmail.class))).thenAnswer(inv -> inv.getArgument(0));
        when(logRepository.save(any(ScheduledEmailLog.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean result = scheduledEmailService.forceProcess(1L);

        assertTrue(result);
        assertEquals("DELIVERED", sampleEmail.getStatus());
        assertNotNull(sampleEmail.getDeliveredAt());
    }

    @Test
    void testForceProcess_notFound() {
        when(scheduledEmailRepository.findById(99L)).thenReturn(Optional.empty());

        boolean result = scheduledEmailService.forceProcess(99L);

        assertFalse(result);
    }

    @Test
    void testForceProcess_alreadyDelivered() {
        sampleEmail.setStatus("DELIVERED");
        when(scheduledEmailRepository.findById(1L)).thenReturn(Optional.of(sampleEmail));

        boolean result = scheduledEmailService.forceProcess(1L);

        assertFalse(result);
    }

    // ─── Cancellation Tests ────────────────────────────────────────────

    @Test
    void testCancelEmail_success() {
        when(scheduledEmailRepository.findById(1L)).thenReturn(Optional.of(sampleEmail));
        when(scheduledEmailRepository.save(any(ScheduledEmail.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean result = scheduledEmailService.cancelEmail(1L);

        assertTrue(result);
        assertEquals("CANCELLED", sampleEmail.getStatus());
    }

    @Test
    void testCancelEmail_alreadyDelivered() {
        sampleEmail.setStatus("DELIVERED");
        when(scheduledEmailRepository.findById(1L)).thenReturn(Optional.of(sampleEmail));

        boolean result = scheduledEmailService.cancelEmail(1L);

        assertFalse(result);
    }

    @Test
    void testCancelEmail_notFound() {
        when(scheduledEmailRepository.findById(99L)).thenReturn(Optional.empty());

        boolean result = scheduledEmailService.cancelEmail(99L);

        assertFalse(result);
    }

    @Test
    void testCancelBatch() {
        ScheduledEmail e1 = ScheduledEmail.builder().id(1L).status("PENDING").batchId("b1").build();
        ScheduledEmail e2 = ScheduledEmail.builder().id(2L).status("PENDING").batchId("b1").build();
        ScheduledEmail e3 = ScheduledEmail.builder().id(3L).status("DELIVERED").batchId("b1").build();

        when(scheduledEmailRepository.findByBatchIdOrderByPriorityAscScheduledForAsc("b1"))
                .thenReturn(List.of(e1, e2, e3));
        when(scheduledEmailRepository.save(any(ScheduledEmail.class))).thenAnswer(inv -> inv.getArgument(0));

        int cancelled = scheduledEmailService.cancelBatch("b1");

        assertEquals(2, cancelled); // e3 already delivered
    }

    // ─── Query Tests ───────────────────────────────────────────────────

    @Test
    void testGetEmailById_found() {
        when(scheduledEmailRepository.findById(1L)).thenReturn(Optional.of(sampleEmail));

        Optional<ScheduledEmail> result = scheduledEmailService.getEmailById(1L);

        assertTrue(result.isPresent());
        assertEquals("Project Update", result.get().getSubject());
    }

    @Test
    void testGetEmailById_notFound() {
        when(scheduledEmailRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<ScheduledEmail> result = scheduledEmailService.getEmailById(99L);

        assertFalse(result.isPresent());
    }

    @Test
    void testGetEmails_withStatus() {
        when(scheduledEmailRepository.findByStatusOrderByScheduledForDesc("PENDING"))
                .thenReturn(List.of(sampleEmail));

        List<ScheduledEmail> result = scheduledEmailService.getEmails("PENDING");

        assertEquals(1, result.size());
    }

    @Test
    void testGetEmails_noFilter() {
        when(scheduledEmailRepository.findAll(any())).thenReturn(List.of(sampleEmail));

        List<ScheduledEmail> result = scheduledEmailService.getEmails(null);

        assertEquals(1, result.size());
    }

    // ─── Entity Method Tests ───────────────────────────────────────────

    @Test
    void testScheduledEmail_isOverdue() {
        sampleEmail.setScheduledFor(LocalDateTime.now().minusHours(1));
        sampleEmail.setStatus("PENDING");

        assertTrue(sampleEmail.isOverdue());
    }

    @Test
    void testScheduledEmail_isNotOverdue_whenDelivered() {
        sampleEmail.setScheduledFor(LocalDateTime.now().minusHours(1));
        sampleEmail.setStatus("DELIVERED");

        assertFalse(sampleEmail.isOverdue());
    }

    @Test
    void testScheduledEmail_isRetryable() {
        sampleEmail.setStatus("FAILED");
        sampleEmail.setAttemptCount(1);
        sampleEmail.setMaxRetries(3);
        sampleEmail.setNextRetryAt(LocalDateTime.now().minusMinutes(1));

        assertTrue(sampleEmail.isRetryable());
    }

    @Test
    void testScheduledEmail_isNotRetryable_whenMaxRetriesReached() {
        sampleEmail.setStatus("FAILED");
        sampleEmail.setAttemptCount(3);
        sampleEmail.setMaxRetries(3);
        sampleEmail.setNextRetryAt(LocalDateTime.now().minusMinutes(1));

        assertFalse(sampleEmail.isRetryable());
    }

    @Test
    void testScheduledEmail_isNotRetryable_whenNotInFailedState() {
        sampleEmail.setStatus("PENDING");
        sampleEmail.setAttemptCount(1);
        sampleEmail.setMaxRetries(3);
        sampleEmail.setNextRetryAt(LocalDateTime.now().minusMinutes(1));

        assertFalse(sampleEmail.isRetryable());
    }

    @Test
    void testScheduledEmail_calculateNextRetryTime() {
        sampleEmail.setAttemptCount(2);

        LocalDateTime nextRetry = sampleEmail.calculateNextRetryTime();

        assertNotNull(nextRetry);
        // Attempt 2: delay = 1 * 2^2 = 4 minutes
        assertTrue(nextRetry.isAfter(LocalDateTime.now().plusMinutes(3)));
        assertTrue(nextRetry.isBefore(LocalDateTime.now().plusMinutes(5)));
    }

    @Test
    void testScheduledEmail_calculateNextRetryTime_firstAttempt() {
        sampleEmail.setAttemptCount(0);

        LocalDateTime nextRetry = sampleEmail.calculateNextRetryTime();

        // Attempt 0: delay = 1 * 2^0 = 1 minute
        assertTrue(nextRetry.isAfter(LocalDateTime.now()));
        assertTrue(nextRetry.isBefore(LocalDateTime.now().plusMinutes(2)));
    }

    @Test
    void testDeliveryStatusResponse_build() {
        DeliveryStatusResponse response = DeliveryStatusResponse.builder()
                .id(1L)
                .recipientEmail("alice@example.com")
                .subject("Test")
                .status("PENDING")
                .build();

        assertEquals(1L, response.getId());
        assertEquals("alice@example.com", response.getRecipientEmail());
    }

    @Test
    void testScheduleStats_build() {
        Map<String, Long> statusMap = Map.of("PENDING", 5L, "DELIVERED", 10L);
        ScheduleStats stats = ScheduleStats.builder()
                .totalScheduled(15)
                .pendingCount(5)
                .deliveredCount(10)
                .statusBreakdown(statusMap)
                .build();

        assertEquals(15, stats.getTotalScheduled());
        assertEquals(5L, stats.getStatusBreakdown().get("PENDING"));
    }

    @Test
    void testScheduleEmail_lowPriority() {
        when(scheduledEmailRepository.save(any(ScheduledEmail.class))).thenAnswer(inv -> inv.getArgument(0));

        sampleRequest.setPriority(1);

        ScheduledEmail result = scheduledEmailService.scheduleEmail(sampleRequest);

        assertEquals(Integer.valueOf(1), result.getPriority());
    }

    @Test
    void testScheduleEmail_customRetries() {
        when(scheduledEmailRepository.save(any(ScheduledEmail.class))).thenAnswer(inv -> inv.getArgument(0));

        sampleRequest.setMaxRetries(5);

        ScheduledEmail result = scheduledEmailService.scheduleEmail(sampleRequest);

        assertEquals(Integer.valueOf(5), result.getMaxRetries());
    }

    @Test
    void testScheduleEmail_recurringWithPattern() {
        when(scheduledEmailRepository.save(any(ScheduledEmail.class))).thenAnswer(inv -> inv.getArgument(0));

        sampleRequest.setRecurring(true);
        sampleRequest.setRecurrencePattern("WEEKLY");

        ScheduledEmail result = scheduledEmailService.scheduleEmail(sampleRequest);

        assertTrue(result.getRecurring());
        assertEquals("WEEKLY", result.getRecurrencePattern());
    }

    @Test
    void testScheduleEmail_withCcAndBcc() {
        when(scheduledEmailRepository.save(any(ScheduledEmail.class))).thenAnswer(inv -> inv.getArgument(0));

        sampleRequest.setCcRecipients("cc1@example.com,cc2@example.com");
        sampleRequest.setBccRecipients("bcc1@example.com");

        ScheduledEmail result = scheduledEmailService.scheduleEmail(sampleRequest);

        assertEquals("cc1@example.com,cc2@example.com", result.getCcRecipients());
        assertEquals("bcc1@example.com", result.getBccRecipients());
    }

    @Test
    void testScheduleEmail_withCustomHeaders() {
        when(scheduledEmailRepository.save(any(ScheduledEmail.class))).thenAnswer(inv -> inv.getArgument(0));

        sampleRequest.setCustomHeaders("{\"X-Priority\": \"1\"}");

        ScheduledEmail result = scheduledEmailService.scheduleEmail(sampleRequest);

        assertEquals("{\"X-Priority\": \"1\"}", result.getCustomHeaders());
    }

    @Test
    void testScheduleEmail_readReceipt() {
        when(scheduledEmailRepository.save(any(ScheduledEmail.class))).thenAnswer(inv -> inv.getArgument(0));

        sampleRequest.setReadReceiptRequested(true);

        ScheduledEmail result = scheduledEmailService.scheduleEmail(sampleRequest);

        assertTrue(result.getReadReceiptRequested());
    }

    @Test
    void testForceProcess_failedDelivery() {
        ScheduledEmail email = ScheduledEmail.builder()
                .id(2L)
                .recipientEmail("test@example.com")
                .subject("Test")
                .body("")
                .status("PENDING")
                .attemptCount(0)
                .maxRetries(3)
                .build();

        when(scheduledEmailRepository.findById(2L)).thenReturn(Optional.of(email));
        when(scheduledEmailRepository.save(any(ScheduledEmail.class))).thenAnswer(inv -> inv.getArgument(0));
        when(logRepository.save(any(ScheduledEmailLog.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean result = scheduledEmailService.forceProcess(2L);

        assertFalse(result);
        assertEquals("RETRYING", email.getStatus());
        assertNotNull(email.getNextRetryAt());
    }
}
