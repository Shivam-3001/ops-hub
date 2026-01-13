package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.Report;
import com.company.ops_hub_api.domain.ReportExport;
import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.dto.ExportReportRequestDTO;
import com.company.ops_hub_api.dto.ReportExportDTO;
import com.company.ops_hub_api.repository.ReportExportRepository;
import com.company.ops_hub_api.repository.ReportRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Export Service
 * Handles asynchronous report exports
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final ReportRepository reportRepository;
    private final ReportExportRepository exportRepository;
    private final UserRepository userRepository;
    private final ReportService reportService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Value("${app.exports.directory:./exports}")
    private String exportsDirectory;

    /**
     * Request report export (async)
     */
    @Transactional
    public ReportExportDTO requestExport(ExportReportRequestDTO request, HttpServletRequest httpRequest) {
        // Check export permission
        checkExportPermission();
        
        // Get report
        Report report = reportRepository.findById(request.getReportId())
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
        
        // Get current user
        User user = getCurrentUser();
        
        // Create export record
        ReportExport export = new ReportExport();
        export.setReport(report);
        export.setExportFormat(request.getExportFormat().toUpperCase());
        export.setExportStatus("PENDING");
        export.setExportedBy(user);
        export.setExportedAt(LocalDateTime.now());
        
        // Store parameters and filters
        try {
            if (request.getParameters() != null) {
                export.setParametersUsed(objectMapper.writeValueAsString(request.getParameters()));
            }
            if (request.getFilters() != null) {
                export.setFiltersApplied(objectMapper.writeValueAsString(request.getFilters()));
            }
        } catch (Exception e) {
            log.error("Error serializing export parameters", e);
        }
        
        export = exportRepository.save(export);
        
        // Log export request
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("exportId", export.getId());
        exportData.put("reportId", report.getId());
        exportData.put("reportCode", report.getReportCode());
        exportData.put("exportFormat", request.getExportFormat());
        
        auditLogService.logAction("EXPORT_REQUESTED", "REPORT_EXPORT", export.getId(), 
                null, exportData, httpRequest);
        
        // Trigger async export
        exportAsync(export.getId(), request);
        
        return toDTO(export);
    }

    /**
     * Export report asynchronously
     */
    @Async
    @Transactional
    public void exportAsync(Long exportId, ExportReportRequestDTO request) {
        try {
            ReportExport export = exportRepository.findById(exportId)
                    .orElseThrow(() -> new IllegalArgumentException("Export not found"));
            
            // Update status to PROCESSING
            export.setExportStatus("PROCESSING");
            exportRepository.save(export);
            
            log.info("Starting export {} for report {}", exportId, export.getReport().getReportCode());
            
            // Get report data
            Map<String, Object> parameters = request.getParameters();
            Map<String, Object> filters = request.getFilters();
            
            // Get report data (this will apply data-level access control)
            // Note: ReportService will get current user from security context
            com.company.ops_hub_api.dto.ReportDataDTO reportData = reportService.getReportData(
                    export.getReport().getId(), parameters, filters, null);
            
            // Generate export file
            String filePath = generateExportFile(export, reportData);
            
            // Update export record
            export.setExportStatus("COMPLETED");
            export.setCompletedAt(LocalDateTime.now());
            export.setFilePath(filePath);
            export.setFileName(getFileName(export));
            export.setFileSize(getFileSize(filePath));
            export.setRecordCount(reportData.getRecordCount());
            
            exportRepository.save(export);
            
            log.info("Export {} completed successfully. File: {}", exportId, filePath);
            
            // Log export completion
            Map<String, Object> completionData = new HashMap<>();
            completionData.put("exportId", export.getId());
            completionData.put("filePath", filePath);
            completionData.put("recordCount", reportData.getRecordCount());
            completionData.put("fileSize", export.getFileSize());
            
            auditLogService.logAction("EXPORT_COMPLETED", "REPORT_EXPORT", export.getId(), 
                    null, completionData, null);
            
        } catch (Exception e) {
            log.error("Error exporting report {}", exportId, e);
            
            ReportExport export = exportRepository.findById(exportId).orElse(null);
            if (export != null) {
                export.setExportStatus("FAILED");
                export.setCompletedAt(LocalDateTime.now());
                export.setErrorMessage(e.getMessage());
                exportRepository.save(export);
                
                // Log export failure
                auditLogService.logError("EXPORT_FAILED", "REPORT_EXPORT", exportId, 
                        "Error: " + e.getMessage(), null);
            }
        }
    }

    /**
     * Generate export file
     */
    private String generateExportFile(ReportExport export, com.company.ops_hub_api.dto.ReportDataDTO reportData) 
            throws IOException {
        // Ensure exports directory exists
        Path exportsPath = Paths.get(exportsDirectory);
        if (!Files.exists(exportsPath)) {
            Files.createDirectories(exportsPath);
        }
        
        String format = export.getExportFormat().toUpperCase();
        String fileName = generateFileName(export);
        String filePath = exportsPath.resolve(fileName).toString();
        
        switch (format) {
            case "CSV":
                generateCsvFile(filePath, reportData);
                break;
            case "EXCEL":
                generateExcelFile(filePath, reportData);
                break;
            case "JSON":
                generateJsonFile(filePath, reportData);
                break;
            case "PDF":
                generatePdfFile(filePath, reportData);
                break;
            default:
                throw new IllegalArgumentException("Unsupported export format: " + format);
        }
        
        return filePath;
    }

    /**
     * Generate CSV file
     */
    private void generateCsvFile(String filePath, com.company.ops_hub_api.dto.ReportDataDTO reportData) 
            throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            List<Map<String, Object>> data = reportData.getData();
            if (data.isEmpty()) {
                writer.write("No data available\n");
                return;
            }
            
            // Write header
            Set<String> columns = data.get(0).keySet();
            writer.write(String.join(",", columns) + "\n");
            
            // Write data
            for (Map<String, Object> row : data) {
                List<String> values = new ArrayList<>();
                for (String column : columns) {
                    Object value = row.get(column);
                    String strValue = value != null ? value.toString().replace(",", ";") : "";
                    values.add(strValue);
                }
                writer.write(String.join(",", values) + "\n");
            }
        }
    }

    /**
     * Generate Excel file (simplified - use Apache POI in production)
     */
    private void generateExcelFile(String filePath, com.company.ops_hub_api.dto.ReportDataDTO reportData) 
            throws IOException {
        // For now, generate as CSV with .xlsx extension
        // In production, use Apache POI to generate proper Excel files
        generateCsvFile(filePath.replace(".xlsx", ".csv"), reportData);
        log.warn("Excel export generated as CSV. Install Apache POI for proper Excel support.");
    }

    /**
     * Generate JSON file
     */
    private void generateJsonFile(String filePath, com.company.ops_hub_api.dto.ReportDataDTO reportData) 
            throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(filePath), reportData);
    }

    /**
     * Generate PDF file (simplified - use iText or Apache PDFBox in production)
     */
    private void generatePdfFile(String filePath, com.company.ops_hub_api.dto.ReportDataDTO reportData) 
            throws IOException {
        // For now, generate as text file
        // In production, use iText or Apache PDFBox to generate proper PDF files
        try (FileWriter writer = new FileWriter(filePath.replace(".pdf", ".txt"))) {
            writer.write("Report: " + reportData.getReportName() + "\n");
            writer.write("Generated: " + new Date() + "\n");
            writer.write("Record Count: " + reportData.getRecordCount() + "\n\n");
            writer.write("Data:\n");
            writer.write(objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(reportData.getData()));
        }
        log.warn("PDF export generated as text. Install iText or Apache PDFBox for proper PDF support.");
    }

    /**
     * Generate file name
     */
    private String generateFileName(ReportExport export) {
        String timestamp = LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String reportCode = export.getReport().getReportCode();
        String format = export.getExportFormat().toLowerCase();
        return String.format("%s_%s.%s", reportCode, timestamp, format);
    }

    private String getFileName(ReportExport export) {
        return generateFileName(export);
    }

    private Long getFileSize(String filePath) {
        try {
            File file = new File(filePath);
            return file.exists() ? file.length() : 0L;
        } catch (Exception e) {
            log.error("Error getting file size", e);
            return 0L;
        }
    }

    /**
     * Get export by ID
     */
    @Transactional(readOnly = true)
    public ReportExportDTO getExport(Long exportId) {
        User user = getCurrentUser();
        ReportExport export = exportRepository.findByIdAndExportedById(exportId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Export not found"));
        return toDTO(export);
    }

    /**
     * Get user's exports
     */
    @Transactional(readOnly = true)
    public List<ReportExportDTO> getUserExports() {
        User user = getCurrentUser();
        List<ReportExport> exports = exportRepository.findByUserId(user.getId());
        return exports.stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Get exports for a report
     */
    @Transactional(readOnly = true)
    public List<ReportExportDTO> getReportExports(Long reportId) {
        checkViewReportsPermission();
        List<ReportExport> exports = exportRepository.findByReportId(reportId);
        return exports.stream()
                .map(this::toDTO)
                .toList();
    }

    private void checkExportPermission() {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        if (userPrincipal == null) {
            throw new AccessDeniedException("User not authenticated");
        }
        if (!userPrincipal.hasPermission("EXPORT_REPORTS")) {
            throw new AccessDeniedException("Insufficient permission to export reports");
        }
    }

    private void checkViewReportsPermission() {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        if (userPrincipal == null) {
            throw new AccessDeniedException("User not authenticated");
        }
        if (!userPrincipal.hasPermission("VIEW_REPORTS")) {
            throw new AccessDeniedException("Insufficient permission to view reports");
        }
    }

    private UserPrincipal getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) authentication.getPrincipal();
        }
        return null;
    }

    private User getCurrentUser() {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        if (userPrincipal == null) {
            throw new AccessDeniedException("User not authenticated");
        }
        Long userId = userPrincipal.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private ReportExportDTO toDTO(ReportExport export) {
        String downloadUrl = export.getFilePath() != null 
                ? "/api/reports/exports/" + export.getId() + "/download" 
                : null;
        
        return ReportExportDTO.builder()
                .id(export.getId())
                .reportId(export.getReport().getId())
                .reportCode(export.getReport().getReportCode())
                .reportName(export.getReport().getName())
                .exportFormat(export.getExportFormat())
                .filePath(export.getFilePath())
                .fileName(export.getFileName())
                .fileSize(export.getFileSize())
                .recordCount(export.getRecordCount())
                .exportStatus(export.getExportStatus())
                .exportedBy(export.getExportedBy().getEmployeeId())
                .exportedAt(export.getExportedAt())
                .completedAt(export.getCompletedAt())
                .errorMessage(export.getErrorMessage())
                .downloadUrl(downloadUrl)
                .build();
    }
}
