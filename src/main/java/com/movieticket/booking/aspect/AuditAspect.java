package com.movieticket.booking.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movieticket.booking.model.AuditLog;
import com.movieticket.booking.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Intercepts every controller method, persisting an AuditLog row with
 * username, endpoint, request args, response body, status, and execution time.
 * This satisfies the "audit logs for request/response saved to DB" requirement
 * without polluting controller code.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Around("execution(* com.movieticket.booking.controller..*(..))")
    public Object auditRequestResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        String username = getCurrentUsername();
        String endpoint = "";
        String httpMethod = "";

        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            endpoint = request.getRequestURI();
            httpMethod = request.getMethod();
        }

        String requestPayload = safeSerialize(joinPoint.getArgs());
        int statusCode = 200;
        Object result = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            statusCode = 500;
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - start;
            AuditLog log = AuditLog.builder()
                    .username(username)
                    .httpMethod(httpMethod)
                    .endpoint(endpoint)
                    .requestPayload(requestPayload)
                    .responsePayload(safeSerialize(result))
                    .statusCode(statusCode)
                    .executionTimeMs(duration)
                    .build();
            try {
                auditLogRepository.save(log);
            } catch (Exception ignored) {
                // never let audit logging break the main request flow
            }
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (authentication != null && authentication.isAuthenticated())
                ? authentication.getName()
                : "anonymous";
    }

    private String safeSerialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "unserializable";
        }
    }
}