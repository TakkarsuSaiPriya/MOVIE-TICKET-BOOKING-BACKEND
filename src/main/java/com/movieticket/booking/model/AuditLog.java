package com.movieticket.booking.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Persists every API request/response for traceability (who did what, when).
 * Populated by AuditAspect around controller methods.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String httpMethod;

    private String endpoint;

    @Lob
    private String requestPayload;

    @Lob
    private String responsePayload;

    private Integer statusCode;

    private Long executionTimeMs;

    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}