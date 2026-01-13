package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.security.UserPrincipal;
import com.company.ops_hub_api.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Email Notification Service
 * Service for sending email notifications using templates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;

    /**
     * Send profile update approval notification
     * @param userEmail User's email address (encrypted)
     * @param userName User's name
     * @param requestId Profile update request ID
     */
    public void sendProfileUpdateApprovedNotification(String userEmail, String userName, Long requestId) {
        try {
            String decryptedEmail = encryptionUtil.decrypt(userEmail);
            Map<String, Object> variables = new HashMap<>();
            variables.put("userName", userName);
            variables.put("requestId", requestId != null ? requestId.toString() : "N/A");
            
            User sentBy = getCurrentUser();
            emailService.sendEmail("PROFILE_UPDATE_APPROVED", decryptedEmail, userName, variables, sentBy);
            log.info("Profile update approval notification queued for {}", decryptedEmail);
        } catch (Exception e) {
            log.error("Error sending profile update approval notification", e);
        }
    }

    /**
     * Send profile update rejection notification
     * @param userEmail User's email address (encrypted)
     * @param userName User's name
     * @param requestId Profile update request ID
     * @param rejectionReason Reason for rejection
     */
    public void sendProfileUpdateRejectedNotification(String userEmail, String userName, 
                                                      Long requestId, String rejectionReason) {
        try {
            String decryptedEmail = encryptionUtil.decrypt(userEmail);
            Map<String, Object> variables = new HashMap<>();
            variables.put("userName", userName);
            variables.put("requestId", requestId != null ? requestId.toString() : "N/A");
            variables.put("rejectionReason", rejectionReason != null ? rejectionReason : "No reason provided");
            
            User sentBy = getCurrentUser();
            emailService.sendEmail("PROFILE_UPDATE_REJECTED", decryptedEmail, userName, variables, sentBy);
            log.info("Profile update rejection notification queued for {}", decryptedEmail);
        } catch (Exception e) {
            log.error("Error sending profile update rejection notification", e);
        }
    }

    /**
     * Send profile update submitted notification
     * @param userEmail User's email address (encrypted)
     * @param userName User's name
     * @param requestId Profile update request ID
     */
    public void sendProfileUpdateSubmittedNotification(String userEmail, String userName, Long requestId) {
        try {
            String decryptedEmail = encryptionUtil.decrypt(userEmail);
            Map<String, Object> variables = new HashMap<>();
            variables.put("userName", userName);
            variables.put("requestId", requestId != null ? requestId.toString() : "N/A");
            
            User sentBy = getCurrentUser();
            emailService.sendEmail("PROFILE_UPDATE_SUBMITTED", decryptedEmail, userName, variables, sentBy);
            log.info("Profile update submitted notification queued for {}", decryptedEmail);
        } catch (Exception e) {
            log.error("Error sending profile update submitted notification", e);
        }
    }

    /**
     * Send user onboarding/invitation email
     * @param userEmail User's email address (encrypted)
     * @param userName User's name
     * @param employeeId Employee ID
     * @param temporaryPassword Temporary password (if applicable)
     */
    public void sendUserOnboardingNotification(String userEmail, String userName, String employeeId, String temporaryPassword) {
        try {
            String decryptedEmail = encryptionUtil.decrypt(userEmail);
            Map<String, Object> variables = new HashMap<>();
            variables.put("userName", userName);
            variables.put("employeeId", employeeId);
            variables.put("temporaryPassword", temporaryPassword != null ? temporaryPassword : "Please contact administrator");
            
            User sentBy = getCurrentUser();
            emailService.sendEmail("USER_ONBOARDING", decryptedEmail, userName, variables, sentBy);
            log.info("User onboarding notification queued for {}", decryptedEmail);
        } catch (Exception e) {
            log.error("Error sending user onboarding notification", e);
        }
    }

    /**
     * Send customer allocation notification
     * @param userEmail User's email address (encrypted)
     * @param userName User's name
     * @param customerName Customer name
     * @param customerCode Customer code
     * @param roleCode Role code
     */
    public void sendCustomerAllocatedNotification(String userEmail, String userName, String customerName, 
                                                  String customerCode, String roleCode) {
        try {
            String decryptedEmail = encryptionUtil.decrypt(userEmail);
            Map<String, Object> variables = new HashMap<>();
            variables.put("userName", userName);
            variables.put("customerName", customerName);
            variables.put("customerCode", customerCode);
            variables.put("roleCode", roleCode);
            
            User sentBy = getCurrentUser();
            emailService.sendEmail("CUSTOMER_ALLOCATED", decryptedEmail, userName, variables, sentBy);
            log.info("Customer allocation notification queued for {}", decryptedEmail);
        } catch (Exception e) {
            log.error("Error sending customer allocation notification", e);
        }
    }

    /**
     * Send payment successful notification
     * @param userEmail User's email address (encrypted)
     * @param userName User's name
     * @param paymentReference Payment reference
     * @param amount Payment amount
     * @param customerName Customer name
     */
    public void sendPaymentSuccessfulNotification(String userEmail, String userName, String paymentReference, 
                                                  String amount, String customerName) {
        try {
            String decryptedEmail = encryptionUtil.decrypt(userEmail);
            Map<String, Object> variables = new HashMap<>();
            variables.put("userName", userName);
            variables.put("paymentReference", paymentReference);
            variables.put("amount", amount);
            variables.put("customerName", customerName);
            
            User sentBy = getCurrentUser();
            emailService.sendEmail("PAYMENT_SUCCESSFUL", decryptedEmail, userName, variables, sentBy);
            log.info("Payment successful notification queued for {}", decryptedEmail);
        } catch (Exception e) {
            log.error("Error sending payment successful notification", e);
        }
    }

    /**
     * Get current user from security context
     */
    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                Long userId = userPrincipal.getUserId();
                if (userId != null) {
                    return userRepository.findById(userId).orElse(null);
                }
            }
        } catch (Exception e) {
            log.debug("Could not get current user from security context", e);
        }
        return null;
    }
}
