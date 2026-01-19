package com.company.ops_hub_api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationDTO {
    private Long id;
    private String type;
    private String title;
    private String message;
    private String entityType;
    private Long entityId;
    private String severity;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
