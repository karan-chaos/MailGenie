package com.email.writer;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing endpoints for the Email Snippet & Quick-Reply system.
 * Provides CRUD, variable expansion, search, analytics, import/export, and satisfaction feedback.
 */
@RestController
@RequestMapping("/api/snippets")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "*"})
public class EmailSnippetController {

    private final EmailSnippetService snippetService;

    // ─── CRUD ──────────────────────────────────────────────────────────

    /**
     * Create a new email snippet.
     */
    @PostMapping
    public ResponseEntity<EmailSnippet> createSnippet(@RequestBody SnippetCreateRequest request) {
        EmailSnippet created = snippetService.createSnippet(request);
        return ResponseEntity.ok(created);
    }

    /**
     * Update an existing snippet by ID.
     */
    @PutMapping("/{id}")
    public ResponseEntity<EmailSnippet> updateSnippet(@PathVariable Long id,
                                                      @RequestBody SnippetCreateRequest request) {
        EmailSnippet updated = snippetService.updateSnippet(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a snippet by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteSnippet(@PathVariable Long id) {
        boolean deleted = snippetService.deleteSnippet(id);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    /**
     * Get a single snippet by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EmailSnippet> getSnippet(@PathVariable Long id) {
        return ResponseEntity.ok(snippetService.getSnippetById(id));
    }

    /**
     * Get all snippets, ordered by pinned first then usage count.
     */
    @GetMapping
    public ResponseEntity<List<EmailSnippet>> getAllSnippets() {
        return ResponseEntity.ok(snippetService.getAllSnippets());
    }

    // ─── Expansion ─────────────────────────────────────────────────────

    /**
     * Expand a snippet with variable values, optionally enhanced by AI.
     * This is the primary endpoint used by the Chrome extension and frontend.
     */
    @PostMapping("/expand")
    public ResponseEntity<Map<String, Object>> expandSnippet(@RequestBody SnippetExpandRequest request) {
        Map<String, Object> result = snippetService.expandSnippet(request);
        return ResponseEntity.ok(result);
    }

    // ─── Search & Filtering ────────────────────────────────────────────

    /**
     * Full-text search across snippet titles and bodies.
     */
    @GetMapping("/search")
    public ResponseEntity<List<EmailSnippet>> searchSnippets(@RequestParam String q) {
        return ResponseEntity.ok(snippetService.searchSnippets(q));
    }

    /**
     * Get snippets filtered by category.
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<EmailSnippet>> getSnippetsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(snippetService.getSnippetsByCategory(category));
    }

    /**
     * Get snippets that use a specific variable.
     */
    @GetMapping("/variable/{varName}")
    public ResponseEntity<List<EmailSnippet>> getSnippetsByVariable(@PathVariable String varName) {
        return ResponseEntity.ok(snippetService.getSnippetsByVariable(varName));
    }

    /**
     * Get the top N most-used snippets.
     */
    @GetMapping("/top/{n}")
    public ResponseEntity<List<EmailSnippet>> getTopSnippets(@PathVariable int n) {
        return ResponseEntity.ok(snippetService.getTopSnippets(n));
    }

    /**
     * Get snippets updated in the last N days.
     */
    @GetMapping("/recent/{days}")
    public ResponseEntity<List<EmailSnippet>> getRecentlyUpdated(@PathVariable int days) {
        return ResponseEntity.ok(snippetService.getRecentlyUpdatedSnippets(days));
    }

    /**
     * Get all unique categories with their snippet counts.
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Long>> getCategoryCounts() {
        return ResponseEntity.ok(snippetService.getCategoryCounts());
    }

    // ─── Analytics ─────────────────────────────────────────────────────

    /**
     * Get comprehensive system-wide usage statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<SnippetUsageStats> getSystemStats() {
        return ResponseEntity.ok(snippetService.getSystemUsageStats());
    }

    /**
     * Get detailed usage statistics for a specific snippet.
     */
    @GetMapping("/{id}/usage")
    public ResponseEntity<Map<String, Object>> getSnippetUsageDetails(@PathVariable Long id) {
        return ResponseEntity.ok(snippetService.getSnippetUsageDetails(id));
    }

    // ─── Import / Export ───────────────────────────────────────────────

    /**
     * Export snippets as a JSON list. Optionally filter by category.
     */
    @GetMapping("/export")
    public ResponseEntity<List<Map<String, Object>>> exportSnippets(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(snippetService.exportSnippets(category));
    }

    /**
     * Import snippets from a JSON list. Returns the count of successfully imported snippets.
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importSnippets(@RequestBody List<Map<String, Object>> data) {
        int imported = snippetService.importSnippets(data);
        return ResponseEntity.ok(Map.of(
                "imported", imported,
                "total", data.size()
        ));
    }

    // ─── Satisfaction Feedback ─────────────────────────────────────────

    /**
     * Record user satisfaction for a previous snippet usage.
     */
    @PostMapping("/usage/{usageLogId}/feedback")
    public ResponseEntity<Map<String, Boolean>> recordSatisfaction(
            @PathVariable Long usageLogId,
            @RequestBody Map<String, Object> feedback) {

        int rating = (int) feedback.getOrDefault("rating", 0);
        String text = feedback.get("feedback") != null ? feedback.get("feedback").toString() : null;

        boolean recorded = snippetService.recordSatisfaction(usageLogId, rating, text);
        return ResponseEntity.ok(Map.of("recorded", recorded));
    }
}
