package com.email.writer;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Request DTO for scheduling a new email.
 */
@Data
public class ScheduleEmailRequest {

    private String recipientEmail;
    private String senderName;
    private String subject;
    private String body;
    private Integer priority;
    private LocalDateTime scheduledFor;
    private Integer maxRetries;
    private String provider;
    private String model;
    private String apiKey;
    private String batchId;
    private Boolean recurring;
    private String recurrencePattern;
    private String originalPrompt;
    private String userNotes;
    private String ccRecipients;
    private String bccRecipients;
    private Boolean readReceiptRequested;
    private String customHeaders;
    private Boolean aiGenerateBody;
    private String aiTone;
    private String aiLanguage;
    private String aiCustomInstructions;
}
