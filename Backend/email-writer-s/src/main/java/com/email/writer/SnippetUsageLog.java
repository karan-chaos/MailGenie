package com.email.writer;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * Entity tracking each individual use/expansion of a snippet.
 * Enables analytics: most-used snippets, time-of-day patterns,
 * variable fill rates, and satisfaction trends.
 */
@Entity
@Table(name = "snippet_usage_logs", indexes = {
        @Index(name = "idx_usage_snippet_id", columnList = "snippetId"),
        @Index(name = "idx_usage_timestamp", columnList = "usedAt"),
        @Index(name = "idx_usage_provider", columnList = "provider")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnippetUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long snippetId;

    /**
     * The fully expanded text after variable substitution.
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String expandedContent;

    /**
     * The variables that were provided during expansion, as JSON.
     * Example: {"name":"Alice","company":"TechCorp"}
     */
    @Column(columnDefinition = "TEXT")
    private String variablesUsed;

    /**
     * The LLM provider used for AI enhancement (null if plain expansion).
     */
    @Column(length = 30)
    private String provider;

    /**
     * The LLM model used for AI enhancement (null if plain expansion).
     */
    @Column(length = 60)
    private String model;

    /**
     * Time in milliseconds for the expansion to complete.
     */
    private Long durationMs;

    /**
     * SUCCESS or ERROR.
     */
    @Column(nullable = false, length = 10)
    private String status;

    /**
     * How the snippet was triggered: "shortcut", "api", "dropdown", "search".
     */
    @Column(length = 20)
    private String triggerSource;

    /**
     * User-provided satisfaction rating after using the snippet (1-5).
     * Null means not yet rated.
     */
    private Integer satisfactionRating;

    /**
     * Optional user feedback text.
     */
    @Column(columnDefinition = "TEXT")
    private String userFeedback;

    @Column(nullable = false)
    private LocalDateTime usedAt;

    @PrePersist
    protected void onCreate() {
        usedAt = LocalDateTime.now();
    }
}
