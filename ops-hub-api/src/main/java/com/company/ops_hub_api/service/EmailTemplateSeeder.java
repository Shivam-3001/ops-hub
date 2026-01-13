package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.EmailTemplate;
import com.company.ops_hub_api.repository.EmailTemplateRepository;
import com.company.ops_hub_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Email Template Seeder
 * Creates default email templates for the application
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateSeeder {

    private final EmailTemplateRepository templateRepository;
    private final UserRepository userRepository;

    @Bean
    @Order(3) // Run after RBAC seeder
    @Transactional
    public CommandLineRunner seedEmailTemplates() {
        return args -> {
            if (templateRepository.count() > 0) {
                log.info("Email templates already exist. Skipping seed data.");
                return;
            }

            log.info("Starting email template seeding...");

            // Get admin user for created_by
            var adminUser = userRepository.findByEmployeeId("EMP004").orElse(null);

            // 1. User Onboarding Template
            createTemplate(
                    "USER_ONBOARDING",
                    "Welcome to Ops Hub",
                    "Welcome {{userName}}!",
                    getOnboardingHtml(),
                    getOnboardingText(),
                    Arrays.asList("userName", "employeeId", "temporaryPassword"),
                    "USER_MANAGEMENT",
                    adminUser
            );

            // 2. Profile Update Submitted Template
            createTemplate(
                    "PROFILE_UPDATE_SUBMITTED",
                    "Profile Update Submitted",
                    "Profile Update Request Submitted",
                    getProfileUpdateSubmittedHtml(),
                    getProfileUpdateSubmittedText(),
                    Arrays.asList("userName", "requestId"),
                    "PROFILE",
                    adminUser
            );

            // 3. Profile Update Approved Template
            createTemplate(
                    "PROFILE_UPDATE_APPROVED",
                    "Profile Update Approved",
                    "Your Profile Update Has Been Approved",
                    getProfileUpdateApprovedHtml(),
                    getProfileUpdateApprovedText(),
                    Arrays.asList("userName", "requestId"),
                    "PROFILE",
                    adminUser
            );

            // 4. Profile Update Rejected Template
            createTemplate(
                    "PROFILE_UPDATE_REJECTED",
                    "Profile Update Rejected",
                    "Profile Update Request Rejected",
                    getProfileUpdateRejectedHtml(),
                    getProfileUpdateRejectedText(),
                    Arrays.asList("userName", "requestId", "rejectionReason"),
                    "PROFILE",
                    adminUser
            );

            // 5. Customer Allocated Template
            createTemplate(
                    "CUSTOMER_ALLOCATED",
                    "Customer Assigned to You",
                    "New Customer Assignment",
                    getCustomerAllocatedHtml(),
                    getCustomerAllocatedText(),
                    Arrays.asList("userName", "customerName", "customerCode", "roleCode"),
                    "ALLOCATION",
                    adminUser
            );

            // 6. Payment Successful Template
            createTemplate(
                    "PAYMENT_SUCCESSFUL",
                    "Payment Successful",
                    "Payment Received Successfully",
                    getPaymentSuccessfulHtml(),
                    getPaymentSuccessfulText(),
                    Arrays.asList("userName", "paymentReference", "amount", "customerName"),
                    "PAYMENT",
                    adminUser
            );

            log.info("Email template seeding completed successfully!");
        };
    }

    private EmailTemplate createTemplate(String code, String name, String subject, 
                                        String htmlBody, String textBody, 
                                        List<String> variables, String category, 
                                        com.company.ops_hub_api.domain.User createdBy) {
        EmailTemplate template = new EmailTemplate();
        template.setTemplateCode(code);
        template.setName(name);
        template.setSubject(subject);
        template.setBodyHtml(htmlBody);
        template.setBodyText(textBody);
        template.setVariables(String.join(",", variables));
        template.setCategory(category);
        template.setActive(true);
        template.setCreatedBy(createdBy);
        return templateRepository.save(template);
    }

    // Template HTML Bodies
    private String getOnboardingHtml() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .credentials { background-color: #fff; padding: 15px; border-left: 4px solid #4CAF50; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to Ops Hub!</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>{{userName}}</strong>,</p>
                        <p>Your account has been created successfully. You can now access the Ops Hub platform.</p>
                        <div class="credentials">
                            <p><strong>Employee ID:</strong> {{employeeId}}</p>
                            <p><strong>Temporary Password:</strong> {{temporaryPassword}}</p>
                        </div>
                        <p>Please log in and change your password after your first login.</p>
                        <p>If you have any questions, please contact your administrator.</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated email from Ops Hub. Please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    private String getProfileUpdateSubmittedHtml() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #2196F3; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Profile Update Submitted</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>{{userName}}</strong>,</p>
                        <p>Your profile update request (ID: {{requestId}}) has been submitted successfully and is pending approval.</p>
                        <p>You will be notified once your request has been reviewed.</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    private String getProfileUpdateApprovedHtml() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Profile Update Approved</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>{{userName}}</strong>,</p>
                        <p>Your profile update request (ID: {{requestId}}) has been approved.</p>
                        <p>Your profile has been updated successfully.</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    private String getProfileUpdateRejectedHtml() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #f44336; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .reason { background-color: #fff; padding: 15px; border-left: 4px solid #f44336; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Profile Update Rejected</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>{{userName}}</strong>,</p>
                        <p>Your profile update request (ID: {{requestId}}) has been rejected.</p>
                        <div class="reason">
                            <p><strong>Reason:</strong> {{rejectionReason}}</p>
                        </div>
                        <p>Please review the reason and submit a new request if needed.</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    private String getCustomerAllocatedHtml() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #FF9800; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .info { background-color: #fff; padding: 15px; border-left: 4px solid #FF9800; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>New Customer Assignment</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>{{userName}}</strong>,</p>
                        <p>A new customer has been assigned to you.</p>
                        <div class="info">
                            <p><strong>Customer:</strong> {{customerName}}</p>
                            <p><strong>Customer Code:</strong> {{customerCode}}</p>
                            <p><strong>Role:</strong> {{roleCode}}</p>
                        </div>
                        <p>Please log in to view customer details and start working.</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    private String getPaymentSuccessfulHtml() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .payment-info { background-color: #fff; padding: 15px; border-left: 4px solid #4CAF50; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Payment Successful</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>{{userName}}</strong>,</p>
                        <p>Your payment has been processed successfully.</p>
                        <div class="payment-info">
                            <p><strong>Payment Reference:</strong> {{paymentReference}}</p>
                            <p><strong>Amount:</strong> {{amount}}</p>
                            <p><strong>Customer:</strong> {{customerName}}</p>
                        </div>
                        <p>Thank you for your payment. A receipt has been generated for your records.</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    // Template Text Bodies
    private String getOnboardingText() {
        return """
            Welcome to Ops Hub!
            
            Hello {{userName}},
            
            Your account has been created successfully. You can now access the Ops Hub platform.
            
            Employee ID: {{employeeId}}
            Temporary Password: {{temporaryPassword}}
            
            Please log in and change your password after your first login.
            
            If you have any questions, please contact your administrator.
            
            This is an automated email from Ops Hub. Please do not reply.
            """;
    }

    private String getProfileUpdateSubmittedText() {
        return """
            Profile Update Submitted
            
            Hello {{userName}},
            
            Your profile update request (ID: {{requestId}}) has been submitted successfully and is pending approval.
            
            You will be notified once your request has been reviewed.
            """;
    }

    private String getProfileUpdateApprovedText() {
        return """
            Profile Update Approved
            
            Hello {{userName}},
            
            Your profile update request (ID: {{requestId}}) has been approved.
            
            Your profile has been updated successfully.
            """;
    }

    private String getProfileUpdateRejectedText() {
        return """
            Profile Update Rejected
            
            Hello {{userName}},
            
            Your profile update request (ID: {{requestId}}) has been rejected.
            
            Reason: {{rejectionReason}}
            
            Please review the reason and submit a new request if needed.
            """;
    }

    private String getCustomerAllocatedText() {
        return """
            New Customer Assignment
            
            Hello {{userName}},
            
            A new customer has been assigned to you.
            
            Customer: {{customerName}}
            Customer Code: {{customerCode}}
            Role: {{roleCode}}
            
            Please log in to view customer details and start working.
            """;
    }

    private String getPaymentSuccessfulText() {
        return """
            Payment Successful
            
            Hello {{userName}},
            
            Your payment has been processed successfully.
            
            Payment Reference: {{paymentReference}}
            Amount: {{amount}}
            Customer: {{customerName}}
            
            Thank you for your payment. A receipt has been generated for your records.
            """;
    }
}
