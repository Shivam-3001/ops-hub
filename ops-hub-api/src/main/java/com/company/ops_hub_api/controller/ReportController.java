package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.dto.*;
import com.company.ops_hub_api.security.RequiresPermission;
import com.company.ops_hub_api.service.ExportService;
import com.company.ops_hub_api.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Report Controller
 * REST endpoints for reports and exports
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ExportService exportService;

    /**
     * Get all available reports
     */
    @RequiresPermission("VIEW_REPORTS")
    @GetMapping
    public ResponseEntity<List<ReportDTO>> getReports() {
        List<ReportDTO> reports = reportService.getAvailableReports();
        return ResponseEntity.ok(reports);
    }

    /**
     * Get report by ID
     */
    @RequiresPermission("VIEW_REPORTS")
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportDTO> getReport(@PathVariable Long reportId) {
        ReportDTO report = reportService.getReport(reportId);
        return ResponseEntity.ok(report);
    }

    /**
     * Get report data
     */
    @RequiresPermission("VIEW_REPORTS")
    @PostMapping("/{reportId}/data")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ReportDataDTO> getReportData(
            @PathVariable Long reportId,
            @RequestBody(required = false) Map<String, Object> requestBody,
            HttpServletRequest httpRequest) {
        
        Map<String, Object> parameters = null;
        Map<String, Object> filters = null;
        
        if (requestBody != null) {
            Object paramsObj = requestBody.get("parameters");
            if (paramsObj instanceof Map) {
                parameters = (Map<String, Object>) paramsObj;
            }
            Object filtersObj = requestBody.get("filters");
            if (filtersObj instanceof Map) {
                filters = (Map<String, Object>) filtersObj;
            }
        }
        
        ReportDataDTO data = reportService.getReportData(reportId, parameters, filters, httpRequest);
        return ResponseEntity.ok(data);
    }

    /**
     * Request report export
     */
    @RequiresPermission("EXPORT_REPORTS")
    @PostMapping("/exports")
    public ResponseEntity<ReportExportDTO> exportReport(
            @Valid @RequestBody ExportReportRequestDTO request,
            HttpServletRequest httpRequest) {
        ReportExportDTO export = exportService.requestExport(request, httpRequest);
        return ResponseEntity.accepted().body(export);
    }

    /**
     * Get export by ID
     */
    @RequiresPermission("VIEW_REPORTS")
    @GetMapping("/exports/{exportId}")
    public ResponseEntity<ReportExportDTO> getExport(@PathVariable Long exportId) {
        ReportExportDTO export = exportService.getExport(exportId);
        return ResponseEntity.ok(export);
    }

    /**
     * Get user's exports
     */
    @RequiresPermission("VIEW_REPORTS")
    @GetMapping("/exports/my-exports")
    public ResponseEntity<List<ReportExportDTO>> getMyExports() {
        List<ReportExportDTO> exports = exportService.getUserExports();
        return ResponseEntity.ok(exports);
    }

    /**
     * Get exports for a report
     */
    @RequiresPermission("VIEW_REPORTS")
    @GetMapping("/{reportId}/exports")
    public ResponseEntity<List<ReportExportDTO>> getReportExports(@PathVariable Long reportId) {
        List<ReportExportDTO> exports = exportService.getReportExports(reportId);
        return ResponseEntity.ok(exports);
    }

    /**
     * Download export file
     */
    @RequiresPermission("VIEW_REPORTS")
    @GetMapping("/exports/{exportId}/download")
    public ResponseEntity<?> downloadExport(@PathVariable Long exportId) {
        // TODO: Implement file download
        // This would stream the file from the file system
        return ResponseEntity.notFound().build();
    }
}
