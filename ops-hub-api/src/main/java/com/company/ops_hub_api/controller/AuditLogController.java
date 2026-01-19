package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.dto.AuditLogDTO;
import com.company.ops_hub_api.security.RequiresPermission;
import com.company.ops_hub_api.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @RequiresPermission("VIEW_AUDIT_LOGS")
    public ResponseEntity<Page<AuditLogDTO>> getAuditLogs(
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLogDTO> logs = auditLogService.searchAuditLogs(actionType, entityType, userId, startTime, endTime, pageable);
        return ResponseEntity.ok(logs);
    }
}
