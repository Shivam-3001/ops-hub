package com.company.ops_hub_api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DashboardActivityDTO {
    private String title;
    private String description;
    private String type;
    private LocalDateTime createdAt;
}
