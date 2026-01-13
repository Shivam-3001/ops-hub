-- ============================================
-- Ops Hub - Direct User INSERT Statements
-- ============================================
-- Run this script in your MS SQL Server database
-- 
-- STEP 1: First, get BCrypt hashes by calling:
--   http://localhost:8080/api/auth/generate-hash?password=admin123
--   http://localhost:8080/api/auth/generate-hash?password=password123
-- 
-- STEP 2: Replace the hash placeholders below with the actual hashes
-- STEP 3: Run this script

USE ops_hub;
GO

-- Get or create area_id (required for users)
DECLARE @AreaId BIGINT;
DECLARE @MeerutAreaId BIGINT;

-- Try to find existing areas
SELECT TOP 1 @AreaId = id FROM areas ORDER BY id;

-- If no areas exist, create minimal hierarchy
IF @AreaId IS NULL
BEGIN
    -- Cluster
    IF NOT EXISTS (SELECT 1 FROM clusters WHERE code = 'BUP')
        INSERT INTO clusters (code, name, description, active, created_at, updated_at)
        VALUES ('BUP', 'Bihar UP', 'Cluster', 1, GETDATE(), GETDATE());
    
    DECLARE @ClusterId BIGINT = (SELECT id FROM clusters WHERE code = 'BUP');
    
    -- Circle
    IF NOT EXISTS (SELECT 1 FROM circles WHERE code = 'UP')
        INSERT INTO circles (code, name, description, cluster_id, active, created_at, updated_at)
        VALUES ('UP', 'Uttar Pradesh', 'Circle', @ClusterId, 1, GETDATE(), GETDATE());
    
    DECLARE @CircleId BIGINT = (SELECT id FROM circles WHERE code = 'UP');
    
    -- Zone
    IF NOT EXISTS (SELECT 1 FROM zones WHERE code = 'GZB')
        INSERT INTO zones (code, name, description, circle_id, active, created_at, updated_at)
        VALUES ('GZB', 'Ghaziabad', 'Zone', @CircleId, 1, GETDATE(), GETDATE());
    
    DECLARE @ZoneId BIGINT = (SELECT id FROM zones WHERE code = 'GZB');
    
    -- Area (Behrampur)
    IF NOT EXISTS (SELECT 1 FROM areas WHERE code = 'BHR')
        INSERT INTO areas (code, name, description, zone_id, active, created_at, updated_at)
        VALUES ('BHR', 'Behrampur', 'Area', @ZoneId, 1, GETDATE(), GETDATE());
    
    -- Area (Meerut)
    IF NOT EXISTS (SELECT 1 FROM areas WHERE code = 'MRT')
        INSERT INTO areas (code, name, description, zone_id, active, created_at, updated_at)
        VALUES ('MRT', 'Meerut', 'Area', @ZoneId, 1, GETDATE(), GETDATE());
END

SET @AreaId = (SELECT id FROM areas WHERE code = 'BHR');
SET @MeerutAreaId = (SELECT id FROM areas WHERE code = 'MRT');
IF @MeerutAreaId IS NULL SET @MeerutAreaId = @AreaId;

-- ============================================
-- INSERT USERS
-- ============================================
-- IMPORTANT: Get BCrypt hashes first from:
-- GET http://localhost:8080/api/auth/generate-hash?password=admin123
-- GET http://localhost:8080/api/auth/generate-hash?password=password123
--
-- Then replace HASH_PLACEHOLDER_ADMIN123 and HASH_PLACEHOLDER_PASSWORD123 below

-- EMP004 - Admin (password: admin123)
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP004')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP004',
        'admin',
        '$2a$12$OUaQ2NQafXckT/mHz1F4YOz9R4QHve2hKBfyhLsB8vbqgbJhzWb1O', 
        'admin@example.com',
        'Admin User',
        '9876543213',
        'ADMIN',
        'ADMIN',
        @AreaId,
        1,
        0,
        GETDATE(),
        GETDATE()
    );
END

-- EMP001 - Area Lead (password: password123)
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP001')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP001',
        'shivam',
        '$2a$12$YYkT66reMqxChvJ28SCdpOd07hzjPOeZOJq5rYY5X7IWYB97gYls.', -- Replace with hash from /api/auth/generate-hash?password=password123
        'shivam@example.com',
        'Shivam Kumar',
        '9876543210',
        'AREA_LEAD',
        'MANAGER',
        @AreaId,
        1,
        0,
        GETDATE(),
        GETDATE()
    );
END

-- EMP002 - Zone Lead (password: password123)
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP002')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP002',
        'rahul',
        '$2a$12$YYkT66reMqxChvJ28SCdpOd07hzjPOeZOJq5rYY5X7IWYB97gYls.', -- Same hash as password123
        'rahul@example.com',
        'Rahul Sharma',
        '9876543211',
        'ZONE_LEAD',
        'MANAGER',
        @AreaId,
        1,
        0,
        GETDATE(),
        GETDATE()
    );
END

-- EMP003 - Circle Lead (password: password123)
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP003')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP003',
        'priya',
        '$2a$12$YYkT66reMqxChvJ28SCdpOd07hzjPOeZOJq5rYY5X7IWYB97gYls.', -- Same hash as password123
        'priya@example.com',
        'Priya Singh',
        '9876543212',
        'CIRCLE_LEAD',
        'MANAGER',
        @AreaId,
        1,
        0,
        GETDATE(),
        GETDATE()
    );
END

-- EMP005 - Analyst (password: password123)
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP005')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP005',
        'analyst1',
        '$2a$12$YYkT66reMqxChvJ28SCdpOd07hzjPOeZOJq5rYY5X7IWYB97gYls.', -- Same hash as password123
        'analyst1@example.com',
        'Analyst One',
        '9876543214',
        'ANALYST',
        'ANALYST',
        @MeerutAreaId,
        1,
        0,
        GETDATE(),
        GETDATE()
    );
END

-- EMP006 - Zone Lead (password: password123)
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP006')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP006',
        'manager1',
        '$2a$12$YYkT66reMqxChvJ28SCdpOd07hzjPOeZOJq5rYY5X7IWYB97gYls.', -- Same hash as password123
        'manager1@example.com',
        'Manager One',
        '9876543215',
        'ZONE_LEAD',
        'MANAGER',
        @MeerutAreaId,
        1,
        0,
        GETDATE(),
        GETDATE()
    );
END

-- Verify users were created
SELECT employee_id, username, full_name, user_type, role, active 
FROM users 
WHERE employee_id IN ('EMP001', 'EMP002', 'EMP003', 'EMP004', 'EMP005', 'EMP006')
ORDER BY employee_id;

PRINT 'Users inserted! Remember to replace HASH_PLACEHOLDER values with actual BCrypt hashes.';
GO
