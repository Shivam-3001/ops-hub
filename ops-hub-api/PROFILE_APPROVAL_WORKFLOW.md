# Profile Update Approval Workflow Documentation

## Overview
This document describes the profile update approval workflow implementation for the Ops Hub application. The system allows users to submit profile update requests that require approval from users with the `APPROVE_PROFILE` permission (typically Leads and Admins).

## Architecture

### Components

#### 1. **ProfileUpdateRequestService** (`service/ProfileUpdateRequestService.java`)
- Handles submission, approval, and rejection of profile update requests
- Enforces permission checks
- Manages transactional operations
- Integrates with audit logging and email notifications

#### 2. **AuditLogService** (`service/AuditLogService.java`)
- Centralized audit logging service
- Logs all profile update request actions
- Captures request metadata, IP addresses, and user agents
- Handles both success and failure scenarios

#### 3. **EmailNotificationService** (`service/EmailNotificationService.java`)
- Integration-ready email notification service
- Currently logs notifications (ready for email template integration)
- Sends notifications on submission, approval, and rejection

#### 4. **ProfileUpdateRequestController** (`controller/ProfileUpdateRequestController.java`)
- REST endpoints for profile update request management
- Permission-protected endpoints using `@RequiresPermission`
- Supports submission, viewing, approval, and rejection

### Database Tables

- **user_profiles**: Stores approved user profile data
- **profile_update_requests**: Stores pending and processed update requests
- **audit_logs**: Logs all workflow actions

## Workflow

### 1. Submission Flow

```
User → Submit Request → Request Stored (PENDING) → Audit Log → Email Notification
```

**Steps:**
1. User submits profile update request via `POST /profile-update-requests`
2. Request is stored with status `PENDING`
3. Old and new values are encrypted and stored
4. Audit log entry created
5. Email notification sent (integration-ready)

### 2. Approval Flow

```
Approver → View Pending Requests → Approve Request → Update Profile → Audit Log → Email Notification
```

**Steps:**
1. Approver (with `APPROVE_PROFILE` permission) views pending requests
2. Approver reviews and approves request
3. User profile is updated with requested changes
4. Request status changed to `APPROVED`
5. Audit log entry created
6. Email notification sent to user

### 3. Rejection Flow

```
Approver → View Pending Requests → Reject Request → Audit Log → Email Notification
```

**Steps:**
1. Approver views pending requests
2. Approver rejects request with notes
3. Request status changed to `REJECTED`
4. Audit log entry created
5. Email notification sent to user with rejection reason

## API Endpoints

### Submit Profile Update Request
```
POST /api/profile-update-requests
Authorization: Bearer <token>
Content-Type: application/json

Request Body:
{
  "requestType": "CREATE|UPDATE|DELETE",
  "fieldName": "firstName",  // Optional for UPDATE
  "firstName": "John",
  "lastName": "Doe",
  "phone": "1234567890",
  "email": "john@example.com",
  "reason": "Updating contact information"
}

Response: 200 OK
{
  "id": 1,
  "userId": 1,
  "status": "PENDING",
  "requestType": "UPDATE",
  ...
}
```

### Get Pending Requests (Requires APPROVE_PROFILE)
```
GET /api/profile-update-requests/pending
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "id": 1,
    "userId": 1,
    "userEmployeeId": "EMP001",
    "status": "PENDING",
    ...
  }
]
```

### Get My Requests
```
GET /api/profile-update-requests/my-requests
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "id": 1,
    "status": "PENDING",
    ...
  }
]
```

### Approve Request (Requires APPROVE_PROFILE)
```
POST /api/profile-update-requests/approve
Authorization: Bearer <token>
Content-Type: application/json

Request Body:
{
  "requestId": 1,
  "action": "APPROVE",
  "reviewNotes": "Approved - information verified"
}

Response: 200 OK
{
  "id": 1,
  "status": "APPROVED",
  "reviewedBy": 2,
  "reviewedAt": "2024-01-15T10:30:00",
  ...
}
```

### Reject Request (Requires APPROVE_PROFILE)
```
POST /api/profile-update-requests/reject
Authorization: Bearer <token>
Content-Type: application/json

Request Body:
{
  "requestId": 1,
  "action": "REJECT",
  "reviewNotes": "Rejected - insufficient documentation"
}

Response: 200 OK
{
  "id": 1,
  "status": "REJECTED",
  "reviewedBy": 2,
  "reviewedAt": "2024-01-15T10:30:00",
  "reviewNotes": "Rejected - insufficient documentation",
  ...
}
```

## Permission Requirements

- **Submit Request**: Any authenticated user
- **View Pending Requests**: `APPROVE_PROFILE` permission
- **Approve/Reject Requests**: `APPROVE_PROFILE` permission
- **View My Requests**: Any authenticated user (own requests only)

## Security Features

1. **Permission-Based Authorization**: Uses `@RequiresPermission` annotation
2. **Data Encryption**: Sensitive fields (phone, email) are encrypted
3. **Audit Trail**: All actions are logged with full context
4. **Transactional Safety**: All operations are transactional
5. **Input Validation**: DTOs use Jakarta validation annotations

## Data Flow

### Request Submission
1. User submits request with profile data
2. Sensitive data (phone, email) is encrypted
3. Request stored with encrypted old/new values
4. Status set to `PENDING`

### Request Approval
1. Approver reviews request
2. Encrypted data is decrypted and applied to profile
3. Profile updated with new values
4. Request status updated to `APPROVED`
5. Profile `approvedBy` and `approvedAt` fields updated

### Request Rejection
1. Approver reviews request
2. Request status updated to `REJECTED`
3. Review notes stored
4. Profile remains unchanged

## Audit Logging

All actions are logged in `audit_logs` table with:
- Action type (CREATE, APPROVE, REJECT)
- Entity type (PROFILE_UPDATE_REQUEST)
- Entity ID (request ID)
- Old and new values (JSON)
- User information
- IP address and user agent
- Request URL and method
- Status (SUCCESS/FAILURE)

## Email Notifications

The `EmailNotificationService` provides integration points for:
- **Profile Update Submitted**: Sent when user submits request
- **Profile Update Approved**: Sent when request is approved
- **Profile Update Rejected**: Sent when request is rejected (includes reason)

**Integration Steps:**
1. Load email template from `email_templates` table
2. Replace template variables with user/request data
3. Send email via email service
4. Log to `email_logs` table

## Error Handling

- **Invalid Request**: Returns 400 Bad Request
- **Permission Denied**: Returns 403 Forbidden
- **Request Not Found**: Returns 404 Not Found
- **Invalid State**: Returns 400 Bad Request (e.g., approving non-pending request)
- **Transaction Failures**: Rolled back automatically

## Extensibility

The workflow is designed to be extensible:

1. **Additional Approval Levels**: Can add multi-level approval
2. **Custom Validations**: Can add business rule validations
3. **Notification Channels**: Can extend beyond email (SMS, push notifications)
4. **Request Types**: Can add new request types beyond CREATE/UPDATE/DELETE
5. **Field-Level Approval**: Can implement field-specific approval workflows

## Best Practices

1. **Always Check Permissions**: Use `@RequiresPermission` or programmatic checks
2. **Use Transactions**: All state changes should be transactional
3. **Log Everything**: All actions should be audited
4. **Encrypt Sensitive Data**: Phone, email, and other PII should be encrypted
5. **Validate Input**: Use Jakarta validation on DTOs
6. **Handle Errors Gracefully**: Don't expose internal errors to users
7. **Send Notifications**: Keep users informed of request status

## Testing

Example test scenarios:

```java
@Test
@WithMockUser(authorities = "PERMISSION_APPROVE_PROFILE")
void testApproveRequest() {
    // Test approval flow
}

@Test
void testSubmitRequest() {
    // Test submission flow
}

@Test
@WithMockUser(authorities = "PERMISSION_APPROVE_PROFILE")
void testRejectRequest() {
    // Test rejection flow
}
```

## Future Enhancements

1. **Bulk Approval**: Approve multiple requests at once
2. **Request History**: View complete history of profile changes
3. **Auto-Approval**: Auto-approve low-risk changes
4. **SLA Tracking**: Track approval times and SLA compliance
5. **Notifications Dashboard**: Centralized notification management
6. **Mobile Support**: Mobile-optimized approval interface
