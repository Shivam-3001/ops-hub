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
public class AiConversationSummaryDTO {
    private Long id;
    private String conversationId;
    private String title;
    private String status;
    private String modelName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
