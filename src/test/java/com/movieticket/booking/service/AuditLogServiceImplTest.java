package com.movieticket.booking.service;

import com.movieticket.booking.model.AuditLog;
import com.movieticket.booking.repository.AuditLogRepository;
import com.movieticket.booking.service.impl.AuditLogServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Test
    void getAllLogs_returnsAllPersistedLogs() {
        AuditLog log = AuditLog.builder().id(1L).username("john").build();
        when(auditLogRepository.findAll()).thenReturn(List.of(log));

        List<AuditLog> result = auditLogService.getAllLogs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("john");
    }

    @Test
    void getAllLogs_returnsEmptyList_whenNoLogs() {
        when(auditLogRepository.findAll()).thenReturn(List.of());
        assertThat(auditLogService.getAllLogs()).isEmpty();
    }
}