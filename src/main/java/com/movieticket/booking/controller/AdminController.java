package com.movieticket.booking.controller;

import com.movieticket.booking.dto.response.ApiResponse;
import com.movieticket.booking.model.AuditLog;
import com.movieticket.booking.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * INTERNAL-only endpoints. /api/admin/** restricted to ADMIN/STAFF in SecurityConfig.
 * /api/audit/** restricted to ADMIN only.
 */
@RestController
@RequiredArgsConstructor
public class AdminController {

    private final AuditLogService auditLogService;

    @GetMapping("/api/admin/ping")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.success("Internal access OK", "pong"));
    }

    @GetMapping("/api/audit/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getLogs() {
        List<AuditLog> logs = auditLogService.getAllLogs();
        return ResponseEntity.ok(ApiResponse.success("Audit logs fetched", logs));
    }
}