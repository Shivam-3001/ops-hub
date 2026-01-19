-- ============================================
-- Ops Hub - Demo Customer Data (Workflow + Notifications)
-- ============================================
-- Run this script in your MS SQL Server database.
-- It inserts demo customers, allocations, visits, payments, and notifications.

USE ops_hub;
GO

DECLARE @Now DATETIME2 = SYSUTCDATETIME();

-- Resolve demo users by employee_id
DECLARE @CircleHeadId BIGINT = (SELECT id FROM users WHERE employee_id = 'EMP003');
DECLARE @AreaHeadId BIGINT = (SELECT id FROM users WHERE employee_id = 'EMP001');
DECLARE @StoreHeadId BIGINT = (SELECT id FROM users WHERE employee_id = 'EMP006');
DECLARE @AgentId BIGINT = (SELECT id FROM users WHERE employee_id = 'EMP005');

-- Resolve areas
DECLARE @AreaId BIGINT = (SELECT id FROM areas WHERE code = 'SECTOR_63');
DECLARE @SecondaryAreaId BIGINT = (SELECT id FROM areas WHERE code = 'INDIRAPURAM');
IF @SecondaryAreaId IS NULL SET @SecondaryAreaId = @AreaId;

-- Insert demo customers
IF NOT EXISTS (SELECT 1 FROM customers WHERE customer_code = 'CUST-DEMO-001')
BEGIN
    INSERT INTO customers (
        customer_code, first_name, last_name, phone_encrypted, email_encrypted,
        address_line1, city, state, postal_code, store_name, pending_amount,
        area_id, status, created_by, created_at, updated_at
    ) VALUES (
        'CUST-DEMO-001', 'Rohan', 'Sharma',
        'ENCRYPTED_PHONE_001', 'ENCRYPTED_EMAIL_001',
        'A-12 Sector 63', 'Noida', 'Uttar Pradesh', '201301', 'Store A', 12000.50,
        @AreaId, 'ASSIGNED', @CircleHeadId, @Now, @Now
    );
END

IF NOT EXISTS (SELECT 1 FROM customers WHERE customer_code = 'CUST-DEMO-002')
BEGIN
    INSERT INTO customers (
        customer_code, first_name, last_name, phone_encrypted, email_encrypted,
        address_line1, city, state, postal_code, store_name, pending_amount,
        area_id, status, created_by, created_at, updated_at
    ) VALUES (
        'CUST-DEMO-002', 'Anita', 'Verma',
        'ENCRYPTED_PHONE_002', 'ENCRYPTED_EMAIL_002',
        'B-44 Indirapuram', 'Ghaziabad', 'Uttar Pradesh', '201014', 'Store B', 0.00,
        @SecondaryAreaId, 'PAID', @CircleHeadId, DATEADD(DAY,-5,@Now), @Now
    );
END

IF NOT EXISTS (SELECT 1 FROM customers WHERE customer_code = 'CUST-DEMO-003')
BEGIN
    INSERT INTO customers (
        customer_code, first_name, last_name, phone_encrypted, email_encrypted,
        address_line1, city, state, postal_code, store_name, pending_amount,
        area_id, status, created_by, created_at, updated_at
    ) VALUES (
        'CUST-DEMO-003', 'Mohit', 'Singh',
        'ENCRYPTED_PHONE_003', 'ENCRYPTED_EMAIL_003',
        'C-21 Sector 62', 'Noida', 'Uttar Pradesh', '201309', 'Store C', 8500.00,
        @AreaId, 'PAYMENT_PENDING', @CircleHeadId, DATEADD(DAY,-10,@Now), @Now
    );
END

DECLARE @Customer1 BIGINT = (SELECT id FROM customers WHERE customer_code = 'CUST-DEMO-001');
DECLARE @Customer2 BIGINT = (SELECT id FROM customers WHERE customer_code = 'CUST-DEMO-002');
DECLARE @Customer3 BIGINT = (SELECT id FROM customers WHERE customer_code = 'CUST-DEMO-003');

-- Allocations
IF @Customer1 IS NOT NULL AND @AreaHeadId IS NOT NULL
    IF NOT EXISTS (SELECT 1 FROM customer_allocations WHERE customer_id = @Customer1 AND user_id = @AreaHeadId AND status = 'ACTIVE')
        INSERT INTO customer_allocations (customer_id, user_id, role_code, allocation_type, status, allocated_by, allocated_at)
        VALUES (@Customer1, @AreaHeadId, 'AREA_HEAD', 'UPLOAD', 'ACTIVE', @CircleHeadId, DATEADD(DAY,-2,@Now));

IF @Customer2 IS NOT NULL AND @AgentId IS NOT NULL
    IF NOT EXISTS (SELECT 1 FROM customer_allocations WHERE customer_id = @Customer2 AND user_id = @AgentId AND status = 'ACTIVE')
        INSERT INTO customer_allocations (customer_id, user_id, role_code, allocation_type, status, allocated_by, allocated_at)
        VALUES (@Customer2, @AgentId, 'AGENT', 'PRIMARY', 'ACTIVE', @AreaHeadId, DATEADD(DAY,-4,@Now));

IF @Customer3 IS NOT NULL AND @AgentId IS NOT NULL
    IF NOT EXISTS (SELECT 1 FROM customer_allocations WHERE customer_id = @Customer3 AND user_id = @AgentId AND status = 'ACTIVE')
        INSERT INTO customer_allocations (customer_id, user_id, role_code, allocation_type, status, allocated_by, allocated_at)
        VALUES (@Customer3, @AgentId, 'AGENT', 'PRIMARY', 'ACTIVE', @AreaHeadId, DATEADD(DAY,-9,@Now));

-- Visits
IF @Customer2 IS NOT NULL AND @AgentId IS NOT NULL
    IF NOT EXISTS (SELECT 1 FROM customer_visits WHERE customer_id = @Customer2 AND user_id = @AgentId)
        INSERT INTO customer_visits (customer_id, user_id, visit_date, visit_type, purpose, notes, visit_status, created_at, updated_at)
        VALUES (@Customer2, @AgentId, DATEADD(DAY,-3,@Now), 'FOLLOW_UP', 'Collection', 'Visited and confirmed payment.',
                'COMPLETED', DATEADD(DAY,-3,@Now), DATEADD(DAY,-3,@Now));

-- Payments
IF @Customer2 IS NOT NULL AND @AgentId IS NOT NULL
    IF NOT EXISTS (SELECT 1 FROM payments WHERE payment_reference = 'PAY-DEMO-002')
        INSERT INTO payments (payment_reference, customer_id, user_id, amount, currency, payment_method,
                              payment_status, payment_date, transaction_id, created_at, updated_at)
        VALUES ('PAY-DEMO-002', @Customer2, @AgentId, 6500.00, 'INR', 'UPI', 'SUCCESS',
                DATEADD(DAY,-2,@Now), 'TXN-DEMO-002', DATEADD(DAY,-2,@Now), DATEADD(DAY,-2,@Now));

IF @Customer3 IS NOT NULL AND @AgentId IS NOT NULL
    IF NOT EXISTS (SELECT 1 FROM payments WHERE payment_reference = 'PAY-DEMO-003')
        INSERT INTO payments (payment_reference, customer_id, user_id, amount, currency, payment_method,
                              payment_status, created_at, updated_at)
        VALUES ('PAY-DEMO-003', @Customer3, @AgentId, 8500.00, 'INR', 'UPI', 'INITIATED',
                DATEADD(DAY,-9,@Now), DATEADD(DAY,-9,@Now));

-- Notifications (in-app)
IF @AreaHeadId IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM app_notifications WHERE user_id = @AreaHeadId AND title = 'New customer assigned'
)
    INSERT INTO app_notifications (user_id, notification_type, title, message, entity_type, entity_id, severity, created_at)
    VALUES (@AreaHeadId, 'ASSIGNMENT', 'New customer assigned', 'Customer CUST-DEMO-001 assigned to your area.',
            'CUSTOMER', @Customer1, 'INFO', @Now);

IF @AgentId IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM app_notifications WHERE user_id = @AgentId AND title = 'Payment completed'
)
    INSERT INTO app_notifications (user_id, notification_type, title, message, entity_type, entity_id, severity, created_at)
    VALUES (@AgentId, 'PAYMENT', 'Payment completed', 'Payment PAY-DEMO-002 completed for customer CUST-DEMO-002.',
            'PAYMENT', (SELECT id FROM payments WHERE payment_reference = 'PAY-DEMO-002'), 'INFO', @Now);

IF @CircleHeadId IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM app_notifications WHERE user_id = @CircleHeadId AND notification_type = 'ESCALATION'
)
    INSERT INTO app_notifications (user_id, notification_type, title, message, entity_type, entity_id, severity, created_at)
    VALUES (@CircleHeadId, 'ESCALATION', 'Payment pending escalation',
            'Payment PAY-DEMO-003 pending for 7+ days (Customer CUST-DEMO-003).',
            'PAYMENT', (SELECT id FROM payments WHERE payment_reference = 'PAY-DEMO-003'), 'WARNING', @Now);

PRINT 'Demo data inserted. Refresh customers and notifications in the UI.';
GO
