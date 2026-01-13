-- ============================================
-- Ops Hub - Simple User Seeding SQL Script
-- ============================================
-- IMPORTANT: Replace the password_hash values with actual BCrypt hashes
-- Get hashes from: http://localhost:8080/api/auth/generate-hash?password=admin123
-- Or use: https://bcrypt-generator.com/

USE ops_hub;
GO

-- First, get the area_id (assuming at least one area exists)
-- If no areas exist, run the full seed_users.sql script first

DECLARE @AreaId BIGINT;

-- Try to get Behrampur area, or use the first available area
SELECT TOP 1 @AreaId = id FROM areas WHERE code = 'BHR' OR code IS NOT NULL ORDER BY id;

-- If no areas exist, create minimal hierarchy
IF @AreaId IS NULL
BEGIN
    -- Create cluster
    IF NOT EXISTS (SELECT 1 FROM clusters WHERE code = 'BUP')
        INSERT INTO clusters (code, name, description, active, created_at, updated_at)
        VALUES ('BUP', 'Bihar UP', 'Cluster: Bihar UP', 1, GETDATE(), GETDATE());
    
    DECLARE @ClusterId BIGINT = (SELECT id FROM clusters WHERE code = 'BUP');
    
    -- Create circle
    IF NOT EXISTS (SELECT 1 FROM circles WHERE code = 'UP')
        INSERT INTO circles (code, name, description, cluster_id, active, created_at, updated_at)
        VALUES ('UP', 'Uttar Pradesh', 'Circle: Uttar Pradesh', @ClusterId, 1, GETDATE(), GETDATE());
    
    DECLARE @CircleId BIGINT = (SELECT id FROM circles WHERE code = 'UP');
    
    -- Create zone
    IF NOT EXISTS (SELECT 1 FROM zones WHERE code = 'GZB')
        INSERT INTO zones (code, name, description, circle_id, active, created_at, updated_at)
        VALUES ('GZB', 'Ghaziabad', 'Zone: Ghaziabad', @CircleId, 1, GETDATE(), GETDATE());
    
    DECLARE @ZoneId BIGINT = (SELECT id FROM zones WHERE code = 'GZB');
    
    -- Create area
    IF NOT EXISTS (SELECT 1 FROM areas WHERE code = 'BHR')
        INSERT INTO areas (code, name, description, zone_id, active, created_at, updated_at)
        VALUES ('BHR', 'Behrampur', 'Area: Behrampur', @ZoneId, 1, GETDATE(), GETDATE());
    
    SET @AreaId = (SELECT id FROM areas WHERE code = 'BHR');
END

-- Get Meerut area or use same area
DECLARE @MeerutAreaId BIGINT = (SELECT id FROM areas WHERE code = 'MRT');
IF @MeerutAreaId IS NULL SET @MeerutAreaId = @AreaId;

-- ============================================
-- INSERT USERS
-- ============================================
-- REPLACE THE PASSWORD_HASH VALUES BELOW WITH ACTUAL BCRYPT HASHES
-- Use: GET http://localhost:8080/api/auth/generate-hash?password=admin123
-- Or visit: https://bcrypt-generator.com/

-- EMP004 - Admin (password: admin123)
-- REPLACE THIS HASH: $2a$10$REPLACE_WITH_ACTUAL_HASH_FROM_GENERATOR
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP004')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP004',
        'admin',
        '$2a$10$REPLACE_WITH_ACTUAL_HASH_FROM_GENERATOR', -- REPLACE: Get hash from /api/auth/generate-hash?password=admin123
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
    PRINT 'User EMP004 (Admin) created';
END
ELSE
BEGIN
    PRINT 'User EMP004 already exists';
END

-- EMP001 - Area Lead (password: password123)
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP001')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP001',
        'shivam',
        '$2a$10$REPLACE_WITH_ACTUAL_HASH_FROM_GENERATOR', -- REPLACE: Get hash from /api/auth/generate-hash?password=password123
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
    PRINT 'User EMP001 (Area Lead) created';
END

-- EMP002 - Zone Lead (password: password123)
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP002')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP002',
        'rahul',
        '$2a$10$REPLACE_WITH_ACTUAL_HASH_FROM_GENERATOR', -- REPLACE: Get hash from /api/auth/generate-hash?password=password123
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
    PRINT 'User EMP002 (Zone Lead) created';
END

-- EMP003 - Circle Lead (password: password123)
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP003')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP003',
        'priya',
        '$2a$10$REPLACE_WITH_ACTUAL_HASH_FROM_GENERATOR', -- REPLACE: Get hash from /api/auth/generate-hash?password=password123
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
    PRINT 'User EMP003 (Circle Lead) created';
END

-- EMP005 - Analyst (password: password123)
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP005')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP005',
        'analyst1',
        '$2a$10$REPLACE_WITH_ACTUAL_HASH_FROM_GENERATOR', -- REPLACE: Get hash from /api/auth/generate-hash?password=password123
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
    PRINT 'User EMP005 (Analyst) created';
END

-- EMP006 - Zone Lead (password: password123)
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP006')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP006',
        'manager1',
        '$2a$10$REPLACE_WITH_ACTUAL_HASH_FROM_GENERATOR', -- REPLACE: Get hash from /api/auth/generate-hash?password=password123
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
    PRINT 'User EMP006 (Zone Lead) created';
END

-- Verify
SELECT employee_id, username, full_name, user_type, role, active 
FROM users 
WHERE employee_id IN ('EMP001', 'EMP002', 'EMP003', 'EMP004', 'EMP005', 'EMP006')
ORDER BY employee_id;

PRINT '========================================';
PRINT 'User seeding completed!';
PRINT 'IMPORTANT: Replace password_hash values with actual BCrypt hashes';
PRINT 'Get hashes from: http://localhost:8080/api/auth/generate-hash?password=YOUR_PASSWORD';
PRINT '========================================';
GO
