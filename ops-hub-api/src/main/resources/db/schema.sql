-- ============================================================
-- Ops Hub Enterprise Application - Database Schema
-- MS SQL Server
-- ============================================================

-- ============================================================
-- SECTION 1: Authentication & Access Control
-- ============================================================

-- Users table (already exists, but adding missing columns if needed)
-- Note: This assumes the users table structure from existing implementation
-- Additional columns may be added via ALTER statements if needed

-- User Roles (Many-to-Many between Users and Roles)
CREATE TABLE user_roles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    UNIQUE (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- Roles table
CREATE TABLE roles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code NVARCHAR(50) NOT NULL UNIQUE,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(500),
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- Permissions table
CREATE TABLE permissions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code NVARCHAR(100) NOT NULL UNIQUE,
    name NVARCHAR(200) NOT NULL,
    description NVARCHAR(500),
    resource NVARCHAR(100) NOT NULL,
    action NVARCHAR(50) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE()
);

-- Role Permissions (Many-to-Many)
CREATE TABLE role_permissions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    UNIQUE (role_id, permission_id)
);

-- User Sessions table
CREATE TABLE user_sessions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash NVARCHAR(255) NOT NULL,
    device_type NVARCHAR(50),
    device_name NVARCHAR(200),
    browser NVARCHAR(100),
    ip_address NVARCHAR(45),
    user_agent NVARCHAR(500),
    login_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    last_activity_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    expires_at DATETIME2 NOT NULL,
    revoked BIT NOT NULL DEFAULT 0,
    revoked_at DATETIME2,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_token_hash ON user_sessions(token_hash);
CREATE INDEX idx_user_sessions_expires_at ON user_sessions(expires_at);

-- ============================================================
-- SECTION 2: User Profile & Approval Workflow
-- ============================================================

-- User Profiles table
CREATE TABLE user_profiles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    first_name NVARCHAR(100),
    last_name NVARCHAR(100),
    middle_name NVARCHAR(100),
    date_of_birth DATE,
    gender NVARCHAR(20),
    phone_encrypted NVARCHAR(500),
    alternate_phone_encrypted NVARCHAR(500),
    email_encrypted NVARCHAR(500),
    address_line1 NVARCHAR(500),
    address_line2 NVARCHAR(500),
    city NVARCHAR(100),
    state NVARCHAR(100),
    postal_code NVARCHAR(20),
    country NVARCHAR(100),
    profile_picture_url NVARCHAR(500),
    emergency_contact_name NVARCHAR(200),
    emergency_contact_phone_encrypted NVARCHAR(500),
    approved_by BIGINT,
    approved_at DATETIME2,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (approved_by) REFERENCES users(id)
);

-- Profile Update Requests table
CREATE TABLE profile_update_requests (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    profile_id BIGINT,
    request_type NVARCHAR(50) NOT NULL, -- CREATE, UPDATE, DELETE
    field_name NVARCHAR(100),
    old_value_encrypted NVARCHAR(MAX),
    new_value_encrypted NVARCHAR(MAX),
    reason NVARCHAR(1000),
    status NVARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    requested_by BIGINT NOT NULL,
    requested_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    reviewed_by BIGINT,
    reviewed_at DATETIME2,
    review_notes NVARCHAR(1000),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (profile_id) REFERENCES user_profiles(id) ON DELETE CASCADE,
    FOREIGN KEY (requested_by) REFERENCES users(id),
    FOREIGN KEY (reviewed_by) REFERENCES users(id)
);

CREATE INDEX idx_profile_update_requests_user_id ON profile_update_requests(user_id);
CREATE INDEX idx_profile_update_requests_status ON profile_update_requests(status);

-- ============================================================
-- SECTION 3: Customer Management
-- ============================================================

-- Customers table
CREATE TABLE customers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_code NVARCHAR(50) NOT NULL UNIQUE,
    first_name NVARCHAR(100) NOT NULL,
    last_name NVARCHAR(100),
    middle_name NVARCHAR(100),
    phone_encrypted NVARCHAR(500) NOT NULL,
    alternate_phone_encrypted NVARCHAR(500),
    email_encrypted NVARCHAR(500),
    address_line1 NVARCHAR(500),
    address_line2 NVARCHAR(500),
    city NVARCHAR(100),
    state NVARCHAR(100),
    postal_code NVARCHAR(20),
    country NVARCHAR(100),
    area_id BIGINT,
    status NVARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, SUSPENDED
    notes NVARCHAR(2000),
    created_by BIGINT,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    FOREIGN KEY (area_id) REFERENCES areas(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_customers_customer_code ON customers(customer_code);
CREATE INDEX idx_customers_area_id ON customers(area_id);
CREATE INDEX idx_customers_status ON customers(status);

-- Customer Uploads table
CREATE TABLE customer_uploads (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    file_name NVARCHAR(500) NOT NULL,
    file_path NVARCHAR(1000),
    file_size BIGINT,
    total_rows INT NOT NULL DEFAULT 0,
    successful_rows INT NOT NULL DEFAULT 0,
    failed_rows INT NOT NULL DEFAULT 0,
    upload_status NVARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    uploaded_by BIGINT NOT NULL,
    uploaded_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    processed_at DATETIME2,
    error_summary NVARCHAR(MAX),
    FOREIGN KEY (uploaded_by) REFERENCES users(id)
);

CREATE INDEX idx_customer_uploads_uploaded_by ON customer_uploads(uploaded_by);
CREATE INDEX idx_customer_uploads_upload_status ON customer_uploads(upload_status);

-- Customer Upload Errors table
CREATE TABLE customer_upload_errors (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    upload_id BIGINT NOT NULL,
    row_number INT NOT NULL,
    column_name NVARCHAR(100),
    error_code NVARCHAR(50),
    error_message NVARCHAR(1000) NOT NULL,
    row_data NVARCHAR(MAX),
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    FOREIGN KEY (upload_id) REFERENCES customer_uploads(id) ON DELETE CASCADE
);

CREATE INDEX idx_customer_upload_errors_upload_id ON customer_upload_errors(upload_id);

-- ============================================================
-- SECTION 4: Customer Allocation & Work Assignment
-- ============================================================

-- Customer Allocations table
CREATE TABLE customer_allocations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_code NVARCHAR(50) NOT NULL,
    allocation_type NVARCHAR(50) NOT NULL, -- PRIMARY, SECONDARY, TEMPORARY
    status NVARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, TRANSFERRED
    allocated_by BIGINT NOT NULL,
    allocated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    deallocated_at DATETIME2,
    deallocation_reason NVARCHAR(500),
    notes NVARCHAR(1000),
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (allocated_by) REFERENCES users(id)
);

CREATE INDEX idx_customer_allocations_customer_id ON customer_allocations(customer_id);
CREATE INDEX idx_customer_allocations_user_id ON customer_allocations(user_id);
CREATE INDEX idx_customer_allocations_status ON customer_allocations(status);

-- ============================================================
-- SECTION 5: Field Visit & Review System
-- ============================================================

-- Customer Visits table
CREATE TABLE customer_visits (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    visit_date DATETIME2 NOT NULL,
    visit_type NVARCHAR(50), -- SCHEDULED, UNSCHEDULED, FOLLOW_UP
    purpose NVARCHAR(200),
    notes NVARCHAR(MAX),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    address NVARCHAR(500),
    visit_status NVARCHAR(50) NOT NULL DEFAULT 'COMPLETED', -- SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    scheduled_at DATETIME2,
    started_at DATETIME2,
    completed_at DATETIME2,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_customer_visits_customer_id ON customer_visits(customer_id);
CREATE INDEX idx_customer_visits_user_id ON customer_visits(user_id);
CREATE INDEX idx_customer_visits_visit_date ON customer_visits(visit_date);

-- Customer Reviews table
CREATE TABLE customer_reviews (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    visit_id BIGINT NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review_text NVARCHAR(2000),
    review_categories NVARCHAR(500), -- JSON array of categories
    is_positive BIT NOT NULL DEFAULT 1,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    FOREIGN KEY (visit_id) REFERENCES customer_visits(id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_customer_reviews_customer_id ON customer_reviews(customer_id);
CREATE INDEX idx_customer_reviews_user_id ON customer_reviews(user_id);
CREATE INDEX idx_customer_reviews_rating ON customer_reviews(rating);

-- ============================================================
-- SECTION 6: Payments & UPI Integration
-- ============================================================

-- Payments table
CREATE TABLE payments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    payment_reference NVARCHAR(100) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    user_id BIGINT,
    amount DECIMAL(18, 2) NOT NULL,
    currency NVARCHAR(3) NOT NULL DEFAULT 'INR',
    payment_method NVARCHAR(50) NOT NULL, -- UPI, CASH, CARD, BANK_TRANSFER
    upi_id NVARCHAR(200),
    transaction_id NVARCHAR(200),
    gateway_transaction_id NVARCHAR(200),
    payment_status NVARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, SUCCESS, FAILED, REFUNDED
    payment_date DATETIME2,
    failure_reason NVARCHAR(500),
    gateway_response NVARCHAR(MAX),
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_payments_customer_id ON payments(customer_id);
CREATE INDEX idx_payments_payment_reference ON payments(payment_reference);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX idx_payments_payment_status ON payments(payment_status);

-- Payment Events table
CREATE TABLE payment_events (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    event_type NVARCHAR(50) NOT NULL, -- INITIATED, CALLBACK_RECEIVED, PROCESSED, FAILED
    event_data NVARCHAR(MAX),
    gateway_name NVARCHAR(100),
    ip_address NVARCHAR(45),
    user_agent NVARCHAR(500),
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
);

CREATE INDEX idx_payment_events_payment_id ON payment_events(payment_id);
CREATE INDEX idx_payment_events_event_type ON payment_events(event_type);

-- ============================================================
-- SECTION 7: Automated Email System
-- ============================================================

-- Email Templates table
CREATE TABLE email_templates (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    template_code NVARCHAR(100) NOT NULL UNIQUE,
    name NVARCHAR(200) NOT NULL,
    subject NVARCHAR(500) NOT NULL,
    body_html NVARCHAR(MAX),
    body_text NVARCHAR(MAX),
    variables NVARCHAR(MAX), -- JSON array of variable names
    category NVARCHAR(100),
    active BIT NOT NULL DEFAULT 1,
    created_by BIGINT,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Email Logs table
CREATE TABLE email_logs (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    template_id BIGINT,
    template_code NVARCHAR(100),
    recipient_email_encrypted NVARCHAR(500) NOT NULL,
    recipient_name NVARCHAR(200),
    subject NVARCHAR(500) NOT NULL,
    body_html NVARCHAR(MAX),
    body_text NVARCHAR(MAX),
    email_status NVARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, SENT, DELIVERED, FAILED, BOUNCED
    sent_at DATETIME2,
    delivered_at DATETIME2,
    failed_at DATETIME2,
    failure_reason NVARCHAR(1000),
    provider_message_id NVARCHAR(200),
    provider_response NVARCHAR(MAX),
    sent_by BIGINT,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    FOREIGN KEY (template_id) REFERENCES email_templates(id),
    FOREIGN KEY (sent_by) REFERENCES users(id)
);

CREATE INDEX idx_email_logs_recipient_email ON email_logs(recipient_email_encrypted);
CREATE INDEX idx_email_logs_email_status ON email_logs(email_status);
CREATE INDEX idx_email_logs_sent_at ON email_logs(sent_at);

-- ============================================================
-- SECTION 8: AI Agent & Intelligence
-- ============================================================

-- AI Conversations table
CREATE TABLE ai_conversations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    conversation_id NVARCHAR(100) NOT NULL,
    title NVARCHAR(500),
    context_data NVARCHAR(MAX), -- JSON context
    model_name NVARCHAR(100),
    status NVARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, ARCHIVED, DELETED
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_ai_conversations_user_id ON ai_conversations(user_id);
CREATE INDEX idx_ai_conversations_conversation_id ON ai_conversations(conversation_id);

-- AI Actions table
CREATE TABLE ai_actions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    conversation_id BIGINT,
    action_type NVARCHAR(100) NOT NULL, -- DATA_QUERY, REPORT_GENERATION, RECOMMENDATION
    action_name NVARCHAR(200) NOT NULL,
    action_data NVARCHAR(MAX), -- JSON action parameters
    execution_status NVARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, EXECUTING, COMPLETED, FAILED
    result_data NVARCHAR(MAX), -- JSON result
    error_message NVARCHAR(2000),
    executed_at DATETIME2,
    completed_at DATETIME2,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE
);

CREATE INDEX idx_ai_actions_conversation_id ON ai_actions(conversation_id);
CREATE INDEX idx_ai_actions_execution_status ON ai_actions(execution_status);

-- ============================================================
-- SECTION 9: Reports & MIS
-- ============================================================

-- Reports table
CREATE TABLE reports (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    report_code NVARCHAR(100) NOT NULL UNIQUE,
    name NVARCHAR(200) NOT NULL,
    description NVARCHAR(1000),
    report_type NVARCHAR(50) NOT NULL, -- STANDARD, CUSTOM, SCHEDULED
    category NVARCHAR(100),
    query_sql NVARCHAR(MAX),
    parameters NVARCHAR(MAX), -- JSON parameters definition
    access_roles NVARCHAR(500), -- JSON array of role codes
    active BIT NOT NULL DEFAULT 1,
    created_by BIGINT,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Report Exports table
CREATE TABLE report_exports (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    report_id BIGINT NOT NULL,
    export_format NVARCHAR(50) NOT NULL, -- CSV, EXCEL, PDF, JSON
    file_path NVARCHAR(1000),
    file_name NVARCHAR(500),
    file_size BIGINT,
    parameters_used NVARCHAR(MAX), -- JSON parameters used
    filters_applied NVARCHAR(MAX), -- JSON filters
    record_count INT,
    export_status NVARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    exported_by BIGINT NOT NULL,
    exported_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    completed_at DATETIME2,
    error_message NVARCHAR(1000),
    FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE,
    FOREIGN KEY (exported_by) REFERENCES users(id)
);

CREATE INDEX idx_report_exports_report_id ON report_exports(report_id);
CREATE INDEX idx_report_exports_exported_by ON report_exports(exported_by);
CREATE INDEX idx_report_exports_export_status ON report_exports(export_status);

-- ============================================================
-- SECTION 10: Audit & System Configuration
-- ============================================================

-- Audit Logs table
CREATE TABLE audit_logs (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT,
    action_type NVARCHAR(100) NOT NULL, -- CREATE, UPDATE, DELETE, LOGIN, LOGOUT, EXPORT
    entity_type NVARCHAR(100) NOT NULL, -- USER, CUSTOMER, PAYMENT, etc.
    entity_id BIGINT,
    old_values NVARCHAR(MAX), -- JSON old values
    new_values NVARCHAR(MAX), -- JSON new values
    ip_address NVARCHAR(45),
    user_agent NVARCHAR(500),
    request_url NVARCHAR(1000),
    request_method NVARCHAR(10),
    status NVARCHAR(50), -- SUCCESS, FAILURE
    error_message NVARCHAR(2000),
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- App Settings table
CREATE TABLE app_settings (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    setting_key NVARCHAR(200) NOT NULL UNIQUE,
    setting_value NVARCHAR(MAX),
    setting_type NVARCHAR(50) NOT NULL, -- STRING, NUMBER, BOOLEAN, JSON
    category NVARCHAR(100),
    description NVARCHAR(1000),
    is_encrypted BIT NOT NULL DEFAULT 0,
    is_public BIT NOT NULL DEFAULT 0,
    updated_by BIGINT,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    FOREIGN KEY (updated_by) REFERENCES users(id)
);

-- Feature Flags table
CREATE TABLE feature_flags (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    flag_key NVARCHAR(200) NOT NULL UNIQUE,
    flag_name NVARCHAR(200) NOT NULL,
    description NVARCHAR(1000),
    enabled BIT NOT NULL DEFAULT 0,
    enabled_for_roles NVARCHAR(500), -- JSON array of role codes
    enabled_for_users NVARCHAR(MAX), -- JSON array of user IDs
    rollout_percentage INT DEFAULT 0 CHECK (rollout_percentage >= 0 AND rollout_percentage <= 100),
    created_by BIGINT,
    created_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_feature_flags_flag_key ON feature_flags(flag_key);
CREATE INDEX idx_feature_flags_enabled ON feature_flags(enabled);
