package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.Report;
import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.dto.ReportDTO;
import com.company.ops_hub_api.dto.ReportDataDTO;
import com.company.ops_hub_api.repository.ReportRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.repository.UserRoleRepository;
import com.company.ops_hub_api.security.UserPrincipal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Report Service
 * Handles report viewing with permission checks and data-level access control
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final ReportDataFilter dataFilter;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    /**
     * Get all available reports for current user
     */
    @Transactional(readOnly = true)
    public List<ReportDTO> getAvailableReports() {
        checkViewReportsPermission();
        
        User user = getCurrentUser();
        List<String> userRoles = getUserRoles(user);
        
        // Get all active reports
        List<Report> reports = reportRepository.findAllActive();
        
        // Filter by access roles
        return reports.stream()
                .filter(report -> canAccessReport(report, userRoles))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get report by ID
     */
    @Transactional(readOnly = true)
    public ReportDTO getReport(Long reportId) {
        checkViewReportsPermission();
        if (reportId == null) {
            throw new IllegalArgumentException("Report ID cannot be null");
        }
        
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
        
        User user = getCurrentUser();
        List<String> userRoles = getUserRoles(user);
        
        if (!canAccessReport(report, userRoles)) {
            throw new AccessDeniedException("Access denied to this report");
        }
        
        return toDTO(report);
    }

    /**
     * Get report data
     */
    @Transactional(readOnly = true)
    public ReportDataDTO getReportData(Long reportId, Map<String, Object> parameters, 
                                      Map<String, Object> filters, HttpServletRequest request) {
        checkViewReportsPermission();
        if (reportId == null) {
            throw new IllegalArgumentException("Report ID cannot be null");
        }
        
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
        
        User user = getCurrentUser();
        List<String> userRoles = getUserRoles(user);
        
        if (!canAccessReport(report, userRoles)) {
            throw new AccessDeniedException("Access denied to this report");
        }
        
        // Execute report query with data-level access control
        List<Map<String, Object>> data = executeReportQuery(report, parameters, filters, user);
        
        // Log report access
        Map<String, Object> accessData = new HashMap<>();
        accessData.put("reportId", reportId);
        accessData.put("reportCode", report.getReportCode());
        accessData.put("parameters", parameters);
        accessData.put("filters", filters);
        accessData.put("recordCount", data.size());
        
        auditLogService.logAction("VIEW_REPORT", "REPORT", reportId, null, accessData, request);
        
        return ReportDataDTO.builder()
                .reportId(report.getId())
                .reportCode(report.getReportCode())
                .reportName(report.getName())
                .data(data)
                .recordCount(data.size())
                .metadata(buildMetadata(report))
                .filtersApplied(filters)
                .build();
    }

    /**
     * Execute report query with data-level access control
     */
    private List<Map<String, Object>> executeReportQuery(Report report, Map<String, Object> parameters,
                                                         Map<String, Object> filters, User user) {
        try {
            String querySql = report.getQuerySql();
            if (querySql == null || querySql.trim().isEmpty()) {
                log.warn("Report {} has no query SQL", report.getReportCode());
                return new ArrayList<>();
            }
            
            // Apply data-level access control
            String accessControlClause = dataFilter.buildAccessControlWhereClause(user, "c");
            if (!accessControlClause.isEmpty()) {
                // Add access control to query
                querySql = injectAccessControl(querySql, accessControlClause);
            }
            
            // Replace parameters in query
            querySql = replaceParameters(querySql, parameters);
            
            // Execute query
            Query query = entityManager.createNativeQuery(querySql);
            
            // Apply filters if any
            applyFilters(query, filters, report);
            
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();
            
            // Convert to Map list
            List<Map<String, Object>> data = convertResultsToMap(results, report);
            
            // Apply post-query filtering if needed
            if (filters != null && !filters.isEmpty()) {
                data = applyPostQueryFilters(data, filters);
            }
            
            // Apply data-level filter (for in-memory filtering)
            data = dataFilter.filterReportData(data, user, "customerId");
            
            return data;
            
        } catch (Exception e) {
            log.error("Error executing report query for report {}", report.getReportCode(), e);
            throw new RuntimeException("Failed to execute report query: " + e.getMessage(), e);
        }
    }

    /**
     * Inject access control clause into SQL query
     */
    private String injectAccessControl(String querySql, String accessControlClause) {
        // Simple injection: add to WHERE clause
        // In production, use proper SQL parser
        String upperQuery = querySql.toUpperCase();
        if (upperQuery.contains("WHERE")) {
            return querySql + accessControlClause;
        } else {
            return querySql + " WHERE 1=1" + accessControlClause;
        }
    }

    /**
     * Replace parameters in query
     */
    private String replaceParameters(String querySql, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return querySql;
        }
        
        String result = querySql;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String placeholder = ":" + entry.getKey();
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    /**
     * Apply filters to query
     */
    private void applyFilters(Query query, Map<String, Object> filters, Report report) {
        List<String> filterKeys = resolveFilterKeys(report);
        if (filterKeys.isEmpty()) {
            if (filters == null) {
                return;
            }
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                if (entry.getValue() != null) {
                    query.setParameter(entry.getKey(), entry.getValue());
                }
            }
            return;
        }

        Map<String, Object> safeFilters = filters == null ? new HashMap<>() : filters;
        for (String key : filterKeys) {
            query.setParameter(key, safeFilters.get(key));
        }
    }

    /**
     * Apply post-query filters
     */
    private List<Map<String, Object>> applyPostQueryFilters(List<Map<String, Object>> data, 
                                                           Map<String, Object> filters) {
        // Apply in-memory filters for complex filtering
        return data.stream()
                .filter(record -> {
                    for (Map.Entry<String, Object> entry : filters.entrySet()) {
                        Object recordValue = record.get(entry.getKey());
                        Object filterValue = entry.getValue();
                        if (filterValue != null && !filterValue.equals(recordValue)) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Convert query results to Map list
     */
    private List<Map<String, Object>> convertResultsToMap(List<Object[]> results, Report report) {
        if (results.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> columns = resolveReportColumns(report);
        List<Map<String, Object>> data = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> record = new HashMap<>();
            for (int i = 0; i < row.length; i++) {
                String key = (columns != null && i < columns.size()) ? columns.get(i) : "column" + i;
                record.put(key, row[i]);
            }
            data.add(record);
        }
        return data;
    }

    private List<String> resolveReportColumns(Report report) {
        if (report == null || report.getParameters() == null || report.getParameters().isEmpty()) {
            return null;
        }
        try {
            Map<String, Object> params = objectMapper.readValue(
                    report.getParameters(),
                    new TypeReference<Map<String, Object>>() {});
            Object columnsObj = params.get("columns");
            if (columnsObj instanceof List<?> list) {
                List<String> columns = new ArrayList<>();
                for (Object item : list) {
                    if (item != null) {
                        columns.add(item.toString());
                    }
                }
                return columns.isEmpty() ? null : columns;
            }
        } catch (Exception e) {
            log.warn("Failed to parse report columns for {}", report.getReportCode(), e);
        }
        return null;
    }

    private List<String> resolveFilterKeys(Report report) {
        if (report == null || report.getParameters() == null || report.getParameters().isEmpty()) {
            return List.of();
        }
        try {
            Map<String, Object> params = objectMapper.readValue(
                    report.getParameters(),
                    new TypeReference<Map<String, Object>>() {});
            Object filtersObj = params.get("filters");
            if (filtersObj instanceof List<?> list) {
                List<String> keys = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> filter) {
                        Object key = filter.get("key");
                        if (key != null) {
                            keys.add(key.toString());
                        }
                    }
                }
                return keys;
            }
        } catch (Exception e) {
            log.warn("Failed to parse report filters for {}", report.getReportCode(), e);
        }
        return List.of();
    }

    /**
     * Build metadata for report
     */
    private Map<String, Object> buildMetadata(Report report) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reportType", report.getReportType());
        metadata.put("category", report.getCategory());
        metadata.put("generatedAt", new Date());
        return metadata;
    }

    /**
     * Check if user can access report based on access roles
     */
    private boolean canAccessReport(Report report, List<String> userRoles) {
        if (report.getAccessRoles() == null || report.getAccessRoles().isEmpty()) {
            return true; // No restriction
        }
        
        try {
            List<String> accessRoles = objectMapper.readValue(
                    report.getAccessRoles(), 
                    new TypeReference<List<String>>() {});
            
            // Check if user has any of the required roles
            return userRoles.stream().anyMatch(accessRoles::contains);
        } catch (Exception e) {
            log.error("Error parsing access roles for report {}", report.getReportCode(), e);
            return false;
        }
    }

    /**
     * Convert Report to DTO
     */
    private ReportDTO toDTO(Report report) {
        try {
            Map<String, Object> parameters = null;
            if (report.getParameters() != null && !report.getParameters().isEmpty()) {
                parameters = objectMapper.readValue(
                        report.getParameters(), 
                        new TypeReference<Map<String, Object>>() {});
            }
            
            List<String> accessRoles = null;
            if (report.getAccessRoles() != null && !report.getAccessRoles().isEmpty()) {
                accessRoles = objectMapper.readValue(
                        report.getAccessRoles(), 
                        new TypeReference<List<String>>() {});
            }
            
            return ReportDTO.builder()
                    .id(report.getId())
                    .reportCode(report.getReportCode())
                    .name(report.getName())
                    .description(report.getDescription())
                    .reportType(report.getReportType())
                    .category(report.getCategory())
                    .parameters(parameters)
                    .accessRoles(accessRoles)
                    .active(report.getActive())
                    .createdBy(report.getCreatedBy() != null ? report.getCreatedBy().getEmployeeId() : null)
                    .createdAt(report.getCreatedAt())
                    .updatedAt(report.getUpdatedAt())
                    .build();
        } catch (Exception e) {
            log.error("Error converting report to DTO", e);
            throw new RuntimeException("Error processing report", e);
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

    private List<String> getUserRoles(User user) {
        // Get roles from UserPrincipal if available, otherwise query
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        if (userPrincipal != null) {
            return new ArrayList<>(userPrincipal.getRoles());
        }
        // Fallback: query roles
        return userRoleRepository.findRoleCodesByUserId(user.getId());
    }
}
