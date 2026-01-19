package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.AuditLog;
import com.company.ops_hub_api.repository.AuditLogRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.company.ops_hub_api.dto.AuditLogDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        logActionForUser(null, actionType, entityType, entityId, oldValues, newValues, request);
    }

    @Transactional
    public void logAction(String actionType, String entityType, Long entityId, 
                         Map<String, Object> oldValues, Map<String, Object> newValues) {
        logAction(actionType, entityType, entityId, oldValues, newValues, null);
    }

    @Transactional
    public void logActionForUser(Long actorUserId, String actionType, String entityType, Long entityId,
                                 Map<String, Object> oldValues, Map<String, Object> newValues,
                                 HttpServletRequest request) {
        try {
            AuditLog auditLog = new AuditLog();
            setActor(auditLog, actorUserId);
            populateLog(auditLog, actionType, entityType, entityId, oldValues, newValues, request);
            auditLog.setStatus("SUCCESS");
            auditLog.setCreatedAt(LocalDateTime.now());
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Error creating audit log", e);
        }
    }

    @Transactional
    public void logError(String actionType, String entityType, Long entityId, 
                        String errorMessage, HttpServletRequest request) {
        try {
            AuditLog auditLog = new AuditLog();
            setActor(auditLog, null);
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

    @Transactional(readOnly = true)
    public Page<AuditLogDTO> searchAuditLogs(String actionType, String entityType, Long userId,
                                             LocalDateTime startTime, LocalDateTime endTime,
                                             Pageable pageable) {
        return auditLogRepository.searchAuditLogs(actionType, entityType, userId, startTime, endTime, pageable)
                .map(this::toDTO);
    }

    private void setActor(AuditLog auditLog, Long actorUserId) {
        if (actorUserId != null) {
            userRepository.findById(actorUserId).ifPresent(auditLog::setUser);
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.company.ops_hub_api.security.UserPrincipal) {
            com.company.ops_hub_api.security.UserPrincipal userPrincipal =
                (com.company.ops_hub_api.security.UserPrincipal) authentication.getPrincipal();
            Long userId = userPrincipal.getUserId();
            if (userId != null) {
                userRepository.findById(userId).ifPresent(auditLog::setUser);
            }
        }
    }

    private void populateLog(AuditLog auditLog, String actionType, String entityType, Long entityId,
                             Map<String, Object> oldValues, Map<String, Object> newValues,
                             HttpServletRequest request) throws Exception {
        auditLog.setActionType(actionType);
        auditLog.setEntityType(entityType);
        if (entityId != null) {
            auditLog.setEntityId(entityId);
        }
        if (oldValues != null && !oldValues.isEmpty()) {
            auditLog.setOldValues(objectMapper.writeValueAsString(oldValues));
        }
        if (newValues != null && !newValues.isEmpty()) {
            auditLog.setNewValues(objectMapper.writeValueAsString(newValues));
        }
        if (request != null) {
            auditLog.setIpAddress(getClientIpAddress(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));
            auditLog.setRequestUrl(request.getRequestURI());
            auditLog.setRequestMethod(request.getMethod());
        }
    }

    private AuditLogDTO toDTO(AuditLog auditLog) {
        String userName = null;
        String employeeId = null;
        Long userId = null;
        if (auditLog.getUser() != null) {
            userId = auditLog.getUser().getId();
            employeeId = auditLog.getUser().getEmployeeId();
            userName = auditLog.getUser().getFullName() != null
                    ? auditLog.getUser().getFullName()
                    : auditLog.getUser().getUsername();
        }
        return AuditLogDTO.builder()
                .id(auditLog.getId())
                .userId(userId)
                .employeeId(employeeId)
                .userName(userName)
                .actionType(auditLog.getActionType())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .oldValues(auditLog.getOldValues())
                .newValues(auditLog.getNewValues())
                .ipAddress(auditLog.getIpAddress())
                .requestUrl(auditLog.getRequestUrl())
                .requestMethod(auditLog.getRequestMethod())
                .status(auditLog.getStatus())
                .errorMessage(auditLog.getErrorMessage())
                .createdAt(auditLog.getCreatedAt())
                .build();
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
