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
public class CustomerUploadHistoryDTO {
    private Long id;
    private String fileName;
    private Integer totalRows;
    private Integer successfulRows;
    private Integer failedRows;
    private String status;
    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;
    private String errorSummary;
    private String uploadedByName;
    private String uploadedByEmployeeId;
}
