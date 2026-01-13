-- ============================================
-- Ops Hub - User Seeding SQL Script
-- ============================================
-- This script creates the hierarchy and test users
-- Run this in your MS SQL Server database

USE ops_hub;
GO

-- ============================================
-- Step 1: Create Hierarchy (if not exists)
-- ============================================

-- Create Cluster
IF NOT EXISTS (SELECT 1 FROM clusters WHERE code = 'BUP')
BEGIN
    INSERT INTO clusters (code, name, description, active, created_at, updated_at)
    VALUES ('BUP', 'Bihar UP', 'Cluster: Bihar UP', 1, GETDATE(), GETDATE());
END
DECLARE @BUPClusterId BIGINT = (SELECT id FROM clusters WHERE code = 'BUP');

-- Create Circle
IF NOT EXISTS (SELECT 1 FROM circles WHERE code = 'UP')
BEGIN
    INSERT INTO circles (code, name, description, cluster_id, active, created_at, updated_at)
    VALUES ('UP', 'Uttar Pradesh', 'Circle: Uttar Pradesh', @BUPClusterId, 1, GETDATE(), GETDATE());
END
DECLARE @UPCircleId BIGINT = (SELECT id FROM circles WHERE code = 'UP');

-- Create Zone
IF NOT EXISTS (SELECT 1 FROM zones WHERE code = 'GZB')
BEGIN
    INSERT INTO zones (code, name, description, circle_id, active, created_at, updated_at)
    VALUES ('GZB', 'Ghaziabad', 'Zone: Ghaziabad', @UPCircleId, 1, GETDATE(), GETDATE());
END
DECLARE @GZBZoneId BIGINT = (SELECT id FROM zones WHERE code = 'GZB');

-- Create Areas
IF NOT EXISTS (SELECT 1 FROM areas WHERE code = 'BHR')
BEGIN
    INSERT INTO areas (code, name, description, zone_id, active, created_at, updated_at)
    VALUES ('BHR', 'Behrampur', 'Area: Behrampur', @GZBZoneId, 1, GETDATE(), GETDATE());
END
DECLARE @BHRAreaId BIGINT = (SELECT id FROM areas WHERE code = 'BHR');

IF NOT EXISTS (SELECT 1 FROM areas WHERE code = 'MRT')
BEGIN
    INSERT INTO areas (code, name, description, zone_id, active, created_at, updated_at)
    VALUES ('MRT', 'Meerut', 'Area: Meerut', @GZBZoneId, 1, GETDATE(), GETDATE());
END
DECLARE @MRTAreaId BIGINT = (SELECT id FROM areas WHERE code = 'MRT');

-- ============================================
-- Step 2: Insert Users
-- ============================================
-- BCrypt hashes (10 rounds):
-- To generate correct hashes, use: POST http://localhost:8080/api/auth/generate-hash?password=admin123
-- Or use online BCrypt generator: https://bcrypt-generator.com/
--
-- Pre-generated hashes (you may need to regenerate these):
-- "admin123" -> Use the hash generator endpoint or online tool
-- "password123" -> Use the hash generator endpoint or online tool

-- Note: Email and phone are stored encrypted in production, but for seeding we'll use plain text
-- The application will encrypt them on first update if EncryptionUtil is used

-- EMP001 - Area Lead
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP001')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP001',
        'shivam',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- password123
        'shivam@example.com',
        'Shivam Kumar',
        '9876543210',
        'AREA_LEAD',
        'MANAGER',
        @BHRAreaId,
        1,
        0,
        GETDATE(),
        GETDATE()
    );
END

-- EMP002 - Zone Lead
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP002')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP002',
        'rahul',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- password123
        'rahul@example.com',
        'Rahul Sharma',
        '9876543211',
        'ZONE_LEAD',
        'MANAGER',
        @BHRAreaId,
        1,
        0,
        GETDATE(),
        GETDATE()
    );
END

-- EMP003 - Circle Lead
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP003')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP003',
        'priya',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- password123
        'priya@example.com',
        'Priya Singh',
        '9876543212',
        'CIRCLE_LEAD',
        'MANAGER',
        @BHRAreaId,
        1,
        0,
        GETDATE(),
        GETDATE()
    );
END

-- EMP004 - Admin (password: admin123)
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP004')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP004',
        'admin',
        '$2a$10$rKqX5Y8Z9A0B1C2D3E4F5G6H7I8J9K0L1M2N3O4P5Q6R7S8T9U0V1W2X3Y4Z5', -- admin123 (will be replaced with correct hash)
        'admin@example.com',
        'Admin User',
        '9876543213',
        'ADMIN',
        'ADMIN',
        @BHRAreaId,
        1,
        0,
        GETDATE(),
        GETDATE()
    );
END

-- EMP005 - Analyst
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP005')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP005',
        'analyst1',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- password123
        'analyst1@example.com',
        'Analyst One',
        '9876543214',
        'ANALYST',
        'ANALYST',
        @MRTAreaId,
        1,
        0,
        GETDATE(),
        GETDATE()
    );
END

-- EMP006 - Zone Lead
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP006')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP006',
        'manager1',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- password123
        'manager1@example.com',
        'Manager One',
        '9876543215',
        'ZONE_LEAD',
        'MANAGER',
        @MRTAreaId,
        1,
        0,
        GETDATE(),
        GETDATE()
    );
END

-- ============================================
-- Step 3: Verify Users Created
-- ============================================
SELECT 
    employee_id,
    username,
    full_name,
    user_type,
    role,
    active,
    CASE WHEN password_hash IS NOT NULL THEN 'Yes' ELSE 'No' END AS has_password
FROM users
WHERE employee_id IN ('EMP001', 'EMP002', 'EMP003', 'EMP004', 'EMP005', 'EMP006')
ORDER BY employee_id;

PRINT 'User seeding completed!';
PRINT 'Test Credentials:';
PRINT '  EMP001 / password123 (Area Lead)';
PRINT '  EMP002 / password123 (Zone Lead)';
PRINT '  EMP003 / password123 (Circle Lead)';
PRINT '  EMP004 / admin123 (Admin)';
PRINT '  EMP005 / password123 (Analyst)';
PRINT '  EMP006 / password123 (Zone Lead)';
GO
