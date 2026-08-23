package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for managing Enterprise Telemetry Logs
 */
@Repository
public interface TelemetryLogRepository extends JpaRepository<TelemetryLog, Long> {
}
