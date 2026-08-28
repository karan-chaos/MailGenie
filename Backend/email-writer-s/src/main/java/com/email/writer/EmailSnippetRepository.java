package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for EmailSnippet entity with custom search and analytics queries.
 */
@Repository
public interface EmailSnippetRepository extends JpaRepository<EmailSnippet, Long> {

    /**
     * Find snippets by category, ordered by usage count descending.
     */
    List<EmailSnippet> findByCategoryOrderByUsageCountDesc(String category);

    /**
     * Find all snippets ordered by pinned first, then usage count descending.
     */
    List<EmailSnippet> findAllByOrderByPinnedDescUsageCountDesc();

    /**
     * Find a snippet by its keyboard shortcut.
     */
    Optional<EmailSnippet> findByShortcut(String shortcut);

    /**
     * Full-text search on title and body.
     */
    @Query("SELECT s FROM EmailSnippet s WHERE " +
           "LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.body) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY s.usageCount DESC")
    List<EmailSnippet> searchByTitleOrBody(@Param("query") String query);

    /**
     * Find snippets whose required variables contain a given variable name.
     */
    @Query("SELECT s FROM EmailSnippet s WHERE s.requiredVariables LIKE CONCAT('%', :varName, '%')")
    List<EmailSnippet> findByRequiredVariable(@Param("varName") String varName);

    /**
     * Find the top N most-used snippets.
     */
    List<EmailSnippet> findTopNByOrderByUsageCountDesc(int n);

    /**
     * Find snippets updated in the last N days.
     */
    @Query("SELECT s FROM EmailSnippet s WHERE s.updatedAt >= :sinceDate ORDER BY s.updatedAt DESC")
    List<EmailSnippet> findRecentlyUpdated(@Param("sinceDate") java.time.LocalDateTime sinceDate);

    /**
     * Find shared snippets.
     */
    List<EmailSnippet> findBySharedTrueOrderByUsageCountDesc();

    /**
     * Check if a shortcut already exists (excluding a given ID).
     */
    @Query("SELECT COUNT(s) > 0 FROM EmailSnippet s WHERE s.shortcut = :shortcut AND s.id <> :excludeId")
    boolean existsByShortcutExcludingId(@Param("shortcut") String shortcut, @Param("excludeId") Long excludeId);

    /**
     * Find snippets matching multiple categories.
     */
    @Query("SELECT s FROM EmailSnippet s WHERE s.category IN :categories ORDER BY s.usageCount DESC")
    List<EmailSnippet> findByCategories(@Param("categories") List<String> categories);

    /**
     * Count snippets by category.
     */
    @Query("SELECT s.category, COUNT(s) FROM EmailSnippet s WHERE s.category IS NOT NULL GROUP BY s.category")
    List<Object[]> countByCategory();
}
