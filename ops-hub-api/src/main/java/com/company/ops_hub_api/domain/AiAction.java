package com.company.ops_hub_api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_actions")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private AiConversation conversation;

    @Column(nullable = false, length = 100, name = "action_type")
    private String actionType; // DATA_QUERY, REPORT_GENERATION, RECOMMENDATION

    @Column(nullable = false, length = 200, name = "action_name")
    private String actionName;

    @Column(name = "action_data", columnDefinition = "NVARCHAR(MAX)")
    private String actionData; // JSON

    @Column(nullable = false, length = 50, name = "execution_status")
    private String executionStatus = "PENDING"; // PENDING, EXECUTING, COMPLETED, FAILED

    @Column(name = "result_data", columnDefinition = "NVARCHAR(MAX)")
    private String resultData; // JSON

    @Column(length = 2000, name = "error_message")
    private String errorMessage;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreatedDate
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;
}
