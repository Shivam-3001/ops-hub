package com.company.ops_hub_api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_events")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(nullable = false, length = 50, name = "event_type")
    private String eventType; // INITIATED, CALLBACK_RECEIVED, PROCESSED, FAILED

    @Column(name = "event_data", columnDefinition = "NVARCHAR(MAX)")
    private String eventData; // JSON

    @Column(length = 100, name = "gateway_name")
    private String gatewayName;

    @Column(length = 45, name = "ip_address")
    private String ipAddress;

    @Column(length = 500, name = "user_agent")
    private String userAgent;

    @CreatedDate
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;
}
