package com.company.ops_hub_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiActionDTO {
    private Long id;
    private String actionType;
    private String actionName;
    private String executionStatus;
    private LocalDateTime createdAt;
}
