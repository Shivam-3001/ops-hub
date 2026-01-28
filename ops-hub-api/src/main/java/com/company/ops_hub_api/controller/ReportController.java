package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.dto.*;
import com.company.ops_hub_api.security.RequiresPermission;
import com.company.ops_hub_api.domain.ReportExport;
import com.company.ops_hub_api.service.ExportService;
import com.company.ops_hub_api.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public ResponseEntity<Resource> downloadExport(@PathVariable Long exportId) {
        ReportExport export = exportService.getExportForDownload(exportId);
        String filePathValue = export.getFilePath();
        if (filePathValue == null || filePathValue.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path filePath = java.util.Objects.requireNonNull(Paths.get(filePathValue));
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new FileSystemResource(filePath);
            String fileName = export.getFileName() != null && !export.getFileName().isBlank()
                    ? export.getFileName()
                    : filePath.getFileName().toString();
            long size = Files.size(filePath);
            MediaType contentType = java.util.Objects.requireNonNull(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .contentLength(size)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
