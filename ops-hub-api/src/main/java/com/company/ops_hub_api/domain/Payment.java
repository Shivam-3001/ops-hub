package com.company.ops_hub_api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100, name = "payment_reference")
    private String paymentReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(nullable = false, length = 50, name = "payment_method")
    private String paymentMethod; // UPI, CASH, CARD, BANK_TRANSFER

    @Column(length = 200, name = "upi_id")
    private String upiId;

    @Column(length = 200, name = "transaction_id")
    private String transactionId;

    @Column(length = 200, name = "gateway_transaction_id")
    private String gatewayTransactionId;

    @Column(nullable = false, length = 50, name = "payment_status")
    private String paymentStatus = "INITIATED"; // INITIATED, SUCCESS, FAILED, REFUNDED

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(length = 500, name = "failure_reason")
    private String failureReason;

    @Column(name = "gateway_response", columnDefinition = "NVARCHAR(MAX)")
    private String gatewayResponse;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PaymentEvent> events;

    @CreatedDate
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;
}
