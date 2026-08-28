package com.email.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enterprise-grade service for managing reusable email snippets.
 * Supports variable substitution, AI-enhanced expansion, usage analytics,
 * search/filtering, and import/export of snippet collections.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSnippetService {

    private final EmailSnippetRepository snippetRepository;
    private final SnippetUsageLogRepository usageLogRepository;
    private final EmailGeneratorService emailGeneratorService;

    // ─── CRUD Operations ──────────────────────────────────────────────

    /**
     * Create a new email snippet.
     */
    @Transactional
    public EmailSnippet createSnippet(SnippetCreateRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Snippet title is required.");
        }
        if (request.getBody() == null || request.getBody().trim().isEmpty()) {
            throw new IllegalArgumentException("Snippet body is required.");
        }

        EmailSnippet snippet = EmailSnippet.builder()
                .title(request.getTitle().trim())
                .body(request.getBody().trim())
                .category(request.getCategory() != null ? request.getCategory().trim() : null)
                .shortcut(request.getShortcut() != null ? request.getShortcut().trim() : null)
                .requiredVariables(request.getRequiredVariables())
                .defaultVariableValues(request.getDefaultVariableValues())
                .tone(request.getTone())
                .language(request.getLanguage())
                .pinned(Boolean.TRUE.equals(request.getPinned()))
                .shared(Boolean.TRUE.equals(request.getShared()))
                .usageCount(0L)
                .avgSatisfaction(0.0)
                .satisfactionVotes(0)
                .build();

        if (snippet.getShortcut() != null && !snippet.getShortcut().isEmpty()) {
            validateUniqueShortcut(snippet.getShortcut(), null);
        }

        EmailSnippet saved = snippetRepository.save(snippet);
        log.info("Created snippet: id={}, title='{}', category='{}'", saved.getId(), saved.getTitle(), saved.getCategory());
        return saved;
    }

    /**
     * Update an existing snippet.
     */
    @Transactional
    public EmailSnippet updateSnippet(Long id, SnippetCreateRequest request) {
        EmailSnippet existing = snippetRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Snippet not found with id: " + id));

        if (request.getTitle() != null) existing.setTitle(request.getTitle().trim());
        if (request.getBody() != null) existing.setBody(request.getBody().trim());
        if (request.getCategory() != null) existing.setCategory(request.getCategory().trim());
        if (request.getRequiredVariables() != null) existing.setRequiredVariables(request.getRequiredVariables());
        if (request.getDefaultVariableValues() != null) existing.setDefaultVariableValues(request.getDefaultVariableValues());
        if (request.getTone() != null) existing.setTone(request.getTone());
        if (request.getLanguage() != null) existing.setLanguage(request.getLanguage());
        if (request.getPinned() != null) existing.setPinned(request.getPinned());
        if (request.getShared() != null) existing.setShared(request.getShared());

        if (request.getShortcut() != null) {
            String newShortcut = request.getShortcut().trim();
            if (!newShortcut.isEmpty()) {
                validateUniqueShortcut(newShortcut, id);
            }
            existing.setShortcut(newShortcut.isEmpty() ? null : newShortcut);
        }

        EmailSnippet saved = snippetRepository.save(existing);
        log.info("Updated snippet: id={}, title='{}'", saved.getId(), saved.getTitle());
        return saved;
    }

    /**
     * Delete a snippet by ID.
     */
    @Transactional
    public boolean deleteSnippet(Long id) {
        if (snippetRepository.existsById(id)) {
            snippetRepository.deleteById(id);
            log.info("Deleted snippet: id={}", id);
            return true;
        }
        return false;
    }

    /**
     * Get a single snippet by ID.
     */
    public EmailSnippet getSnippetById(Long id) {
        return snippetRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Snippet not found with id: " + id));
    }

    /**
     * Get all snippets ordered by pinned first, then usage count.
     */
    public List<EmailSnippet> getAllSnippets() {
        return snippetRepository.findAllByOrderByPinnedDescUsageCountDesc();
    }

    // ─── Variable Expansion ────────────────────────────────────────────

    /**
     * Expand a snippet by substituting variables and optionally enhancing with AI.
     * Returns the expanded content and logs the usage event.
     */
    @Transactional
    public Map<String, Object> expandSnippet(SnippetExpandRequest request) {
        long startTime = System.currentTimeMillis();

        // Resolve the snippet
        EmailSnippet snippet = resolveSnippet(request);

        // Build variable map: defaults + provided overrides
        Map<String, String> effectiveVariables = new LinkedHashMap<>(snippet.getDefaultValues());
        if (request.getVariables() != null) {
            effectiveVariables.putAll(request.getVariables());
        }

        // Substitute variables in the body
        String expandedContent = substituteVariables(snippet.getBody(), effectiveVariables);

        // AI enhancement (optional)
        String provider = null;
        String model = null;
        if (request.isAiEnhanced()) {
            provider = request.getProvider() != null ? request.getProvider() : "groq";
            model = request.getModel();
            try {
                EmailRequest aiRequest = new EmailRequest();
                aiRequest.setEmailContent(expandedContent);
                aiRequest.setTone(snippet.getTone() != null ? snippet.getTone() : "professional");
                aiRequest.setProvider(provider);
                aiRequest.setModel(model);
                aiRequest.setApiKey(request.getApiKey());
                aiRequest.setComposeMode(true);
                aiRequest.setLanguage(snippet.getLanguage());

                String aiResult = emailGeneratorService.generateEmailReply(aiRequest);
                if (aiResult != null && !aiResult.trim().isEmpty()) {
                    expandedContent = aiResult;
                }
            } catch (Exception e) {
                log.warn("AI enhancement failed for snippet {}, falling back to plain expansion: {}",
                        snippet.getId(), e.getMessage());
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        // Log the usage
        String variablesJson = serializeVariables(effectiveVariables);
        String status = "SUCCESS";
        String triggerSource = request.getTriggerSource() != null ? request.getTriggerSource() : "api";

        try {
            SnippetUsageLog usageLog = SnippetUsageLog.builder()
                    .snippetId(snippet.getId())
                    .expandedContent(expandedContent)
                    .variablesUsed(variablesJson)
                    .provider(provider)
                    .model(model)
                    .durationMs(duration)
                    .status(status)
                    .triggerSource(triggerSource)
                    .build();
            usageLogRepository.save(usageLog);

            // Increment usage counter
            snippet.setUsageCount(snippet.getUsageCount() + 1);
            snippetRepository.save(snippet);
        } catch (Exception e) {
            log.error("Failed to log snippet usage: {}", e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("snippetId", snippet.getId());
        result.put("title", snippet.getTitle());
        result.put("expandedContent", expandedContent);
        result.put("variablesUsed", effectiveVariables);
        result.put("aiEnhanced", request.isAiEnhanced());
        result.put("provider", provider);
        result.put("durationMs", duration);
        result.put("triggerSource", triggerSource);
        return result;
    }

    // ─── Search & Filtering ────────────────────────────────────────────

    /**
     * Full-text search across snippet titles and bodies.
     */
    public List<EmailSnippet> searchSnippets(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllSnippets();
        }
        return snippetRepository.searchByTitleOrBody(query.trim());
    }

    /**
     * Find snippets by category.
     */
    public List<EmailSnippet> getSnippetsByCategory(String category) {
        return snippetRepository.findByCategoryOrderByUsageCountDesc(category);
    }

    /**
     * Find snippets by multiple categories.
     */
    public List<EmailSnippet> getSnippetsByCategories(List<String> categories) {
        return snippetRepository.findByCategories(categories);
    }

    /**
     * Find snippets that use a specific variable.
     */
    public List<EmailSnippet> getSnippetsByVariable(String varName) {
        return snippetRepository.findByRequiredVariable(varName);
    }

    /**
     * Get the top N most-used snippets.
     */
    public List<EmailSnippet> getTopSnippets(int n) {
        return snippetRepository.findTopNByOrderByUsageCountDesc(n);
    }

    /**
     * Get snippets updated in the last N days.
     */
    public List<EmailSnippet> getRecentlyUpdatedSnippets(int days) {
        return snippetRepository.findRecentlyUpdated(LocalDateTime.now().minusDays(days));
    }

    /**
     * Get all unique categories with snippet counts.
     */
    public Map<String, Long> getCategoryCounts() {
        List<Object[]> results = snippetRepository.countByCategory();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : results) {
            String category = (String) row[0];
            Long count = (Long) row[1];
            counts.put(category, count);
        }
        return counts;
    }

    // ─── Usage Analytics ───────────────────────────────────────────────

    /**
     * Get comprehensive usage statistics across the entire system.
     */
    public SnippetUsageStats getSystemUsageStats() {
        List<SnippetUsageLog> allLogs = usageLogRepository.findAll();
        List<EmailSnippet> allSnippets = snippetRepository.findAll();

        long totalExpansions = allLogs.size();
        long activeSnippetCount = allLogs.stream()
                .map(SnippetUsageLog::getSnippetId)
                .distinct().count();
        long totalSnippetCount = allSnippets.size();

        Double systemAvgSatisfaction = allLogs.stream()
                .filter(l -> l.getSatisfactionRating() != null)
                .mapToInt(SnippetUsageLog::getSatisfactionRating)
                .average()
                .orElse(0.0);

        Double avgDuration = allLogs.stream()
                .filter(l -> l.getDurationMs() != null)
                .mapToLong(SnippetUsageLog::getDurationMs)
                .average()
                .orElse(0.0);

        Map<String, Long> triggerBreakdown = allLogs.stream()
                .filter(l -> l.getTriggerSource() != null)
                .collect(Collectors.groupingBy(SnippetUsageLog::getTriggerSource, Collectors.counting()));

        Map<String, Long> providerBreakdown = allLogs.stream()
                .filter(l -> l.getProvider() != null)
                .collect(Collectors.groupingBy(SnippetUsageLog::getProvider, Collectors.counting()));

        Map<String, Long> dailyTrend = buildDailyUsageTrend(allLogs);

        Map<String, Long> categoryBreakdown = allSnippets.stream()
                .filter(s -> s.getCategory() != null)
                .collect(Collectors.groupingBy(EmailSnippet::getCategory, Collectors.counting()));

        Map<Long, Long> topSnippets = allLogs.stream()
                .collect(Collectors.groupingBy(SnippetUsageLog::getSnippetId, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        return SnippetUsageStats.builder()
                .totalExpansions(totalExpansions)
                .activeSnippetCount(activeSnippetCount)
                .totalSnippetCount(totalSnippetCount)
                .systemAvgSatisfaction(Math.round(systemAvgSatisfaction * 100.0) / 100.0)
                .avgDurationMs(Math.round(avgDuration * 100.0) / 100.0)
                .triggerSourceBreakdown(triggerBreakdown)
                .providerBreakdown(providerBreakdown)
                .dailyUsageTrend(dailyTrend)
                .categoryBreakdown(categoryBreakdown)
                .topSnippets(topSnippets)
                .build();
    }

    /**
     * Get per-snippet usage statistics.
     */
    public Map<String, Object> getSnippetUsageDetails(Long snippetId) {
        EmailSnippet snippet = getSnippetById(snippetId);
        List<SnippetUsageLog> logs = usageLogRepository.findBySnippetIdOrderByUsedAtDesc(snippetId);

        long successCount = logs.stream().filter(l -> "SUCCESS".equals(l.getStatus())).count();
        long errorCount = logs.size() - successCount;

        Double avgDuration = usageLogRepository.averageDurationBySnippetId(snippetId);
        Double avgSatisfaction = usageLogRepository.averageSatisfactionBySnippetId(snippetId);
        Double avgContentLength = usageLogRepository.averageContentLengthBySnippetId(snippetId);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("snippetId", snippet.getId());
        details.put("title", snippet.getTitle());
        details.put("totalUsages", logs.size());
        details.put("successCount", successCount);
        details.put("errorCount", errorCount);
        details.put("successRate", logs.isEmpty() ? 0.0 : Math.round(((double) successCount / logs.size()) * 10000.0) / 100.0);
        details.put("avgDurationMs", avgDuration != null ? Math.round(avgDuration * 100.0) / 100.0 : 0.0);
        details.put("avgSatisfaction", avgSatisfaction != null ? Math.round(avgSatisfaction * 100.0) / 100.0 : null);
        details.put("avgContentLength", avgContentLength != null ? Math.round(avgContentLength) : 0);
        details.put("lastUsedAt", logs.isEmpty() ? null : logs.get(0).getUsedAt());
        details.put("recentLogs", logs.stream().limit(20).collect(Collectors.toList()));
        return details;
    }

    // ─── Import / Export ───────────────────────────────────────────────

    /**
     * Export all snippets (or filtered set) as a JSON-serializable list.
     */
    public List<Map<String, Object>> exportSnippets(String category) {
        List<EmailSnippet> snippets;
        if (category != null && !category.trim().isEmpty()) {
            snippets = snippetRepository.findByCategoryOrderByUsageCountDesc(category.trim());
        } else {
            snippets = snippetRepository.findAllByOrderByPinnedDescUsageCountDesc();
        }

        return snippets.stream().map(this::snippetToExportMap).collect(Collectors.toList());
    }

    /**
     * Import snippets from an exported list. Returns count of successfully imported snippets.
     */
    @Transactional
    public int importSnippets(List<Map<String, Object>> snippetDataList) {
        int imported = 0;
        for (Map<String, Object> data : snippetDataList) {
            try {
                SnippetCreateRequest request = new SnippetCreateRequest();
                request.setTitle(getStringOrEmpty(data, "title"));
                request.setBody(getStringOrEmpty(data, "body"));
                request.setCategory(getStringOrNull(data, "category"));
                request.setShortcut(getStringOrNull(data, "shortcut"));
                request.setRequiredVariables(getStringOrNull(data, "requiredVariables"));
                request.setDefaultVariableValues(getStringOrNull(data, "defaultVariableValues"));
                request.setTone(getStringOrNull(data, "tone"));
                request.setLanguage(getStringOrNull(data, "language"));
                request.setPinned(getBooleanOrDefault(data, "pinned", false));
                request.setShared(getBooleanOrDefault(data, "shared", false));

                createSnippet(request);
                imported++;
            } catch (Exception e) {
                log.warn("Failed to import snippet '{}': {}", data.get("title"), e.getMessage());
            }
        }
        log.info("Imported {} / {} snippets", imported, snippetDataList.size());
        return imported;
    }

    // ─── Satisfaction Feedback ─────────────────────────────────────────

    /**
     * Record user satisfaction for a snippet usage event.
     */
    @Transactional
    public boolean recordSatisfaction(Long usageLogId, int rating, String feedback) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Satisfaction rating must be between 1 and 5.");
        }
        Optional<SnippetUsageLog> logOpt = usageLogRepository.findById(usageLogId);
        if (logOpt.isEmpty()) return false;

        SnippetUsageLog usageLog = logOpt.get();
        usageLog.setSatisfactionRating(rating);
        usageLog.setUserFeedback(feedback);
        usageLogRepository.save(usageLog);

        // Update aggregate satisfaction on the snippet
        EmailSnippet snippet = snippetRepository.findById(usageLog.getSnippetId()).orElse(null);
        if (snippet != null) {
            int newVoteCount = snippet.getSatisfactionVotes() + 1;
            double newAvg = ((snippet.getAvgSatisfaction() * snippet.getSatisfactionVotes()) + rating) / newVoteCount;
            snippet.setAvgSatisfaction(Math.round(newAvg * 100.0) / 100.0);
            snippet.setSatisfactionVotes(newVoteCount);
            snippetRepository.save(snippet);
        }
        return true;
    }

    // ─── Internal Helpers ──────────────────────────────────────────────

    private EmailSnippet resolveSnippet(SnippetExpandRequest request) {
        if (request.getSnippetId() != null) {
            return getSnippetById(request.getSnippetId());
        }
        if (request.getShortcut() != null && !request.getShortcut().trim().isEmpty()) {
            return snippetRepository.findByShortcut(request.getShortcut().trim())
                    .orElseThrow(() -> new NoSuchElementException(
                            "No snippet found with shortcut: " + request.getShortcut()));
        }
        throw new IllegalArgumentException("Either snippetId or shortcut must be provided.");
    }

    /**
     * Substitute {variableName} placeholders with actual values.
     */
    String substituteVariables(String body, Map<String, String> variables) {
        if (body == null || variables == null || variables.isEmpty()) {
            return body;
        }
        String result = body;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    private void validateUniqueShortcut(String shortcut, Long excludeId) {
        boolean exists;
        if (excludeId != null) {
            exists = snippetRepository.existsByShortcutExcludingId(shortcut, excludeId);
        } else {
            exists = snippetRepository.findByShortcut(shortcut).isPresent();
        }
        if (exists) {
            throw new IllegalArgumentException("Shortcut '" + shortcut + "' is already in use.");
        }
    }

    private Map<String, Long> buildDailyUsageTrend(List<SnippetUsageLog> logs) {
        LocalDate today = LocalDate.now();
        Map<String, Long> trend = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateKey = date.format(formatter);
            long count = logs.stream()
                    .filter(l -> l.getUsedAt() != null && l.getUsedAt().toLocalDate().equals(date))
                    .count();
            trend.put(dateKey, count);
        }
        return trend;
    }

    private Map<String, Object> snippetToExportMap(EmailSnippet snippet) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", snippet.getTitle());
        map.put("body", snippet.getBody());
        map.put("category", snippet.getCategory());
        map.put("shortcut", snippet.getShortcut());
        map.put("requiredVariables", snippet.getRequiredVariables());
        map.put("defaultVariableValues", snippet.getDefaultVariableValues());
        map.put("tone", snippet.getTone());
        map.put("language", snippet.getLanguage());
        map.put("pinned", snippet.getPinned());
        map.put("shared", snippet.getShared());
        map.put("usageCount", snippet.getUsageCount());
        return map;
    }

    private String serializeVariables(Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private String getStringOrEmpty(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private String getStringOrNull(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private boolean getBooleanOrDefault(Map<String, Object> map, String key, boolean defaultVal) {
        Object val = map.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val != null) return Boolean.parseBoolean(val.toString());
        return defaultVal;
    }
}
