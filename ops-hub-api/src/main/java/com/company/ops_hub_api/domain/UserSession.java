package com.company.ops_hub_api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255, name = "token_hash")
    private String tokenHash;

    @Column(length = 50, name = "device_type")
    private String deviceType;

    @Column(length = 200, name = "device_name")
    private String deviceName;

    @Column(length = 100)
    private String browser;

    @Column(length = 45, name = "ip_address")
    private String ipAddress;

    @Column(length = 500, name = "user_agent")
    private String userAgent;

    @Column(nullable = false, name = "login_at")
    private LocalDateTime loginAt;

    @Column(nullable = false, name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(nullable = false, name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean revoked = false;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}
