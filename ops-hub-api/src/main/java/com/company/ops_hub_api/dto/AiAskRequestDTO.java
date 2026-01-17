package com.company.ops_hub_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiAskRequestDTO {
    @NotBlank(message = "Question is required")
    private String question;

    private String conversationId;
    private String currentPage;
    private String currentModule;
    private Map<String, Object> context;
}
