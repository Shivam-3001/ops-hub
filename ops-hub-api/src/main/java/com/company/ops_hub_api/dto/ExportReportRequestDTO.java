package com.company.ops_hub_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Export Report Request DTO
 * Request to export a report
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportReportRequestDTO {
    
    @NotNull(message = "Report ID is required")
    private Long reportId;
    
    @NotBlank(message = "Export format is required")
    private String exportFormat; // CSV, EXCEL, PDF, JSON
    
    private Map<String, Object> parameters;
    
    private Map<String, Object> filters;
}
