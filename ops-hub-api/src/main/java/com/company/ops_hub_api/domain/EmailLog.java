package com.company.ops_hub_api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private EmailTemplate template;

    @Column(length = 100, name = "template_code")
    private String templateCode;

    @Column(nullable = false, length = 500, name = "recipient_email_encrypted")
    private String recipientEmailEncrypted;

    @Column(length = 200, name = "recipient_name")
    private String recipientName;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(name = "body_html", columnDefinition = "NVARCHAR(MAX)")
    private String bodyHtml;

    @Column(name = "body_text", columnDefinition = "NVARCHAR(MAX)")
    private String bodyText;

    @Column(nullable = false, length = 50, name = "email_status")
    private String emailStatus = "PENDING"; // PENDING, SENT, DELIVERED, FAILED, BOUNCED

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(length = 1000, name = "failure_reason")
    private String failureReason;

    @Column(length = 200, name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "provider_response", columnDefinition = "NVARCHAR(MAX)")
    private String providerResponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_by")
    private User sentBy;

    @CreatedDate
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;
}
