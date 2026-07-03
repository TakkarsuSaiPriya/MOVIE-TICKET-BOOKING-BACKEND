package com.movieticket.booking.service.impl;

import com.movieticket.booking.model.AuditLog;
import com.movieticket.booking.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements com.movieticket.booking.service.AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAll();
    }
}