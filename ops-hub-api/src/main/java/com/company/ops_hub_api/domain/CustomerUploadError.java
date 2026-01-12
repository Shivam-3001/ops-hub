package com.company.ops_hub_api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_upload_errors")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerUploadError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_id", nullable = false)
    private CustomerUpload upload;

    @Column(nullable = false, name = "row_number")
    private Integer rowNumber;

    @Column(length = 100, name = "column_name")
    private String columnName;

    @Column(length = 50, name = "error_code")
    private String errorCode;

    @Column(nullable = false, length = 1000, name = "error_message")
    private String errorMessage;

    @Column(name = "row_data", columnDefinition = "NVARCHAR(MAX)")
    private String rowData;

    @CreatedDate
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;
}
