package com.company.ops_hub_api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Email Notification Service
 * Integration-ready service for sending email notifications
 * Currently logs notifications - can be extended to use email templates and email_logs table
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    /**
     * Send profile update approval notification
     * @param userEmail User's email address
     * @param userName User's name
     * @param requestId Profile update request ID
     */
    public void sendProfileUpdateApprovedNotification(String userEmail, String userName, Long requestId) {
        log.info("Sending profile update approval notification to {} for request {}", userEmail, requestId);
        // TODO: Integrate with email service
        // 1. Load email template (e.g., "PROFILE_UPDATE_APPROVED")
        // 2. Replace template variables with user data
        // 3. Send email via email service
        // 4. Log to email_logs table
    }

    /**
     * Send profile update rejection notification
     * @param userEmail User's email address
     * @param userName User's name
     * @param requestId Profile update request ID
     * @param rejectionReason Reason for rejection
     */
    public void sendProfileUpdateRejectedNotification(String userEmail, String userName, 
                                                      Long requestId, String rejectionReason) {
        log.info("Sending profile update rejection notification to {} for request {} with reason: {}", 
                userEmail, requestId, rejectionReason);
        // TODO: Integrate with email service
        // 1. Load email template (e.g., "PROFILE_UPDATE_REJECTED")
        // 2. Replace template variables with user data and rejection reason
        // 3. Send email via email service
        // 4. Log to email_logs table
    }

    /**
     * Send profile update submitted notification
     * @param userEmail User's email address
     * @param userName User's name
     * @param requestId Profile update request ID
     */
    public void sendProfileUpdateSubmittedNotification(String userEmail, String userName, Long requestId) {
        log.info("Sending profile update submitted notification to {} for request {}", userEmail, requestId);
        // TODO: Integrate with email service
        // 1. Load email template (e.g., "PROFILE_UPDATE_SUBMITTED")
        // 2. Replace template variables with user data
        // 3. Send email via email service
        // 4. Log to email_logs table
    }
}
