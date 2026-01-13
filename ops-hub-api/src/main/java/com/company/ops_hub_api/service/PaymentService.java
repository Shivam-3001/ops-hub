package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.Customer;
import com.company.ops_hub_api.domain.Payment;
import com.company.ops_hub_api.domain.PaymentEvent;
import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.dto.InitiatePaymentDTO;
import com.company.ops_hub_api.dto.PaymentCallbackDTO;
import com.company.ops_hub_api.dto.PaymentReceiptDTO;
import com.company.ops_hub_api.repository.CustomerRepository;
import com.company.ops_hub_api.repository.PaymentEventRepository;
import com.company.ops_hub_api.repository.PaymentRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PaymentGatewayService gatewayService;
    private final AuditLogService auditLogService;
    private final EmailNotificationService emailNotificationService;
    private final ObjectMapper objectMapper;

    /**
     * Initiate a payment
     * Only users with COLLECT_PAYMENT permission can initiate payments
     */
    @Transactional
    public Payment initiatePayment(InitiatePaymentDTO dto, HttpServletRequest httpRequest) {
        // Check permission
        checkPaymentPermission();
        
        // Get current user
        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        
        // Get customer
        Long customerId = dto.getCustomerId();
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        
        // Validate UPI ID for UPI payments
        if ("UPI".equalsIgnoreCase(dto.getPaymentMethod()) && 
            (dto.getUpiId() == null || dto.getUpiId().trim().isEmpty())) {
            throw new IllegalArgumentException("UPI ID is required for UPI payments");
        }
        
        // Generate unique payment reference
        String paymentReference = generatePaymentReference();
        
        // Create payment record
        Payment payment = new Payment();
        payment.setPaymentReference(paymentReference);
        payment.setCustomer(customer);
        payment.setUser(currentUser);
        payment.setAmount(dto.getAmount());
        payment.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "INR");
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setUpiId(dto.getUpiId());
        payment.setPaymentStatus("PENDING"); // Maps to INITIATED in user terms
        
        Payment savedPayment = paymentRepository.save(payment);
        
        // Initiate gateway payment (for UPI)
        if ("UPI".equalsIgnoreCase(dto.getPaymentMethod())) {
            try {
                Map<String, Object> gatewayResponse = gatewayService.initiateUpiPayment(
                        paymentReference, dto.getAmount(), dto.getUpiId());
                
                savedPayment.setGatewayTransactionId((String) gatewayResponse.get("gatewayTransactionId"));
                savedPayment.setPaymentStatus("PROCESSING"); // Maps to INITIATED
                savedPayment.setGatewayResponse(convertToJson(gatewayResponse));
                savedPayment = paymentRepository.save(savedPayment);
                
                // Create payment event
                createPaymentEvent(savedPayment, "INITIATED", gatewayResponse, httpRequest);
            } catch (Exception e) {
                log.error("Error initiating gateway payment", e);
                savedPayment.setPaymentStatus("FAILED");
                savedPayment.setFailureReason("Gateway initiation failed: " + e.getMessage());
                savedPayment = paymentRepository.save(savedPayment);
            }
        } else {
            // For non-UPI payments (CASH, CARD, etc.), mark as SUCCESS immediately
            savedPayment.setPaymentStatus("SUCCESS");
            savedPayment.setPaymentDate(LocalDateTime.now());
            savedPayment.setTransactionId(generateTransactionId());
            savedPayment = paymentRepository.save(savedPayment);
            
            // Create payment event
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("paymentMethod", dto.getPaymentMethod());
            eventData.put("status", "SUCCESS");
            createPaymentEvent(savedPayment, "PROCESSED", eventData, httpRequest);
        }
        
        // Log audit
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("paymentId", savedPayment.getId());
        newValues.put("paymentReference", paymentReference);
        newValues.put("customerId", customer.getId());
        newValues.put("customerCode", customer.getCustomerCode());
        newValues.put("amount", dto.getAmount().toString());
        newValues.put("paymentMethod", dto.getPaymentMethod());
        newValues.put("paymentStatus", savedPayment.getPaymentStatus());
        
        Long paymentId = savedPayment.getId();
        if (paymentId != null) {
            auditLogService.logAction("CREATE", "PAYMENT", paymentId, 
                    null, newValues, httpRequest);
        }
        
        log.info("Payment {} initiated for customer {} by user {}", 
                paymentReference, customer.getCustomerCode(), currentUser.getEmployeeId());
        
        return savedPayment;
    }

    /**
     * Handle payment gateway callback
     * Ensures idempotency - same callback processed only once
     */
    @Transactional
    public Payment handleCallback(PaymentCallbackDTO dto, HttpServletRequest httpRequest) {
        log.info("Processing payment callback for reference: {}, gateway transaction: {}", 
                dto.getPaymentReference(), dto.getGatewayTransactionId());
        
        // Find payment by reference
        Payment payment = paymentRepository.findByPaymentReference(dto.getPaymentReference())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        
        // Check for duplicate callback (idempotency check)
        paymentEventRepository.findByPaymentReferenceAndGatewayTransactionId(
                dto.getPaymentReference(), dto.getGatewayTransactionId())
                .ifPresent(existing -> {
                    log.warn("Duplicate callback detected for payment {} and gateway transaction {}. Ignoring.",
                            dto.getPaymentReference(), dto.getGatewayTransactionId());
                    throw new IllegalStateException("Callback already processed");
                });
        
        // Verify callback signature (in production)
        Map<String, Object> callbackData = new HashMap<>();
        callbackData.put("gatewayTransactionId", dto.getGatewayTransactionId());
        callbackData.put("status", dto.getStatus());
        callbackData.put("failureReason", dto.getFailureReason());
        callbackData.put("gatewayResponse", dto.getGatewayResponse());
        
        if (!gatewayService.verifyCallbackSignature(callbackData, dto.getSignature())) {
            log.error("Invalid callback signature for payment {}", dto.getPaymentReference());
            throw new SecurityException("Invalid callback signature");
        }
        
        // Process callback
        Map<String, Object> normalizedCallback = gatewayService.processCallback(callbackData);
        
        // Update payment status (never trust client-side status)
        String normalizedStatus = (String) normalizedCallback.get("status");
        String oldStatus = payment.getPaymentStatus();
        
        if ("SUCCESS".equalsIgnoreCase(normalizedStatus)) {
            payment.setPaymentStatus("SUCCESS");
            payment.setPaymentDate(LocalDateTime.now());
            payment.setTransactionId(generateTransactionId());
        } else if ("FAILED".equalsIgnoreCase(normalizedStatus)) {
            payment.setPaymentStatus("FAILED");
            payment.setFailureReason((String) normalizedCallback.get("failureReason"));
        }
        
        payment.setGatewayTransactionId(dto.getGatewayTransactionId());
        payment.setGatewayResponse(dto.getGatewayResponse());
        
        Payment savedPayment = paymentRepository.save(payment);
        
        // Create payment event
        createPaymentEvent(savedPayment, "CALLBACK_RECEIVED", normalizedCallback, httpRequest);
        
        // Log audit
        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("paymentStatus", oldStatus);
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("paymentStatus", savedPayment.getPaymentStatus());
        newValues.put("gatewayTransactionId", dto.getGatewayTransactionId());
        newValues.put("callbackStatus", normalizedStatus);
        
        Long paymentId = savedPayment.getId();
        if (paymentId != null) {
            auditLogService.logAction("UPDATE", "PAYMENT", paymentId, 
                    oldValues, newValues, httpRequest);
        }
        
        // Send email notification for successful payment
        if ("SUCCESS".equalsIgnoreCase(normalizedStatus)) {
            try {
                User user = savedPayment.getUser();
                Customer customer = savedPayment.getCustomer();
                String userName = user.getFullName() != null ? user.getFullName() : user.getUsername();
                String customerName = buildCustomerName(customer);
                String amount = savedPayment.getAmount() + " " + savedPayment.getCurrency();
                
                emailNotificationService.sendPaymentSuccessfulNotification(
                        user.getEmail(),
                        userName,
                        savedPayment.getPaymentReference(),
                        amount,
                        customerName
                );
            } catch (Exception e) {
                log.error("Error sending payment success email notification", e);
            }
        }
        
        log.info("Payment callback processed. Payment: {}, Status: {}", 
                dto.getPaymentReference(), savedPayment.getPaymentStatus());
        
        return savedPayment;
    }

    /**
     * Get payment by reference
     */
    @Transactional(readOnly = true)
    public Payment getPaymentByReference(String paymentReference) {
        return paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
    }

    /**
     * Get payment receipt
     */
    @Transactional(readOnly = true)
    public PaymentReceiptDTO getPaymentReceipt(String paymentReference) {
        Payment payment = getPaymentByReference(paymentReference);
        
        Customer customer = payment.getCustomer();
        User user = payment.getUser();
        
        return PaymentReceiptDTO.builder()
                .receiptNumber("RCPT-" + paymentReference)
                .paymentReference(paymentReference)
                .receiptDate(LocalDateTime.now())
                .paymentDate(payment.getPaymentDate())
                .customerCode(customer != null ? customer.getCustomerCode() : null)
                .customerName(customer != null ? buildCustomerName(customer) : null)
                .customerAddress(customer != null ? buildCustomerAddress(customer) : null)
                .customerPhone(customer != null ? customer.getPhoneEncrypted() : null) // Note: Decrypt in production
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .transactionId(payment.getTransactionId())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .paymentStatus(payment.getPaymentStatus())
                .collectedBy(user != null ? 
                        (user.getFullName() != null ? user.getFullName() : user.getUsername()) : null)
                .collectedByEmployeeId(user != null ? user.getEmployeeId() : null)
                .notes(payment.getGatewayResponse()) // Can be customized
                .receiptFormat("PDF") // Default format
                .build();
    }

    /**
     * Get payments for current user
     */
    @Transactional(readOnly = true)
    public List<Payment> getMyPayments() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        return paymentRepository.findByUserId(userId);
    }

    /**
     * Get payments for a customer
     */
    @Transactional(readOnly = true)
    public List<Payment> getCustomerPayments(Long customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        return paymentRepository.findByCustomerId(customerId);
    }

    private void checkPaymentPermission() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("User not authenticated");
        }
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        if (!userPrincipal.hasPermission("COLLECT_PAYMENT")) {
            throw new AccessDeniedException("Insufficient permissions to collect payments");
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("User not authenticated");
        }
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private String generatePaymentReference() {
        return "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase() + 
               "-" + System.currentTimeMillis();
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private void createPaymentEvent(Payment payment, String eventType, Map<String, Object> eventData, HttpServletRequest httpRequest) {
        try {
            PaymentEvent event = new PaymentEvent();
            event.setPayment(payment);
            event.setEventType(eventType);
            event.setEventData(convertToJson(eventData));
            event.setGatewayName((String) eventData.get("gatewayName"));
            
            if (httpRequest != null) {
                event.setIpAddress(getClientIpAddress(httpRequest));
                event.setUserAgent(httpRequest.getHeader("User-Agent"));
            }
            
            paymentEventRepository.save(event);
        } catch (Exception e) {
            log.error("Error creating payment event", e);
            // Don't throw - event creation failure shouldn't break payment flow
        }
    }

    private String convertToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Error converting to JSON", e);
            return obj.toString();
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String buildCustomerName(Customer customer) {
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

    private String buildCustomerAddress(Customer customer) {
        StringBuilder address = new StringBuilder();
        if (customer.getAddressLine1() != null) {
            address.append(customer.getAddressLine1());
        }
        if (customer.getAddressLine2() != null) {
            if (address.length() > 0) address.append(", ");
            address.append(customer.getAddressLine2());
        }
        if (customer.getCity() != null) {
            if (address.length() > 0) address.append(", ");
            address.append(customer.getCity());
        }
        if (customer.getState() != null) {
            if (address.length() > 0) address.append(", ");
            address.append(customer.getState());
        }
        if (customer.getPostalCode() != null) {
            if (address.length() > 0) address.append(" - ");
            address.append(customer.getPostalCode());
        }
        return address.toString();
    }
}
