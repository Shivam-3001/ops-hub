-- ============================================
-- Ops Hub - Direct User INSERT Statements
-- ============================================
-- Run this script in your MS SQL Server database
-- 
-- STEP 1: First, get BCrypt hashes by calling:
--   http://localhost:8080/api/auth/generate-hash?password=Jan@2026
--   http://localhost:8080/api/auth/generate-hash?password=Ops@2026
-- 
-- STEP 2: Replace the hash placeholders below with the actual hashes
-- STEP 3: Run this script

USE ops_hub;
GO

-- Get or create area_id (required for users)
DECLARE @AreaId BIGINT;
DECLARE @SecondaryAreaId BIGINT;

-- Password hashes (replace placeholders with actual BCrypt hashes)
DECLARE @HashAdmin NVARCHAR(255) = '$2a$12$M9uX4SmUbliutLybP.GzHuSy0ubeuCsCnxiVQEiUruQ85vsrAkoc6';
DECLARE @HashUsers NVARCHAR(255) = '$2a$12$Ld8ooygBdMYwgrXH9iaNRe7d9jSGUSOi807snO9Kav2BUY1CysUcq';

-- Try to find existing areas (prefer Sector 63 and Indirapuram)
SELECT @AreaId = id FROM areas WHERE code = 'SECTOR_63';
SELECT @SecondaryAreaId = id FROM areas WHERE code = 'INDIRAPURAM';

-- If no areas exist, create minimal hierarchy aligned to final mapping
IF @AreaId IS NULL
BEGIN
    -- Cluster
    IF NOT EXISTS (SELECT 1 FROM clusters WHERE code = 'NORTH')
        INSERT INTO clusters (code, name, description, active, created_at, updated_at)
        VALUES ('NORTH', 'North', 'Cluster', 1, GETDATE(), GETDATE());
    
    DECLARE @ClusterId BIGINT = (SELECT id FROM clusters WHERE code = 'NORTH');
    
    -- Circle
    IF NOT EXISTS (SELECT 1 FROM circles WHERE code = 'UTTAR_PRADESH')
        INSERT INTO circles (code, name, description, cluster_id, active, created_at, updated_at)
        VALUES ('UTTAR_PRADESH', 'Uttar Pradesh', 'Circle', @ClusterId, 1, GETDATE(), GETDATE());
    
    DECLARE @CircleId BIGINT = (SELECT id FROM circles WHERE code = 'UTTAR_PRADESH');
    
    -- Zone
    IF NOT EXISTS (SELECT 1 FROM zones WHERE code = 'NOIDA')
        INSERT INTO zones (code, name, description, circle_id, active, created_at, updated_at)
        VALUES ('NOIDA', 'Noida', 'Zone', @CircleId, 1, GETDATE(), GETDATE());
    
    DECLARE @ZoneId BIGINT = (SELECT id FROM zones WHERE code = 'NOIDA');
    
    -- Area (Sector 63)
    IF NOT EXISTS (SELECT 1 FROM areas WHERE code = 'SECTOR_63')
        INSERT INTO areas (code, name, description, zone_id, active, created_at, updated_at)
        VALUES ('SECTOR_63', 'Sector 63', 'Area', @ZoneId, 1, GETDATE(), GETDATE());
    
    -- Area (Indirapuram)
    IF NOT EXISTS (SELECT 1 FROM areas WHERE code = 'INDIRAPURAM')
        INSERT INTO areas (code, name, description, zone_id, active, created_at, updated_at)
        VALUES ('INDIRAPURAM', 'Indirapuram', 'Area', @ZoneId, 1, GETDATE(), GETDATE());
END

SET @AreaId = (SELECT id FROM areas WHERE code = 'SECTOR_63');
SET @SecondaryAreaId = (SELECT id FROM areas WHERE code = 'INDIRAPURAM');
IF @SecondaryAreaId IS NULL SET @SecondaryAreaId = @AreaId;

-- ============================================
-- INSERT USERS
-- ============================================
-- IMPORTANT: Get BCrypt hashes first from:
-- GET http://localhost:8080/api/auth/generate-hash?password=Jan@2026
-- GET http://localhost:8080/api/auth/generate-hash?password=Ops@2026
--
-- Then replace HASH_PLACEHOLDER_ADMIN and HASH_PLACEHOLDER_USERS below

-- EMP004 - Admin (username: shivam, password: Jan@2026)
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP004')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP004',
        'shivam',
        @HashAdmin,
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

-- EMP001 - Area Head (password: Ops@2026)
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP001')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP001',
        'shivam-area',
        @HashUsers,
        'shivam@example.com',
        'Shivam Kumar',
        '9876543210',
        'AREA_HEAD',
        'AREA_HEAD',
        @AreaId,
        1,
        0,
        GETDATE(),
        GETDATE()
    );
END

-- EMP002 - Zone Head (password: Ops@2026)
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP002')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP002',
        'rahul',
        @HashUsers,
        'rahul@example.com',
        'Rahul Sharma',
        '9876543211',
        'ZONE_HEAD',
        'ZONE_HEAD',
        @AreaId,
        1,
        0,
        GETDATE(),
        GETDATE()
    );
END

-- EMP003 - Circle Head (password: Ops@2026)
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP003')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP003',
        'priya',
        @HashUsers,
        'priya@example.com',
        'Priya Singh',
        '9876543212',
        'CIRCLE_HEAD',
        'CIRCLE_HEAD',
        @AreaId,
        1,
        0,
        GETDATE(),
        GETDATE()
    );
END

-- EMP005 - Agent (password: Ops@2026)
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP005')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP005',
        'agent1',
        @HashUsers,
        'agent1@example.com',
        'Agent One',
        '9876543214',
        'AGENT',
        'AGENT',
        @SecondaryAreaId,
        1,
        0,
        GETDATE(),
        GETDATE()
    );
END

-- EMP006 - Store Head (password: Ops@2026)
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP006')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP006',
        'store1',
        @HashUsers,
        'store1@example.com',
        'Store Head One',
        '9876543215',
        'STORE_HEAD',
        'STORE_HEAD',
        @SecondaryAreaId,
        1,
        0,
        GETDATE(),
        GETDATE()
    );
END

-- EMP007 - Cluster Head (password: Ops@2026)
IF NOT EXISTS (SELECT 1 FROM users WHERE employee_id = 'EMP007')
BEGIN
    INSERT INTO users (employee_id, username, password_hash, email, full_name, phone, user_type, role, area_id, active, two_factor_enabled, created_at, updated_at)
    VALUES (
        'EMP007',
        'cluster1',
        @HashUsers,
        'cluster1@example.com',
        'Cluster Head One',
        '9876543216',
        'CLUSTER_HEAD',
        'CLUSTER_HEAD',
        @AreaId,
        1,
        0,
        GETDATE(),
        GETDATE()
    );
END

-- Assign user_roles based on roles table (required for permissions)
DECLARE @AdminRoleId BIGINT = (SELECT id FROM roles WHERE code = 'ADMIN');
DECLARE @ClusterRoleId BIGINT = (SELECT id FROM roles WHERE code = 'CLUSTER_HEAD');
DECLARE @CircleRoleId BIGINT = (SELECT id FROM roles WHERE code = 'CIRCLE_HEAD');
DECLARE @ZoneRoleId BIGINT = (SELECT id FROM roles WHERE code = 'ZONE_HEAD');
DECLARE @AreaRoleId BIGINT = (SELECT id FROM roles WHERE code = 'AREA_HEAD');
DECLARE @StoreRoleId BIGINT = (SELECT id FROM roles WHERE code = 'STORE_HEAD');
DECLARE @AgentRoleId BIGINT = (SELECT id FROM roles WHERE code = 'AGENT');

IF NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = (SELECT id FROM users WHERE employee_id = 'EMP004') AND role_id = @AdminRoleId)
    INSERT INTO user_roles (user_id, role_id, created_at) VALUES ((SELECT id FROM users WHERE employee_id = 'EMP004'), @AdminRoleId, GETDATE());
IF NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = (SELECT id FROM users WHERE employee_id = 'EMP007') AND role_id = @ClusterRoleId)
    INSERT INTO user_roles (user_id, role_id, created_at) VALUES ((SELECT id FROM users WHERE employee_id = 'EMP007'), @ClusterRoleId, GETDATE());
IF NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = (SELECT id FROM users WHERE employee_id = 'EMP003') AND role_id = @CircleRoleId)
    INSERT INTO user_roles (user_id, role_id, created_at) VALUES ((SELECT id FROM users WHERE employee_id = 'EMP003'), @CircleRoleId, GETDATE());
IF NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = (SELECT id FROM users WHERE employee_id = 'EMP002') AND role_id = @ZoneRoleId)
    INSERT INTO user_roles (user_id, role_id, created_at) VALUES ((SELECT id FROM users WHERE employee_id = 'EMP002'), @ZoneRoleId, GETDATE());
IF NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = (SELECT id FROM users WHERE employee_id = 'EMP001') AND role_id = @AreaRoleId)
    INSERT INTO user_roles (user_id, role_id, created_at) VALUES ((SELECT id FROM users WHERE employee_id = 'EMP001'), @AreaRoleId, GETDATE());
IF NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = (SELECT id FROM users WHERE employee_id = 'EMP006') AND role_id = @StoreRoleId)
    INSERT INTO user_roles (user_id, role_id, created_at) VALUES ((SELECT id FROM users WHERE employee_id = 'EMP006'), @StoreRoleId, GETDATE());
IF NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = (SELECT id FROM users WHERE employee_id = 'EMP005') AND role_id = @AgentRoleId)
    INSERT INTO user_roles (user_id, role_id, created_at) VALUES ((SELECT id FROM users WHERE employee_id = 'EMP005'), @AgentRoleId, GETDATE());

-- Verify users were created
SELECT employee_id, username, full_name, user_type, role, active 
FROM users 
WHERE employee_id IN ('EMP001', 'EMP002', 'EMP003', 'EMP004', 'EMP005', 'EMP006', 'EMP007')
ORDER BY employee_id;

PRINT 'Users inserted! Remember to replace HASH_PLACEHOLDER values with actual BCrypt hashes.';
GO
