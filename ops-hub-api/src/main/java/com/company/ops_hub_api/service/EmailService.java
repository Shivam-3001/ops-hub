package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.EmailLog;
import com.company.ops_hub_api.domain.EmailTemplate;
import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.repository.EmailLogRepository;
import com.company.ops_hub_api.repository.EmailTemplateRepository;
import com.company.ops_hub_api.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Email Service
 * Handles email sending with template support, async processing, and retry logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailTemplateRepository templateRepository;
    private final EmailLogRepository emailLogRepository;
    private final EncryptionUtil encryptionUtil;

    // Pattern for template variables: {{variableName}}
    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    /**
     * Send email using a template
     * This method is async and will not block the calling thread
     */
    @Async
    @Transactional
    public void sendEmail(String templateCode, String recipientEmail, String recipientName, 
                         Map<String, Object> variables, User sentBy) {
        try {
            // Load template
            EmailTemplate template = templateRepository.findByTemplateCodeAndActiveTrue(templateCode)
                    .orElseThrow(() -> new IllegalArgumentException("Email template not found or inactive: " + templateCode));

            // Process template variables
            String processedSubject = processTemplate(template.getSubject(), variables);
            String processedBodyHtml = template.getBodyHtml() != null 
                    ? processTemplate(template.getBodyHtml(), variables) : null;
            String processedBodyText = template.getBodyText() != null 
                    ? processTemplate(template.getBodyText(), variables) : null;

            // Encrypt recipient email
            String encryptedEmail = encryptionUtil.encrypt(recipientEmail);

            // Create email log entry
            EmailLog emailLog = new EmailLog();
            emailLog.setTemplate(template);
            emailLog.setTemplateCode(templateCode);
            emailLog.setRecipientEmailEncrypted(encryptedEmail);
            emailLog.setRecipientName(recipientName);
            emailLog.setSubject(processedSubject);
            emailLog.setBodyHtml(processedBodyHtml);
            emailLog.setBodyText(processedBodyText);
            emailLog.setEmailStatus("PENDING");
            emailLog.setSentBy(sentBy);
            emailLog.setCreatedAt(LocalDateTime.now());

            emailLog = emailLogRepository.save(emailLog);

            // Attempt to send email
            sendEmailAsync(emailLog, recipientEmail);

        } catch (Exception e) {
            log.error("Error sending email with template {} to {}", templateCode, recipientEmail, e);
            // Create failed log entry
            createFailedEmailLog(templateCode, recipientEmail, recipientName, 
                    "Error processing email: " + e.getMessage(), sentBy);
        }
    }

    /**
     * Send email asynchronously
     * This is where actual email sending logic would be integrated
     */
    @Async
    @Transactional
    public void sendEmailAsync(EmailLog emailLog, String recipientEmail) {
        try {
            log.info("Sending email to {} with subject: {}", recipientEmail, emailLog.getSubject());

            // TODO: Integrate with actual email service (SMTP, SendGrid, AWS SES, etc.)
            // For now, we simulate email sending
            boolean emailSent = sendEmailViaProvider(
                    recipientEmail,
                    emailLog.getRecipientName(),
                    emailLog.getSubject(),
                    emailLog.getBodyHtml(),
                    emailLog.getBodyText()
            );

            if (emailSent) {
                emailLog.setEmailStatus("SENT");
                emailLog.setSentAt(LocalDateTime.now());
                emailLog.setProviderMessageId("MSG-" + System.currentTimeMillis());
                emailLog.setProviderResponse("Email sent successfully");
                log.info("Email sent successfully to {}", recipientEmail);
            } else {
                emailLog.setEmailStatus("FAILED");
                emailLog.setFailedAt(LocalDateTime.now());
                emailLog.setFailureReason("Email provider returned failure");
                log.warn("Email sending failed for {}", recipientEmail);
            }

            emailLogRepository.save(emailLog);

        } catch (Exception e) {
            log.error("Error sending email to {}", recipientEmail, e);
            emailLog.setEmailStatus("FAILED");
            emailLog.setFailedAt(LocalDateTime.now());
            emailLog.setFailureReason("Exception: " + e.getMessage());
            emailLogRepository.save(emailLog);
        }
    }

    /**
     * Send email via email provider
     * This method should be replaced with actual email service integration
     */
    private boolean sendEmailViaProvider(String recipientEmail, String recipientName, 
                                        String subject, String bodyHtml, String bodyText) {
        // TODO: Replace with actual email service integration
        // Examples:
        // - JavaMailSender (SMTP)
        // - SendGrid API
        // - AWS SES
        // - Mailgun
        // - etc.

        // For now, simulate successful sending
        // In production, this would make actual API calls to email service
        log.debug("Simulating email send to {}: {}", recipientEmail, subject);
        
        // Simulate network delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate 95% success rate for testing
        return Math.random() > 0.05;
    }

    /**
     * Process template with variables
     * Replaces {{variableName}} with actual values
     */
    private String processTemplate(String template, Map<String, Object> variables) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        String result = template;
        java.util.regex.Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(template);

        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            Object value = variables.get(variableName);
            String replacement = value != null ? value.toString() : "";
            result = result.replace("{{" + variableName + "}}", replacement);
        }

        return result;
    }

    /**
     * Create failed email log entry
     */
    private void createFailedEmailLog(String templateCode, String recipientEmail, 
                                      String recipientName, String failureReason, User sentBy) {
        try {
            EmailLog emailLog = new EmailLog();
            emailLog.setTemplateCode(templateCode);
            emailLog.setRecipientEmailEncrypted(encryptionUtil.encrypt(recipientEmail));
            emailLog.setRecipientName(recipientName);
            emailLog.setEmailStatus("FAILED");
            emailLog.setFailedAt(LocalDateTime.now());
            emailLog.setFailureReason(failureReason);
            emailLog.setSentBy(sentBy);
            emailLog.setCreatedAt(LocalDateTime.now());
            emailLogRepository.save(emailLog);
        } catch (Exception e) {
            log.error("Error creating failed email log", e);
        }
    }

    /**
     * Retry failed emails
     * This can be called by a scheduled job
     */
    @Async
    @Transactional
    public void retryFailedEmails(int maxRetries, int retryDelayMinutes) {
        LocalDateTime retryBefore = LocalDateTime.now().minusMinutes(retryDelayMinutes);
        List<EmailLog> failedEmails = emailLogRepository.findFailedEmailsForRetry("FAILED", retryBefore);

        log.info("Retrying {} failed emails", failedEmails.size());

        for (EmailLog emailLog : failedEmails) {
            try {
                // Decrypt email for retry
                String recipientEmail = encryptionUtil.decrypt(emailLog.getRecipientEmailEncrypted());
                
                // Check retry count (could be stored in a separate field)
                // For simplicity, we'll just retry once
                emailLog.setEmailStatus("PENDING");
                emailLogRepository.save(emailLog);
                
                sendEmailAsync(emailLog, recipientEmail);
                
            } catch (Exception e) {
                log.error("Error retrying email {}", emailLog.getId(), e);
            }
        }
    }


    /**
     * Send email without template (direct)
     */
    @Async
    @Transactional
    public void sendEmailDirect(String recipientEmail, String recipientName, String subject, 
                               String bodyHtml, String bodyText, User sentBy) {
        try {
            String encryptedEmail = encryptionUtil.encrypt(recipientEmail);

            EmailLog emailLog = new EmailLog();
            emailLog.setTemplateCode("DIRECT");
            emailLog.setRecipientEmailEncrypted(encryptedEmail);
            emailLog.setRecipientName(recipientName);
            emailLog.setSubject(subject);
            emailLog.setBodyHtml(bodyHtml);
            emailLog.setBodyText(bodyText);
            emailLog.setEmailStatus("PENDING");
            emailLog.setSentBy(sentBy);
            emailLog.setCreatedAt(LocalDateTime.now());

            emailLog = emailLogRepository.save(emailLog);
            sendEmailAsync(emailLog, recipientEmail);

        } catch (Exception e) {
            log.error("Error sending direct email to {}", recipientEmail, e);
        }
    }
}
