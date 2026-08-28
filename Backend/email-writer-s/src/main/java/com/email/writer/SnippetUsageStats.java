package com.email.writer;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;

/**
 * Response DTO containing aggregated usage statistics for a snippet or the system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnippetUsageStats {

    /**
     * Total number of times snippets have been used across the system.
     */
    private long totalExpansions;

    /**
     * Total number of unique snippets that have been used at least once.
     */
    private long activeSnippetCount;

    /**
     * Total number of snippets in the system.
     */
    private long totalSnippetCount;

    /**
     * Average satisfaction rating across all rated usages (1-5).
     */
    private Double systemAvgSatisfaction;

    /**
     * Average expansion duration in milliseconds.
     */
    private Double avgDurationMs;

    /**
     * Usage counts grouped by trigger source (shortcut, api, dropdown, search).
     */
    private Map<String, Long> triggerSourceBreakdown;

    /**
     * Usage counts grouped by LLM provider.
     */
    private Map<String, Long> providerBreakdown;

    /**
     * Daily usage counts for the last 30 days (date string -> count).
     */
    private Map<String, Long> dailyUsageTrend;

    /**
     * Snippet counts grouped by category.
     */
    private Map<String, Long> categoryBreakdown;

    /**
     * Top 10 most-used snippets (id -> count).
     */
    private Map<Long, Long> topSnippets;
}
