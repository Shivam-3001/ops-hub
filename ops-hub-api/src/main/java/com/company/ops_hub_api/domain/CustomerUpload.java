package com.company.ops_hub_api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "customer_uploads")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500, name = "file_name")
    private String fileName;

    @Column(length = 1000, name = "file_path")
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(nullable = false, name = "total_rows")
    private Integer totalRows = 0;

    @Column(nullable = false, name = "successful_rows")
    private Integer successfulRows = 0;

    @Column(nullable = false, name = "failed_rows")
    private Integer failedRows = 0;

    @Column(nullable = false, length = 50, name = "upload_status")
    private String uploadStatus = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @CreatedDate
    @Column(nullable = false, name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_summary", columnDefinition = "NVARCHAR(MAX)")
    private String errorSummary;

    @OneToMany(mappedBy = "upload", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CustomerUploadError> errors;
}
