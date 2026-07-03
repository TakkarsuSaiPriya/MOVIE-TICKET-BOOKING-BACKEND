package com.movieticket.booking.service;

import com.movieticket.booking.model.AuditLog;
import java.util.List;

public interface AuditLogService {
    List<AuditLog> getAllLogs();
}