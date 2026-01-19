package com.company.ops_hub_api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogDTO {
    private Long id;
    private Long userId;
    private String userName;
    private String employeeId;
    private String actionType;
    private String entityType;
    private Long entityId;
    private String oldValues;
    private String newValues;
    private String ipAddress;
    private String requestUrl;
    private String requestMethod;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
}
