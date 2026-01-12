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
@Table(name = "feature_flags")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200, name = "flag_key")
    private String flagKey;

    @Column(nullable = false, length = 200, name = "flag_name")
    private String flagName;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Boolean enabled = false;

    @Column(length = 500, name = "enabled_for_roles")
    private String enabledForRoles; // JSON array

    @Column(name = "enabled_for_users", columnDefinition = "NVARCHAR(MAX)")
    private String enabledForUsers; // JSON array

    @Column(name = "rollout_percentage")
    @jakarta.validation.constraints.Min(0)
    @jakarta.validation.constraints.Max(100)
    private Integer rolloutPercentage = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreatedDate
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;
}
