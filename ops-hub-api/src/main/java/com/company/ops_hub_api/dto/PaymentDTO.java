package com.company.ops_hub_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDTO {
    private Long id;
    private String paymentReference;
    private Long customerId;
    private String customerCode;
    private String customerName;
    private Long userId;
    private String userEmployeeId;
    private String userName;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String upiId;
    private String transactionId;
    private String gatewayTransactionId;
    private String paymentStatus;
    private LocalDateTime paymentDate;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
