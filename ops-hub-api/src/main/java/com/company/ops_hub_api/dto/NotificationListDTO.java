package com.company.ops_hub_api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NotificationListDTO {
    private List<NotificationDTO> notifications;
    private long unreadCount;
}
