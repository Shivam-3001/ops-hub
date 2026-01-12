package com.company.ops_hub_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackDTO {
    
    @NotBlank(message = "Payment reference is required")
    private String paymentReference;
    
    @NotBlank(message = "Gateway transaction ID is required")
    private String gatewayTransactionId;
    
    @NotBlank(message = "Status is required")
    private String status; // SUCCESS, FAILED
    
    private String gatewayName;
    private String failureReason;
    private String gatewayResponse; // Full gateway response as JSON string
    private String signature; // For signature verification
}
