package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.AuditLog;
import com.company.ops_hub_api.repository.AuditLogRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void logAction(String actionType, String entityType, Long entityId, 
                         Map<String, Object> oldValues, Map<String, Object> newValues,
                         HttpServletRequest request) {
        try {
            AuditLog auditLog = new AuditLog();
            
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof com.company.ops_hub_api.security.UserPrincipal) {
                com.company.ops_hub_api.security.UserPrincipal userPrincipal = 
                    (com.company.ops_hub_api.security.UserPrincipal) authentication.getPrincipal();
                Long userId = userPrincipal.getUserId();
                if (userId != null) {
                    userRepository.findById(userId).ifPresent(auditLog::setUser);
                }
            }

            auditLog.setActionType(actionType);
            auditLog.setEntityType(entityType);
            if (entityId != null) {
                auditLog.setEntityId(entityId);
            }
            
            // Convert maps to JSON
            if (oldValues != null && !oldValues.isEmpty()) {
                auditLog.setOldValues(objectMapper.writeValueAsString(oldValues));
            }
            if (newValues != null && !newValues.isEmpty()) {
                auditLog.setNewValues(objectMapper.writeValueAsString(newValues));
            }

            // Extract request information
            if (request != null) {
                auditLog.setIpAddress(getClientIpAddress(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
                auditLog.setRequestUrl(request.getRequestURI());
                auditLog.setRequestMethod(request.getMethod());
            }

            auditLog.setStatus("SUCCESS");
            auditLog.setCreatedAt(LocalDateTime.now());

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Error creating audit log", e);
            // Don't throw exception - audit logging should not break the main flow
        }
    }

    @Transactional
    public void logAction(String actionType, String entityType, Long entityId, 
                         Map<String, Object> oldValues, Map<String, Object> newValues) {
        logAction(actionType, entityType, entityId, oldValues, newValues, null);
    }

    @Transactional
    public void logError(String actionType, String entityType, Long entityId, 
                        String errorMessage, HttpServletRequest request) {
        try {
            AuditLog auditLog = new AuditLog();
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof com.company.ops_hub_api.security.UserPrincipal) {
                com.company.ops_hub_api.security.UserPrincipal userPrincipal = 
                    (com.company.ops_hub_api.security.UserPrincipal) authentication.getPrincipal();
                Long userId = userPrincipal.getUserId();
                if (userId != null) {
                    userRepository.findById(userId).ifPresent(auditLog::setUser);
                }
            }

            auditLog.setActionType(actionType);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setStatus("FAILURE");
            auditLog.setErrorMessage(errorMessage);

            if (request != null) {
                auditLog.setIpAddress(getClientIpAddress(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
                auditLog.setRequestUrl(request.getRequestURI());
                auditLog.setRequestMethod(request.getMethod());
            }

            auditLog.setCreatedAt(LocalDateTime.now());
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Error creating audit log for error", e);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
