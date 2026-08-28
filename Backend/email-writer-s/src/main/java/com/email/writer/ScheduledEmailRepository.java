package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ScheduledEmail entity with queue and analytics queries.
 */
@Repository
public interface ScheduledEmailRepository extends JpaRepository<ScheduledEmail, Long> {

    /**
     * Find all pending emails scheduled on or before a given time, ordered by priority then schedule time.
     */
    List<ScheduledEmail> findByStatusInAndScheduledForBeforeOrderByPriorityAscScheduledForAsc(
            List<String> statuses, LocalDateTime cutoff);

    /**
     * Find emails eligible for retry (FAILED/RETRYING with nextRetryAt in the past).
     */
    @Query("SELECT e FROM ScheduledEmail e WHERE e.status IN ('FAILED','RETRYING') " +
           "AND e.attemptCount < e.maxRetries AND e.nextRetryAt IS NOT NULL AND e.nextRetryAt <= :now " +
           "ORDER BY e.priority ASC, e.nextRetryAt ASC")
    List<ScheduledEmail> findRetryableEmails(@Param("now") LocalDateTime now);

    /**
     * Find emails by status.
     */
    List<ScheduledEmail> findByStatusOrderByScheduledForDesc(String status);

    /**
     * Find emails by recipient.
     */
    List<ScheduledEmail> findByRecipientEmailOrderByScheduledForDesc(String email);

    /**
     * Find emails by batch ID.
     */
    List<ScheduledEmail> findByBatchIdOrderByPriorityAscScheduledForAsc(String batchId);

    /**
     * Find overdue emails (pending but past scheduled time).
     */
    @Query("SELECT e FROM ScheduledEmail e WHERE e.status = 'PENDING' " +
           "AND e.scheduledFor < :now ORDER BY e.scheduledFor ASC")
    List<ScheduledEmail> findOverdueEmails(@Param("now") LocalDateTime now);

    /**
     * Count emails by status.
     */
    long countByStatus(String status);

    /**
     * Count emails scheduled between two dates.
     */
    long countByScheduledForBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find emails scheduled in the future, ordered by scheduled time.
     */
    List<ScheduledEmail> findByScheduledForAfterOrderByScheduledForAsc(LocalDateTime since);

    /**
     * Find upcoming emails for a recipient.
     */
    List<ScheduledEmail> findByRecipientEmailAndStatusInAndScheduledForAfterOrderByScheduledForAsc(
            String email, List<String> statuses, LocalDateTime since);

    /**
     * Find all emails in a given status with a limit.
     */
    @Query("SELECT e FROM ScheduledEmail e WHERE e.status = :status ORDER BY e.scheduledFor ASC")
    List<ScheduledEmail> findTopByStatus(@Param("status") String status, org.springframework.data.domain.Pageable pageable);

    /**
     * Find recurring emails that are due for next occurrence.
     */
    @Query("SELECT e FROM ScheduledEmail e WHERE e.recurring = true AND e.status = 'DELIVERED' " +
           "AND e.recurrencePattern IS NOT NULL")
    List<ScheduledEmail> findRecurringEmails();

    /**
     * Get all unique batch IDs.
     */
    @Query("SELECT DISTINCT e.batchId FROM ScheduledEmail e WHERE e.batchId IS NOT NULL")
    List<String> findAllBatchIds();
}
