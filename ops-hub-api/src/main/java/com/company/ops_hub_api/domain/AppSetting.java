package com.company.ops_hub_api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_settings")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200, name = "setting_key")
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "NVARCHAR(MAX)")
    private String settingValue;

    @Column(nullable = false, length = 50, name = "setting_type")
    private String settingType; // STRING, NUMBER, BOOLEAN, JSON

    @Column(length = 100)
    private String category;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, name = "is_encrypted")
    private Boolean isEncrypted = false;

    @Column(nullable = false, name = "is_public")
    private Boolean isPublic = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @CreatedDate
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;
}
