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
class EmailSnippetServiceTest {

    @Mock
    private EmailSnippetRepository snippetRepository;

    @Mock
    private SnippetUsageLogRepository usageLogRepository;

    @Mock
    private EmailGeneratorService emailGeneratorService;

    @InjectMocks
    private EmailSnippetService snippetService;

    private EmailSnippet sampleSnippet;

    @BeforeEach
    void setUp() {
        sampleSnippet = EmailSnippet.builder()
                .id(1L)
                .title("Quick Thanks")
                .body("Hi {name},\n\nThank you for {reason}.\n\nBest,\n{sender}")
                .category("Courtesy")
                .shortcut("/thx")
                .requiredVariables("name,reason,sender")
                .defaultVariableValues("name=there;reason=your help;sender=[Your Name]")
                .tone("professional")
                .pinned(false)
                .shared(true)
                .usageCount(0L)
                .avgSatisfaction(0.0)
                .satisfactionVotes(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─── Variable Substitution Tests ───────────────────────────────────

    @Test
    void testSubstituteVariables_basicReplacement() {
        Map<String, String> vars = Map.of("name", "Alice", "reason", "the report", "sender", "Bob");
        String result = snippetService.substituteVariables(sampleSnippet.getBody(), vars);

        assertTrue(result.contains("Hi Alice,"));
        assertTrue(result.contains("Thank you for the report."));
        assertTrue(result.contains("Best,\nBob"));
        assertFalse(result.contains("{name}"));
        assertFalse(result.contains("{reason}"));
        assertFalse(result.contains("{sender}"));
    }

    @Test
    void testSubstituteVariables_emptyBody() {
        String result = snippetService.substituteVariables("", Map.of("name", "Alice"));
        assertEquals("", result);
    }

    @Test
    void testSubstituteVariables_nullBody() {
        String result = snippetService.substituteVariables(null, Map.of("name", "Alice"));
        assertNull(result);
    }

    @Test
    void testSubstituteVariables_noVariables() {
        String body = "Hello world, no placeholders here.";
        String result = snippetService.substituteVariables(body, Map.of());
        assertEquals(body, result);
    }

    @Test
    void testSubstituteVariables_nullVariables() {
        String body = "Hello {name}";
        String result = snippetService.substituteVariables(body, null);
        assertEquals(body, result);
    }

    @Test
    void testSubstituteVariables_unprovidedVariableBecomesEmpty() {
        Map<String, String> vars = Map.of("name", "Alice");
        String result = snippetService.substituteVariables("Hi {name}, {reason}", vars);
        assertTrue(result.contains("Hi Alice, "));
        assertFalse(result.contains("{reason}"));
    }

    // ─── CRUD Tests ────────────────────────────────────────────────────

    @Test
    void testCreateSnippet_success() {
        when(snippetRepository.save(any(EmailSnippet.class))).thenAnswer(inv -> {
            EmailSnippet s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        SnippetCreateRequest request = new SnippetCreateRequest();
        request.setTitle("Test Snippet");
        request.setBody("Hello {name}");
        request.setCategory("Test");
        request.setShortcut("/test");

        EmailSnippet created = snippetService.createSnippet(request);

        assertNotNull(created);
        assertEquals("Test Snippet", created.getTitle());
        assertEquals("Hello {name}", created.getBody());
        assertEquals("/test", created.getShortcut());
        verify(snippetRepository).save(any(EmailSnippet.class));
    }

    @Test
    void testCreateSnippet_throwsOnEmptyTitle() {
        SnippetCreateRequest request = new SnippetCreateRequest();
        request.setTitle("");
        request.setBody("Hello");

        assertThrows(IllegalArgumentException.class, () -> snippetService.createSnippet(request));
    }

    @Test
    void testCreateSnippet_throwsOnEmptyBody() {
        SnippetCreateRequest request = new SnippetCreateRequest();
        request.setTitle("Test");
        request.setBody("");

        assertThrows(IllegalArgumentException.class, () -> snippetService.createSnippet(request));
    }

    @Test
    void testUpdateSnippet_success() {
        when(snippetRepository.findById(1L)).thenReturn(Optional.of(sampleSnippet));
        when(snippetRepository.save(any(EmailSnippet.class))).thenAnswer(inv -> inv.getArgument(0));

        SnippetCreateRequest request = new SnippetCreateRequest();
        request.setTitle("Updated Title");

        EmailSnippet updated = snippetService.updateSnippet(1L, request);

        assertEquals("Updated Title", updated.getTitle());
        verify(snippetRepository).save(any(EmailSnippet.class));
    }

    @Test
    void testUpdateSnippet_notFound() {
        when(snippetRepository.findById(99L)).thenReturn(Optional.empty());

        SnippetCreateRequest request = new SnippetCreateRequest();
        request.setTitle("Updated");

        assertThrows(NoSuchElementException.class, () -> snippetService.updateSnippet(99L, request));
    }

    @Test
    void testDeleteSnippet_success() {
        when(snippetRepository.existsById(1L)).thenReturn(true);

        boolean deleted = snippetService.deleteSnippet(1L);

        assertTrue(deleted);
        verify(snippetRepository).deleteById(1L);
    }

    @Test
    void testDeleteSnippet_notFound() {
        when(snippetRepository.existsById(99L)).thenReturn(false);

        boolean deleted = snippetService.deleteSnippet(99L);

        assertFalse(deleted);
    }

    // ─── Expansion Tests ───────────────────────────────────────────────

    @Test
    void testExpandSnippet_plainExpansion() {
        when(snippetRepository.findById(1L)).thenReturn(Optional.of(sampleSnippet));
        when(snippetRepository.save(any(EmailSnippet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usageLogRepository.save(any(SnippetUsageLog.class))).thenAnswer(inv -> inv.getArgument(0));

        SnippetExpandRequest request = new SnippetExpandRequest();
        request.setSnippetId(1L);
        request.setVariables(Map.of("name", "Alice", "reason", "the great work", "sender", "Bob"));

        Map<String, Object> result = snippetService.expandSnippet(request);

        assertNotNull(result);
        String content = (String) result.get("expandedContent");
        assertTrue(content.contains("Hi Alice,"));
        assertTrue(content.contains("Thank you for the great work."));
        assertEquals(1L, result.get("snippetId"));
        assertEquals(false, result.get("aiEnhanced"));
    }

    @Test
    void testExpandSnippet_usesDefaultsForMissingVars() {
        when(snippetRepository.findById(1L)).thenReturn(Optional.of(sampleSnippet));
        when(snippetRepository.save(any(EmailSnippet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usageLogRepository.save(any(SnippetUsageLog.class))).thenAnswer(inv -> inv.getArgument(0));

        SnippetExpandRequest request = new SnippetExpandRequest();
        request.setSnippetId(1L);
        request.setVariables(Map.of("name", "Alice"));

        Map<String, Object> result = snippetService.expandSnippet(request);
        String content = (String) result.get("expandedContent");

        assertTrue(content.contains("Hi Alice,"));
        assertTrue(content.contains("Thank you for your help."));
    }

    @Test
    void testExpandSnippet_byShortcut() {
        when(snippetRepository.findByShortcut("/thx")).thenReturn(Optional.of(sampleSnippet));
        when(snippetRepository.save(any(EmailSnippet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usageLogRepository.save(any(SnippetUsageLog.class))).thenAnswer(inv -> inv.getArgument(0));

        SnippetExpandRequest request = new SnippetExpandRequest();
        request.setShortcut("/thx");

        Map<String, Object> result = snippetService.expandSnippet(request);

        assertNotNull(result);
        assertEquals(1L, result.get("snippetId"));
    }

    @Test
    void testExpandSnippet_noIdOrShortcut_throws() {
        SnippetExpandRequest request = new SnippetExpandRequest();

        assertThrows(IllegalArgumentException.class, () -> snippetService.expandSnippet(request));
    }

    @Test
    void testExpandSnippet_notFound_throws() {
        when(snippetRepository.findById(99L)).thenReturn(Optional.empty());

        SnippetExpandRequest request = new SnippetExpandRequest();
        request.setSnippetId(99L);

        assertThrows(NoSuchElementException.class, () -> snippetService.expandSnippet(request));
    }

    // ─── Search Tests ──────────────────────────────────────────────────

    @Test
    void testSearchSnippets_emptyQuery_returnsAll() {
        when(snippetRepository.findAllByOrderByPinnedDescUsageCountDesc()).thenReturn(List.of(sampleSnippet));

        List<EmailSnippet> results = snippetService.searchSnippets("");

        assertEquals(1, results.size());
    }

    @Test
    void testSearchSnippets_nullQuery_returnsAll() {
        when(snippetRepository.findAllByOrderByPinnedDescUsageCountDesc()).thenReturn(List.of(sampleSnippet));

        List<EmailSnippet> results = snippetService.searchSnippets(null);

        assertEquals(1, results.size());
    }

    @Test
    void testSearchSnippets_withQuery_delegatesToRepo() {
        when(snippetRepository.searchByTitleOrBody("thanks")).thenReturn(List.of(sampleSnippet));

        List<EmailSnippet> results = snippetService.searchSnippets("thanks");

        assertEquals(1, results.size());
        verify(snippetRepository).searchByTitleOrBody("thanks");
    }

    // ─── Import/Export Tests ───────────────────────────────────────────

    @Test
    void testExportSnippets_all() {
        when(snippetRepository.findAllByOrderByPinnedDescUsageCountDesc()).thenReturn(List.of(sampleSnippet));

        List<Map<String, Object>> exported = snippetService.exportSnippets(null);

        assertEquals(1, exported.size());
        assertEquals("Quick Thanks", exported.get(0).get("title"));
    }

    @Test
    void testExportSnippets_byCategory() {
        when(snippetRepository.findByCategoryOrderByUsageCountDesc("Courtesy")).thenReturn(List.of(sampleSnippet));

        List<Map<String, Object>> exported = snippetService.exportSnippets("Courtesy");

        assertEquals(1, exported.size());
    }

    @Test
    void testImportSnippets_success() {
        when(snippetRepository.save(any(EmailSnippet.class))).thenAnswer(inv -> {
            EmailSnippet s = inv.getArgument(0);
            s.setId(20L);
            return s;
        });

        Map<String, Object> data = new HashMap<>();
        data.put("title", "Imported Snippet");
        data.put("body", "Hello {name}");
        data.put("category", "Test");

        int imported = snippetService.importSnippets(List.of(data));

        assertEquals(1, imported);
        verify(snippetRepository).save(any(EmailSnippet.class));
    }

    @Test
    void testImportSnippets_multipleWithOneFailure() {
        when(snippetRepository.save(any(EmailSnippet.class)))
                .thenAnswer(inv -> {
                    EmailSnippet s = inv.getArgument(0);
                    s.setId(20L);
                    return s;
                });

        Map<String, Object> valid = new HashMap<>();
        valid.put("title", "Valid");
        valid.put("body", "Body");

        Map<String, Object> invalid = new HashMap<>();
        invalid.put("title", "");
        invalid.put("body", "");

        int imported = snippetService.importSnippets(List.of(valid, invalid));

        assertEquals(1, imported);
    }

    // ─── Category Counts ───────────────────────────────────────────────

    @Test
    void testGetCategoryCounts() {
        when(snippetRepository.countByCategory()).thenReturn(List.of(
                new Object[]{"Courtesy", 5L},
                new Object[]{"Meeting", 3L}
        ));

        Map<String, Long> counts = snippetService.getCategoryCounts();

        assertEquals(5L, counts.get("Courtesy"));
        assertEquals(3L, counts.get("Meeting"));
    }

    // ─── Satisfaction Tests ────────────────────────────────────────────

    @Test
    void testRecordSatisfaction_success() {
        SnippetUsageLog log = SnippetUsageLog.builder()
                .id(1L)
                .snippetId(1L)
                .build();

        when(usageLogRepository.findById(1L)).thenReturn(Optional.of(log));
        when(usageLogRepository.save(any(SnippetUsageLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(snippetRepository.findById(1L)).thenReturn(Optional.of(sampleSnippet));
        when(snippetRepository.save(any(EmailSnippet.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean result = snippetService.recordSatisfaction(1L, 5, "Great snippet!");

        assertTrue(result);
        assertEquals(5, log.getSatisfactionRating());
        assertEquals("Great snippet!", log.getUserFeedback());
    }

    @Test
    void testRecordSatisfaction_invalidRating_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> snippetService.recordSatisfaction(1L, 6, "Good"));
    }

    @Test
    void testRecordSatisfaction_usageNotFound() {
        when(usageLogRepository.findById(99L)).thenReturn(Optional.empty());

        boolean result = snippetService.recordSatisfaction(99L, 4, null);

        assertFalse(result);
    }

    // ─── Getter Tests ──────────────────────────────────────────────────

    @Test
    void testGetSnippetById_found() {
        when(snippetRepository.findById(1L)).thenReturn(Optional.of(sampleSnippet));

        EmailSnippet result = snippetService.getSnippetById(1L);

        assertEquals("Quick Thanks", result.getTitle());
    }

    @Test
    void testGetSnippetById_notFound() {
        when(snippetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> snippetService.getSnippetById(99L));
    }

    @Test
    void testGetAllSnippets() {
        when(snippetRepository.findAllByOrderByPinnedDescUsageCountDesc()).thenReturn(List.of(sampleSnippet));

        List<EmailSnippet> results = snippetService.getAllSnippets();

        assertEquals(1, results.size());
    }

    @Test
    void testGetTopSnippets() {
        when(snippetRepository.findTopNByOrderByUsageCountDesc(5)).thenReturn(List.of(sampleSnippet));

        List<EmailSnippet> results = snippetService.getTopSnippets(5);

        assertEquals(1, results.size());
    }

    // ─── Variable Extraction Tests ─────────────────────────────────────

    @Test
    void testEmailSnippet_getVariableNames() {
        List<String> names = sampleSnippet.getVariableNames();

        assertEquals(3, names.size());
        assertTrue(names.contains("name"));
        assertTrue(names.contains("reason"));
        assertTrue(names.contains("sender"));
    }

    @Test
    void testEmailSnippet_getDefaultValues() {
        Map<String, String> defaults = sampleSnippet.getDefaultValues();

        assertEquals("there", defaults.get("name"));
        assertEquals("your help", defaults.get("reason"));
        assertEquals("[Your Name]", defaults.get("sender"));
    }

    @Test
    void testEmailSnippet_getVariableNames_empty() {
        EmailSnippet noVars = EmailSnippet.builder()
                .title("No vars")
                .body("Hello world")
                .build();

        List<String> names = noVars.getVariableNames();
        assertTrue(names.isEmpty());
    }
}
