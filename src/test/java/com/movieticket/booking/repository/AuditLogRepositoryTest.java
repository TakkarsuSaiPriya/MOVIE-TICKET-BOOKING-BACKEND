package com.movieticket.booking.repository;

import com.movieticket.booking.model.AuditLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void save_persistsAuditLogWithTimestamp() {
        AuditLog log = AuditLog.builder()
                .username("john").httpMethod("POST").endpoint("/api/seats/lock")
                .requestPayload("{}").responsePayload("{}").statusCode(200)
                .executionTimeMs(15L).build();

        AuditLog saved = auditLogRepository.save(log);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void findAll_returnsSavedLogs() {
        auditLogRepository.save(AuditLog.builder().username("a").httpMethod("GET")
                .endpoint("/x").statusCode(200).executionTimeMs(1L).build());
        assertThat(auditLogRepository.findAll()).isNotEmpty();
    }
}