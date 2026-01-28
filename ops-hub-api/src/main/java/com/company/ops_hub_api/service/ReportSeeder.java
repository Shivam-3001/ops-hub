package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.Report;
import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.repository.ReportRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ReportSeeder {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Bean
    @Transactional
    public CommandLineRunner seedReports() {
        return args -> {
            seedReport(
                    "CUSTOMER_STATUS",
                    "Customer Status Overview",
                    "Customer status, pending amount, and creation date.",
                    "STANDARD",
                    "CUSTOMER",
                    """
                    SELECT c.id AS customerId,
                           c.customer_code AS customerCode,
                           c.first_name AS firstName,
                           c.last_name AS lastName,
                           c.status AS status,
                           c.pending_amount AS pendingAmount,
                           a.name AS areaName,
                           z.name AS zoneName,
                           ci.name AS circleName,
                           cl.name AS clusterName,
                           c.created_at AS createdAt
                    FROM customers c
                    JOIN areas a ON a.id = c.area_id
                    JOIN zones z ON z.id = a.zone_id
                    JOIN circles ci ON ci.id = z.circle_id
                    JOIN clusters cl ON cl.id = ci.cluster_id
                    WHERE (:status IS NULL OR c.status = :status)
                      AND (:clusterId IS NULL OR cl.id = :clusterId)
                      AND (:circleId IS NULL OR ci.id = :circleId)
                      AND (:zoneId IS NULL OR z.id = :zoneId)
                      AND (:areaId IS NULL OR a.id = :areaId)
                      AND (:fromDate IS NULL OR c.created_at >= :fromDate)
                      AND (:toDate IS NULL OR c.created_at <= :toDate)
                    ORDER BY c.created_at DESC
                    """,
                    buildCustomerStatusParameters()
            );

            seedReport(
                    "ACTIVE_ALLOCATIONS",
                    "Active Allocations",
                    "Current active allocations by assignee.",
                    "STANDARD",
                    "ALLOCATION",
                    """
                    SELECT c.id AS customerId,
                           c.customer_code AS customerCode,
                           c.status AS status,
                           a.allocated_at AS allocatedAt,
                           u.employee_id AS assigneeEmployeeId,
                           u.user_type AS assigneeType,
                           ar.name AS areaName,
                           z.name AS zoneName,
                           ci.name AS circleName,
                           cl.name AS clusterName
                    FROM customers c
                    JOIN customer_allocations a ON a.customer_id = c.id AND a.status = 'ACTIVE'
                    JOIN users u ON u.id = a.user_id
                    JOIN areas ar ON ar.id = c.area_id
                    JOIN zones z ON z.id = ar.zone_id
                    JOIN circles ci ON ci.id = z.circle_id
                    JOIN clusters cl ON cl.id = ci.cluster_id
                    WHERE (:assigneeType IS NULL OR u.user_type = :assigneeType)
                      AND (:clusterId IS NULL OR cl.id = :clusterId)
                      AND (:circleId IS NULL OR ci.id = :circleId)
                      AND (:zoneId IS NULL OR z.id = :zoneId)
                      AND (:areaId IS NULL OR ar.id = :areaId)
                      AND (:fromDate IS NULL OR a.allocated_at >= :fromDate)
                      AND (:toDate IS NULL OR a.allocated_at <= :toDate)
                    ORDER BY a.allocated_at DESC
                    """,
                    buildAllocationParameters()
            );
        };
    }

    private void seedReport(String code, String name, String description, String type, String category,
                            String querySql, Map<String, Object> parameters) {
        Report report = reportRepository.findByReportCode(code).orElseGet(Report::new);
        boolean isNew = report.getId() == null;

        report.setReportCode(code);
        report.setName(name);
        report.setDescription(description);
        report.setReportType(type);
        report.setCategory(category);
        report.setQuerySql(querySql);
        report.setActive(true);
        report.setUpdatedAt(LocalDateTime.now());
        if (isNew) {
            report.setCreatedAt(LocalDateTime.now());
        }

        if (report.getCreatedBy() == null) {
            User createdBy = userRepository.findAll().stream().findFirst().orElse(null);
            report.setCreatedBy(createdBy);
        }

        try {
            report.setParameters(objectMapper.writeValueAsString(parameters));
        } catch (Exception e) {
            log.warn("Failed to serialize report parameters for {}", code, e);
        }

        reportRepository.save(report);
        log.info("{} report {}", isNew ? "Seeded" : "Updated", code);
    }

    private Map<String, Object> buildCustomerStatusParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("columns", List.of(
                "customerId", "customerCode", "firstName", "lastName",
                "status", "pendingAmount", "areaName", "zoneName",
                "circleName", "clusterName", "createdAt"
        ));
        params.put("filters", List.of(
                filter("clusterId", "Cluster", "select", null),
                filter("circleId", "Circle", "select", null),
                filter("zoneId", "Zone", "select", null),
                filter("areaId", "Area", "select", null),
                filter("status", "Status", "select", List.of(
                        "NEW", "ASSIGNED", "VISITED", "PAYMENT_PENDING", "PAID", "CLOSED"
                )),
                filter("fromDate", "From Date", "date", null),
                filter("toDate", "To Date", "date", null)
        ));
        return params;
    }

    private Map<String, Object> buildAllocationParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("columns", List.of(
                "customerId", "customerCode", "status", "allocatedAt",
                "assigneeEmployeeId", "assigneeType", "areaName", "zoneName",
                "circleName", "clusterName"
        ));
        params.put("filters", List.of(
                filter("clusterId", "Cluster", "select", null),
                filter("circleId", "Circle", "select", null),
                filter("zoneId", "Zone", "select", null),
                filter("areaId", "Area", "select", null),
                filter("assigneeType", "Assignee Type", "select", List.of(
                        "CLUSTER_HEAD", "CIRCLE_HEAD", "ZONE_HEAD", "AREA_HEAD", "STORE_HEAD", "AGENT"
                )),
                filter("fromDate", "From Date", "date", null),
                filter("toDate", "To Date", "date", null)
        ));
        return params;
    }

    private Map<String, Object> filter(String key, String label, String type, List<String> options) {
        Map<String, Object> filter = new HashMap<>();
        filter.put("key", key);
        filter.put("label", label);
        filter.put("type", type);
        if (options != null) {
            filter.put("options", options);
        }
        return filter;
    }
}
