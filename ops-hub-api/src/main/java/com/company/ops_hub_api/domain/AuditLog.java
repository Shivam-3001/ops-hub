package com.company.ops_hub_api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100, name = "action_type")
    private String actionType; // CREATE, UPDATE, DELETE, LOGIN, LOGOUT, EXPORT

    @Column(nullable = false, length = 100, name = "entity_type")
    private String entityType; // USER, CUSTOMER, PAYMENT, etc.

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "old_values", columnDefinition = "NVARCHAR(MAX)")
    private String oldValues; // JSON

    @Column(name = "new_values", columnDefinition = "NVARCHAR(MAX)")
    private String newValues; // JSON

    @Column(length = 45, name = "ip_address")
    private String ipAddress;

    @Column(length = 500, name = "user_agent")
    private String userAgent;

    @Column(length = 1000, name = "request_url")
    private String requestUrl;

    @Column(length = 10, name = "request_method")
    private String requestMethod;

    @Column(length = 50)
    private String status; // SUCCESS, FAILURE

    @Column(length = 2000, name = "error_message")
    private String errorMessage;

    @CreatedDate
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;
}
