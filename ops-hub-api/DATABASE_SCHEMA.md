# Ops Hub Database Schema Documentation

## Overview
This document describes the complete database schema for the Ops Hub enterprise application. The schema is organized into 10 functional sections with 24 tables total.

## Schema Location
- **SQL DDL**: `src/main/resources/db/schema.sql`
- **JPA Entities**: `src/main/java/com/company/ops_hub_api/domain/`

## Table Summary

### Section 1: Authentication & Access Control (5 tables)
1. **users** - Application users (existing, updated with relationships)
2. **roles** - User roles (Admin, Lead, Agent, etc.)
3. **permissions** - Fine-grained permissions
4. **role_permissions** - Many-to-many mapping between roles and permissions
5. **user_sessions** - Login sessions, devices, and IP addresses

### Section 2: User Profile & Approval Workflow (2 tables)
6. **user_profiles** - Approved user profile data
7. **profile_update_requests** - Pending profile updates requiring approval

### Section 3: Customer Management (3 tables)
8. **customers** - Customer master data
9. **customer_uploads** - Excel upload metadata
10. **customer_upload_errors** - Row-level errors from Excel uploads

### Section 4: Customer Allocation & Work Assignment (1 table)
11. **customer_allocations** - Assigns customers to users by role

### Section 5: Field Visit & Review System (2 tables)
12. **customer_visits** - Field visit tracking with geo-location
13. **customer_reviews** - Review and rating per visit

### Section 6: Payments & UPI Integration (2 tables)
14. **payments** - Payment transactions with UPI support
15. **payment_events** - Payment lifecycle events and gateway callbacks

### Section 7: Automated Email System (2 tables)
16. **email_templates** - Reusable email templates
17. **email_logs** - Sent emails and delivery status

### Section 8: AI Agent & Intelligence (2 tables)
18. **ai_conversations** - AI chat history and context
19. **ai_actions** - AI-triggered actions and execution status

### Section 9: Reports & MIS (2 tables)
20. **reports** - Report definitions and metadata
21. **report_exports** - Export history and formats

### Section 10: Audit & System Configuration (3 tables)
22. **audit_logs** - Sensitive user and system actions
23. **app_settings** - Application-level configuration
24. **feature_flags** - Dynamic feature enablement

## Key Design Principles

### 1. Security
- **Encryption**: Sensitive fields (email, phone) use `_encrypted` suffix
- **Password Hashing**: Passwords stored as BCrypt hashes
- **Session Management**: Token hashing and expiration tracking
- **Audit Trail**: Comprehensive audit logging

### 2. Data Integrity
- **Foreign Keys**: All relationships enforced at database level
- **Unique Constraints**: Business keys (codes, references) are unique
- **Cascade Deletes**: Appropriate cascading for data consistency
- **Check Constraints**: Data validation (e.g., rating 1-5)

### 3. Performance
- **Indexes**: Strategic indexes on foreign keys and frequently queried columns
- **Lazy Loading**: JPA entities use LAZY fetching by default
- **Batch Operations**: Hibernate batch configuration enabled

### 4. Extensibility
- **JSON Fields**: Flexible storage for dynamic data (parameters, filters, context)
- **Status Fields**: Enum-like status columns for workflow management
- **Timestamps**: Created/updated timestamps on all entities

## Entity Relationships

### User Entity Relationships
- One-to-Many: `UserSession`, `ProfileUpdateRequest`, `CustomerAllocation`, `CustomerVisit`, `CustomerReview`, `AiConversation`, `AuditLog`
- One-to-One: `UserProfile`
- Many-to-One: `Area` (hierarchical location)

### Customer Entity Relationships
- One-to-Many: `CustomerAllocation`, `CustomerVisit`, `Payment`
- Many-to-One: `Area` (hierarchical location)

### Hierarchical Location Structure
- `Cluster` → `Circle` → `Zone` → `Area` → `User` / `Customer`

## Important Notes

### 1. User Entity
- Primary key: `id` (BIGINT, auto-generated)
- Login identifier: `employeeId` (String, unique)
- Status: `active` (Boolean)
- Two-factor: `twoFactorEnabled` (Boolean)

### 2. Encryption
- Use `EncryptionUtil` for encrypting/decrypting sensitive data
- Fields with `_encrypted` suffix should be encrypted before storage
- Encryption secret configured in `application.yaml`

### 3. JWT Sessions
- Sessions tracked in `user_sessions` table
- Token hash stored (not full token)
- Expiration and revocation supported

### 4. Status Fields
- Use consistent status values (e.g., "ACTIVE", "INACTIVE", "PENDING", "COMPLETED")
- Status enums can be created for type safety

### 5. JSON Fields
- Stored as `NVARCHAR(MAX)` in SQL Server
- Use JSON libraries (Jackson) for serialization/deserialization
- Examples: `parameters`, `filters_applied`, `context_data`, `event_data`

## Database Migration

### Using Hibernate DDL Auto
Currently configured as `update` in `application.yaml`:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

### Manual SQL Execution
For production, execute `schema.sql` manually or use a migration tool like Flyway or Liquibase.

## Next Steps

1. **Create Repositories**: JPA repositories for all new entities
2. **Create Services**: Business logic services for each functional area
3. **Create DTOs**: Data transfer objects for API responses
4. **Create Controllers**: REST endpoints for each resource
5. **Add Validation**: Bean validation annotations on entities
6. **Add Enums**: Convert status strings to enums for type safety
7. **Add Tests**: Unit and integration tests for entities and repositories

## Entity List

All JPA entities are located in `com.company.ops_hub_api.domain`:

1. `User` (updated)
2. `Role`
3. `Permission`
4. `RolePermission`
5. `UserSession`
6. `UserProfile`
7. `ProfileUpdateRequest`
8. `Customer`
9. `CustomerUpload`
10. `CustomerUploadError`
11. `CustomerAllocation`
12. `CustomerVisit`
13. `CustomerReview`
14. `Payment`
15. `PaymentEvent`
16. `EmailTemplate`
17. `EmailLog`
18. `AiConversation`
19. `AiAction`
20. `Report`
21. `ReportExport`
22. `AuditLog`
23. `AppSetting`
24. `FeatureFlag`

Plus existing hierarchical entities:
- `Cluster`
- `Circle`
- `Zone`
- `Area`
