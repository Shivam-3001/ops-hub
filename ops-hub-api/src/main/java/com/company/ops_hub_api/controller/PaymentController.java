package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.domain.Payment;
import com.company.ops_hub_api.dto.InitiatePaymentDTO;
import com.company.ops_hub_api.dto.PaymentCallbackDTO;
import com.company.ops_hub_api.dto.PaymentDTO;
import com.company.ops_hub_api.dto.PaymentReceiptDTO;
import com.company.ops_hub_api.security.RequiresPermission;
import com.company.ops_hub_api.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Initiate a payment (requires COLLECT_PAYMENT permission)
     */
    @PostMapping
    @RequiresPermission("COLLECT_PAYMENT")
    public ResponseEntity<PaymentDTO> initiatePayment(
            @Valid @RequestBody InitiatePaymentDTO dto,
            HttpServletRequest httpRequest) {
        Payment payment = paymentService.initiatePayment(dto, httpRequest);
        return ResponseEntity.ok(toDTO(payment));
    }

    /**
     * Handle payment gateway callback
     * This endpoint should be publicly accessible (configured in SecurityConfig)
     */
    @PostMapping("/callback")
    public ResponseEntity<PaymentDTO> handleCallback(
            @Valid @RequestBody PaymentCallbackDTO dto,
            HttpServletRequest httpRequest) {
        Payment payment = paymentService.handleCallback(dto, httpRequest);
        return ResponseEntity.ok(toDTO(payment));
    }

    /**
     * Get payment by reference
     */
    @GetMapping("/reference/{paymentReference}")
    @RequiresPermission("VIEW_PAYMENTS")
    public ResponseEntity<PaymentDTO> getPaymentByReference(@PathVariable String paymentReference) {
        Payment payment = paymentService.getPaymentByReference(paymentReference);
        return ResponseEntity.ok(toDTO(payment));
    }

    /**
     * Get payment receipt
     */
    @GetMapping("/{paymentReference}/receipt")
    @RequiresPermission("VIEW_PAYMENTS")
    public ResponseEntity<PaymentReceiptDTO> getPaymentReceipt(@PathVariable String paymentReference) {
        PaymentReceiptDTO receipt = paymentService.getPaymentReceipt(paymentReference);
        return ResponseEntity.ok(receipt);
    }

    /**
     * Get current user's payments
     */
    @GetMapping("/my-payments")
    @RequiresPermission("VIEW_PAYMENTS")
    public ResponseEntity<List<PaymentDTO>> getMyPayments() {
        List<Payment> payments = paymentService.getMyPayments();
        return ResponseEntity.ok(payments.stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
    }

    /**
     * Get payments for a customer
     */
    @GetMapping("/customers/{customerId}")
    @RequiresPermission("VIEW_PAYMENTS")
    public ResponseEntity<List<PaymentDTO>> getCustomerPayments(@PathVariable Long customerId) {
        List<Payment> payments = paymentService.getCustomerPayments(customerId);
        return ResponseEntity.ok(payments.stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
    }

    /**
     * Manually mark payment as successful after QR collection
     */
    @PostMapping("/{paymentReference}/manual-success")
    @RequiresPermission("COLLECT_PAYMENT")
    public ResponseEntity<PaymentDTO> markPaymentSuccess(
            @PathVariable String paymentReference,
            HttpServletRequest httpRequest) {
        Payment payment = paymentService.markPaymentSuccess(paymentReference, httpRequest);
        return ResponseEntity.ok(toDTO(payment));
    }

    private PaymentDTO toDTO(Payment payment) {
        return PaymentDTO.builder()
                .id(payment.getId())
                .paymentReference(payment.getPaymentReference())
                .customerId(payment.getCustomer() != null ? payment.getCustomer().getId() : null)
                .customerCode(payment.getCustomer() != null ? payment.getCustomer().getCustomerCode() : null)
                .customerName(payment.getCustomer() != null ? buildCustomerName(payment.getCustomer()) : null)
                .userId(payment.getUser() != null ? payment.getUser().getId() : null)
                .userEmployeeId(payment.getUser() != null ? payment.getUser().getEmployeeId() : null)
                .userName(payment.getUser() != null ? 
                        (payment.getUser().getFullName() != null ? 
                                payment.getUser().getFullName() : payment.getUser().getUsername()) : null)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .upiId(payment.getUpiId())
                .transactionId(payment.getTransactionId())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .paymentStatus(payment.getPaymentStatus())
                .paymentDate(payment.getPaymentDate())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private String buildCustomerName(com.company.ops_hub_api.domain.Customer customer) {
        StringBuilder name = new StringBuilder();
        if (customer.getFirstName() != null) {
            name.append(customer.getFirstName());
        }
        if (customer.getMiddleName() != null) {
            if (name.length() > 0) name.append(" ");
            name.append(customer.getMiddleName());
        }
        if (customer.getLastName() != null) {
            if (name.length() > 0) name.append(" ");
            name.append(customer.getLastName());
        }
        return name.length() > 0 ? name.toString() : customer.getCustomerCode();
    }
}
