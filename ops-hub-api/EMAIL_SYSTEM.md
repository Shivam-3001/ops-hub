# Automated Email System

## Overview

The Ops Hub application includes a comprehensive automated email system that sends notifications for various events. The system is designed with:

- **Template-based emails** (not hardcoded)
- **Asynchronous sending** (non-blocking)
- **Delivery status tracking** in `email_logs` table
- **Retry support** for failed emails
- **Integration-ready** for any email provider

## Email Triggers

The system automatically sends emails for the following events:

### 1. User Onboarding / Invitation
- **Template Code**: `USER_ONBOARDING`
- **Trigger**: When a new user is created
- **Variables**: `userName`, `employeeId`, `temporaryPassword`
- **Note**: Should be triggered when users are created via API (not during data seeding)

### 2. Profile Update Submitted
- **Template Code**: `PROFILE_UPDATE_SUBMITTED`
- **Trigger**: When a user submits a profile update request
- **Variables**: `userName`, `requestId`
- **Service**: `ProfileUpdateRequestService.submitRequest()`

### 3. Profile Update Approved
- **Template Code**: `PROFILE_UPDATE_APPROVED`
- **Trigger**: When a profile update request is approved
- **Variables**: `userName`, `requestId`
- **Service**: `ProfileUpdateRequestService.approveRequest()`

### 4. Profile Update Rejected
- **Template Code**: `PROFILE_UPDATE_REJECTED`
- **Trigger**: When a profile update request is rejected
- **Variables**: `userName`, `requestId`, `rejectionReason`
- **Service**: `ProfileUpdateRequestService.rejectRequest()`

### 5. Customer Allocated
- **Template Code**: `CUSTOMER_ALLOCATED`
- **Trigger**: When a customer is allocated to a user
- **Variables**: `userName`, `customerName`, `customerCode`, `roleCode`
- **Service**: `CustomerAllocationService.allocateCustomer()`

### 6. Payment Successful
- **Template Code**: `PAYMENT_SUCCESSFUL`
- **Trigger**: When a payment callback confirms successful payment
- **Variables**: `userName`, `paymentReference`, `amount`, `customerName`
- **Service**: `PaymentService.handleCallback()`

## Architecture

### Components

1. **EmailService** (`com.company.ops_hub_api.service.EmailService`)
   - Core email sending service
   - Handles template processing and variable replacement
   - Async email sending
   - Email logging to `email_logs` table
   - Retry logic for failed emails

2. **EmailNotificationService** (`com.company.ops_hub_api.service.EmailNotificationService`)
   - High-level notification service
   - Provides convenient methods for each email type
   - Handles email decryption and variable preparation

3. **EmailTemplateSeeder** (`com.company.ops_hub_api.service.EmailTemplateSeeder`)
   - Seeds default email templates on application startup
   - Creates templates for all email types

4. **Repositories**
   - `EmailTemplateRepository`: Manages email templates
   - `EmailLogRepository`: Manages email logs and retry queries

### Database Tables

#### email_templates
- Stores reusable email templates
- Fields: `template_code`, `name`, `subject`, `body_html`, `body_text`, `variables`, `category`, `active`

#### email_logs
- Stores all sent emails and delivery status
- Fields: `template_id`, `recipient_email_encrypted`, `subject`, `body_html`, `body_text`, `email_status`, `sent_at`, `delivered_at`, `failed_at`, `failure_reason`, `provider_message_id`

## Email Status Flow

```
PENDING → SENT → DELIVERED
         ↓
       FAILED → (Retry) → SENT
```

- **PENDING**: Email queued for sending
- **SENT**: Email sent to provider (awaiting delivery confirmation)
- **DELIVERED**: Email delivered to recipient
- **FAILED**: Email sending failed (can be retried)
- **BOUNCED**: Email bounced back

## Template Variables

Templates use `{{variableName}}` syntax for variable replacement.

Example:
```
Hello {{userName}},

Your payment of {{amount}} has been processed successfully.
Payment Reference: {{paymentReference}}
```

## Async Configuration

Email sending is configured to run asynchronously using Spring's `@Async`:

- **Thread Pool**: `emailTaskExecutor`
- **Core Pool Size**: 5
- **Max Pool Size**: 10
- **Queue Capacity**: 100

Configuration: `com.company.ops_hub_api.config.AsyncConfig`

## Integration with Email Providers

The `EmailService.sendEmailViaProvider()` method is currently simulated. To integrate with an actual email provider:

### Option 1: JavaMailSender (SMTP)
```java
@Autowired
private JavaMailSender mailSender;

private boolean sendEmailViaProvider(...) {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true);
    helper.setTo(recipientEmail);
    helper.setSubject(subject);
    helper.setText(bodyHtml, true);
    mailSender.send(message);
    return true;
}
```

### Option 2: SendGrid
```java
@Autowired
private SendGrid sendGrid;

private boolean sendEmailViaProvider(...) {
    Email from = new Email("noreply@opshub.com");
    Email to = new Email(recipientEmail);
    Content content = new Content("text/html", bodyHtml);
    Mail mail = new Mail(from, subject, to, content);
    Request request = new Request();
    request.setMethod(Method.POST);
    request.setEndpoint("mail/send");
    request.setBody(mail.build());
    Response response = sendGrid.api(request);
    return response.getStatusCode() == 202;
}
```

### Option 3: AWS SES
```java
@Autowired
private AmazonSimpleEmailService sesClient;

private boolean sendEmailViaProvider(...) {
    SendEmailRequest request = new SendEmailRequest()
        .withDestination(new Destination().withToAddresses(recipientEmail))
        .withMessage(new Message()
            .withSubject(new Content().withData(subject))
            .withBody(new Body().withHtml(new Content().withData(bodyHtml))))
        .withSource("noreply@opshub.com");
    SendEmailResult result = sesClient.sendEmail(request);
    return result.getMessageId() != null;
}
```

## Retry Logic

Failed emails can be retried using:

```java
emailService.retryFailedEmails(maxRetries, retryDelayMinutes);
```

This method:
1. Finds emails with status `FAILED` created before `retryDelayMinutes` ago
2. Resets status to `PENDING`
3. Attempts to send again

**Recommended**: Set up a scheduled job (e.g., using `@Scheduled`) to retry failed emails periodically.

## Usage Examples

### Sending an Email

```java
@Autowired
private EmailNotificationService emailNotificationService;

// Send profile update approved notification
emailNotificationService.sendProfileUpdateApprovedNotification(
    user.getEmail(),  // encrypted email
    user.getFullName(),
    requestId
);
```

### Direct Email Sending (without template)

```java
@Autowired
private EmailService emailService;

Map<String, Object> variables = new HashMap<>();
variables.put("customField", "value");

emailService.sendEmail(
    "CUSTOM_TEMPLATE",
    "user@example.com",
    "User Name",
    variables,
    currentUser
);
```

## Testing

### Check Email Logs

```sql
SELECT 
    el.id,
    et.name as template_name,
    el.email_status,
    el.sent_at,
    el.failed_at,
    el.failure_reason
FROM email_logs el
LEFT JOIN email_templates et ON el.template_id = et.id
ORDER BY el.created_at DESC;
```

### View Failed Emails

```sql
SELECT * FROM email_logs 
WHERE email_status = 'FAILED' 
ORDER BY created_at DESC;
```

## Configuration

### Application Properties

Add email provider configuration to `application.yaml`:

```yaml
# For JavaMailSender (SMTP)
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

# For SendGrid
sendgrid:
  api-key: your-api-key

# For AWS SES
aws:
  ses:
    region: us-east-1
    access-key: your-access-key
    secret-key: your-secret-key
```

## Best Practices

1. **Always use templates** - Never hardcode email content
2. **Encrypt recipient emails** - Store encrypted emails in `email_logs`
3. **Handle failures gracefully** - Don't let email failures break business logic
4. **Monitor email logs** - Regularly check for failed emails
5. **Set up retry jobs** - Automatically retry failed emails
6. **Test templates** - Verify template variables are replaced correctly
7. **Rate limiting** - Consider rate limiting for email sending
8. **Email validation** - Validate email addresses before sending

## Future Enhancements

- [ ] Email delivery webhooks (for delivery status updates)
- [ ] Email template editor UI
- [ ] Email scheduling (send at specific time)
- [ ] Email batching (send multiple emails in one transaction)
- [ ] Email analytics (open rates, click rates)
- [ ] Multi-language email templates
- [ ] Email attachments support
