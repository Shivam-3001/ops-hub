package com.company.ops_hub_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiAskResponseDTO {
    private String conversationId;
    private String response;
}
