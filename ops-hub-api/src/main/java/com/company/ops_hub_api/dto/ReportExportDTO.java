package com.company.ops_hub_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Report Export DTO
 * Response DTO for report export information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportExportDTO {
    private Long id;
    private Long reportId;
    private String reportCode;
    private String reportName;
    private String exportFormat;
    private String filePath;
    private String fileName;
    private Long fileSize;
    private Integer recordCount;
    private String exportStatus; // PENDING, PROCESSING, COMPLETED, FAILED
    private String exportedBy;
    private LocalDateTime exportedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private String downloadUrl; // Generated URL for downloading the file
}
