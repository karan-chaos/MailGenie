package com.email.writer;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a reusable email snippet with variable placeholders.
 * Snippets support template variables like {name}, {company}, {date}
 * that get expanded at generation time.
 */
@Entity
@Table(name = "email_snippets", indexes = {
        @Index(name = "idx_snippet_category", columnList = "category"),
        @Index(name = "idx_snippet_shortcut", columnList = "shortcut"),
        @Index(name = "idx_snippet_frequent", columnList = "usageCount DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailSnippet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(length = 50)
    private String category;

    /**
     * Optional keyboard shortcut (e.g. "/thx") that triggers this snippet.
     */
    @Column(length = 30, unique = true)
    private String shortcut;

    /**
     * Comma-separated list of variable names required by this snippet.
     * For example: "name,company,date"
     */
    @Column(length = 500)
    private String requiredVariables;

    /**
     * Default values for variables as a JSON-style key=value pairs,
     * separated by semicolons. Example: "name=John;company=Acme"
     */
    @Column(length = 1000)
    private String defaultVariableValues;

    /**
     * Optional tone to apply when expanding the snippet.
     */
    @Column(length = 30)
    private String tone;

    /**
     * Optional language override for expansion.
     */
    @Column(length = 30)
    private String language;

    /**
     * Number of times this snippet has been used/expanded.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long usageCount = 0L;

    /**
     * Average user satisfaction rating (1-5) across usage sessions.
     */
    @Column(nullable = false)
    @Builder.Default
    private Double avgSatisfaction = 0.0;

    /**
     * Total satisfaction votes received.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer satisfactionVotes = 0;

    /**
     * Whether this snippet is pinned to the top of listings.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean pinned = false;

    /**
     * Whether this snippet is shared across all users.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean shared = false;

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
     * Extract the list of variable names from the comma-separated requiredVariables field.
     */
    @Transient
    public List<String> getVariableNames() {
        if (requiredVariables == null || requiredVariables.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<String> names = new ArrayList<>();
        for (String v : requiredVariables.split(",")) {
            String trimmed = v.trim();
            if (!trimmed.isEmpty()) {
                names.add(trimmed);
            }
        }
        return names;
    }

    /**
     * Parse the defaultVariableValues into a map.
     */
    @Transient
    public java.util.Map<String, String> getDefaultValues() {
        java.util.Map<String, String> defaults = new java.util.HashMap<>();
        if (defaultVariableValues == null || defaultVariableValues.trim().isEmpty()) {
            return defaults;
        }
        for (String pair : defaultVariableValues.split(";")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                defaults.put(kv[0].trim(), kv[1].trim());
            }
        }
        return defaults;
    }
}
