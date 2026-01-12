package com.company.ops_hub_api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_exports")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportExport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @Column(nullable = false, length = 50, name = "export_format")
    private String exportFormat; // CSV, EXCEL, PDF, JSON

    @Column(length = 1000, name = "file_path")
    private String filePath;

    @Column(length = 500, name = "file_name")
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "parameters_used", columnDefinition = "NVARCHAR(MAX)")
    private String parametersUsed; // JSON

    @Column(name = "filters_applied", columnDefinition = "NVARCHAR(MAX)")
    private String filtersApplied; // JSON

    @Column(name = "record_count")
    private Integer recordCount;

    @Column(nullable = false, length = 50, name = "export_status")
    private String exportStatus = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exported_by", nullable = false)
    private User exportedBy;

    @CreatedDate
    @Column(nullable = false, name = "exported_at")
    private LocalDateTime exportedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(length = 1000, name = "error_message")
    private String errorMessage;
}
