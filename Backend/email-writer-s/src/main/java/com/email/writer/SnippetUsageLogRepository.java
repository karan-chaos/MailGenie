package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for SnippetUsageLog entity with analytics aggregation queries.
 */
@Repository
public interface SnippetUsageLogRepository extends JpaRepository<SnippetUsageLog, Long> {

    /**
     * Find all usage logs for a specific snippet, ordered by most recent.
     */
    List<SnippetUsageLog> findBySnippetIdOrderByUsedAtDesc(Long snippetId);

    /**
     * Find usage logs within a time range.
     */
    List<SnippetUsageLog> findByUsedAtBetweenOrderByUsedAtDesc(LocalDateTime start, LocalDateTime end);

    /**
     * Count total uses for a given snippet.
     */
    long countBySnippetId(Long snippetId);

    /**
     * Count successful uses for a given snippet.
     */
    long countBySnippetIdAndStatus(Long snippetId, String status);

    /**
     * Find the most recently used snippet IDs.
     */
    @Query("SELECT l.snippetId FROM SnippetUsageLog l GROUP BY l.snippetId ORDER BY MAX(l.usedAt) DESC")
    List<Long> findMostRecentlyUsedSnippetIds();

    /**
     * Calculate average duration for a snippet.
     */
    @Query("SELECT AVG(l.durationMs) FROM SnippetUsageLog l WHERE l.snippetId = :snippetId AND l.durationMs IS NOT NULL")
    Double averageDurationBySnippetId(@Param("snippetId") Long snippetId);

    /**
     * Calculate average satisfaction for a snippet.
     */
    @Query("SELECT AVG(l.satisfactionRating) FROM SnippetUsageLog l WHERE l.snippetId = :snippetId AND l.satisfactionRating IS NOT NULL")
    Double averageSatisfactionBySnippetId(@Param("snippetId") Long snippetId);

    /**
     * Count usages grouped by trigger source.
     */
    @Query("SELECT l.triggerSource, COUNT(l) FROM SnippetUsageLog l WHERE l.triggerSource IS NOT NULL GROUP BY l.triggerSource")
    List<Object[]> countByTriggerSource();

    /**
     * Count usages grouped by provider.
     */
    @Query("SELECT l.provider, COUNT(l) FROM SnippetUsageLog l WHERE l.provider IS NOT NULL GROUP BY l.provider")
    List<Object[]> countByProvider();

    /**
     * Count total usage logs in a given day.
     */
    @Query("SELECT COUNT(l) FROM SnippetUsageLog l WHERE l.usedAt >= :startOfDay AND l.usedAt < :endOfDay")
    long countByDay(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * Find usage logs that have not been rated yet.
     */
    List<SnippetUsageLog> findBySnippetIdAndSatisfactionRatingIsNull(Long snippetId);

    /**
     * Get the average character count of expanded content.
     */
    @Query("SELECT AVG(LENGTH(l.expandedContent)) FROM SnippetUsageLog l WHERE l.snippetId = :snippetId")
    Double averageContentLengthBySnippetId(@Param("snippetId") Long snippetId);

    /**
     * Find usage logs by provider within a date range.
     */
    List<SnippetUsageLog> findByProviderAndUsedAtBetweenOrderByUsedAtDesc(
            String provider, LocalDateTime start, LocalDateTime end);
}
