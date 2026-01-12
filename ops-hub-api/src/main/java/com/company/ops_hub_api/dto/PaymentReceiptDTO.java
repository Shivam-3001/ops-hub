package com.company.ops_hub_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Receipt DTO
 * Integration-ready for receipt generation (PDF, email, etc.)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReceiptDTO {
    private String receiptNumber;
    private String paymentReference;
    private LocalDateTime receiptDate;
    private LocalDateTime paymentDate;
    
    // Customer Information
    private String customerCode;
    private String customerName;
    private String customerAddress;
    private String customerPhone;
    
    // Payment Information
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String transactionId;
    private String gatewayTransactionId;
    
    // Status
    private String paymentStatus;
    
    // Additional Information
    private String collectedBy;
    private String collectedByEmployeeId;
    private String notes;
    
    // Receipt Generation Metadata
    private String receiptFormat; // PDF, HTML, JSON
    private String receiptUrl; // URL to generated receipt
}
