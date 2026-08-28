package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ScheduledEmailLog entity with analytics queries.
 */
@Repository
public interface ScheduledEmailLogRepository extends JpaRepository<ScheduledEmailLog, Long> {

    /**
     * Find all log entries for a specific scheduled email, ordered by attempt number.
     */
    List<ScheduledEmailLog> findByScheduledEmailIdOrderByAttemptNumberAsc(Long scheduledEmailId);

    /**
     * Find the most recent log entry for a scheduled email.
     */
    List<ScheduledEmailLog> findTop1ByScheduledEmailIdOrderByAttemptedAtDesc(Long scheduledEmailId);

    /**
     * Count attempts for a specific scheduled email.
     */
    long countByScheduledEmailId(Long scheduledEmailId);

    /**
     * Count successful deliveries.
     */
    long countByOutcome(String outcome);

    /**
     * Count outcomes grouped by outcome type.
     */
    @Query("SELECT l.outcome, COUNT(l) FROM ScheduledEmailLog l GROUP BY l.outcome")
    List<Object[]> countGroupByOutcome();

    /**
     * Average response time across all attempts.
     */
    @Query("SELECT AVG(l.responseTimeMs) FROM ScheduledEmailLog l WHERE l.responseTimeMs IS NOT NULL")
    Double averageResponseTime();

    /**
     * Average response time for a specific provider.
     */
    @Query("SELECT AVG(l.responseTimeMs) FROM ScheduledEmailLog l WHERE l.providerUsed = :provider AND l.responseTimeMs IS NOT NULL")
    Double averageResponseTimeByProvider(@Param("provider") String provider);

    /**
     * Count attempts by provider.
     */
    @Query("SELECT l.providerUsed, COUNT(l) FROM ScheduledEmailLog l WHERE l.providerUsed IS NOT NULL GROUP BY l.providerUsed")
    List<Object[]> countByProvider();

    /**
     * Find logs with content regeneration.
     */
    List<ScheduledEmailLog> findByContentRegeneratedTrue();

    /**
     * Find error logs by error code.
     */
    List<ScheduledEmailLog> findByErrorCode(String errorCode);

    /**
     * Get the total number of delivery attempts across all emails.
     */
    @Query("SELECT COALESCE(SUM(l.attemptNumber), 0) FROM ScheduledEmailLog l")
    long totalAttemptCount();
}
