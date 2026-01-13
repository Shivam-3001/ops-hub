package com.company.ops_hub_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Report Data DTO
 * Response DTO for report data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDataDTO {
    private Long reportId;
    private String reportCode;
    private String reportName;
    private List<Map<String, Object>> data;
    private Integer recordCount;
    private Map<String, Object> metadata;
    private Map<String, Object> filtersApplied;
}
