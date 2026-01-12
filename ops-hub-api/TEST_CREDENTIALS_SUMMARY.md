# Test Credentials Summary

## 🔐 Quick Reference - Test Credentials

| Employee ID | Password | Role | User Type | Key Permissions |
|------------|----------|------|-----------|----------------|
| **EMP004** | `admin123` | ADMIN | ADMIN | ✅ **ALL PERMISSIONS** |
| **EMP001** | `password123` | MANAGER | AREA_LEAD | Most permissions (excludes MANAGE_USERS) |
| **EMP002** | `password123` | MANAGER | ZONE_LEAD | Most permissions (excludes MANAGE_USERS) |
| **EMP003** | `password123` | MANAGER | CIRCLE_LEAD | Most permissions (excludes MANAGE_USERS) |
| **EMP005** | `password123` | ANALYST | ANALYST | Basic permissions only |
| **EMP006** | `password123` | MANAGER | ZONE_LEAD | Most permissions (excludes MANAGE_USERS) |

## 📋 Login Request Format

**Endpoint:** `POST http://localhost:8080/api/auth/login`

**Request Body:**
```json
{
  "employeeId": "EMP004",
  "password": "admin123"
}
```

**Note:** Login uses `employeeId` (not username or email)

## ✅ Expected Login Response

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJlbXBsb3llZUlkIjoiRU1QMDA0IiwidXNlclR5cGUiOiJBRE1JTiIsInVzZXJJZCI6NCwic3ViIjoiYWRtaW4iLCJpYXQiOjE3MDQ5ODI5MzUsImV4cCI6MTcwNTA2OTMzNX0...",
  "employeeId": "EMP004",
  "username": "admin",
  "fullName": "Admin User",
  "userType": "ADMIN",
  "role": "ADMIN",
  "areaName": "Behrampur",
  "zoneName": "Ghaziabad",
  "circleName": "Uttar Pradesh",
  "clusterName": "Bihar UP"
}
```

## 🔄 Complete Login Flow

### Step 1: Start Application
```powershell
cd ops-hub-api
.\mvnw.cmd spring-boot:run
```

**Wait for these log messages:**
- ✅ `Started OpsHubApiApplication`
- ✅ `Data seeding completed successfully!`
- ✅ `RBAC data seeding complete.`

### Step 2: Login
```powershell
$body = @{
    employeeId = "EMP004"
    password = "admin123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
    -Method POST `
    -ContentType "application/json" `
    -Body $body

$token = $response.token
```

### Step 3: Use Token in Requests
```powershell
$headers = @{
    Authorization = "Bearer $token"
}

# Example: Get my permissions
Invoke-RestMethod -Uri "http://localhost:8080/api/permissions/me" `
    -Method GET `
    -Headers $headers
```

## 🧪 Test Scenarios

### Scenario 1: Admin Login (Full Access)
```json
{
  "employeeId": "EMP004",
  "password": "admin123"
}
```
**Expected:** Token with all permissions

### Scenario 2: Lead Login (Manager Access)
```json
{
  "employeeId": "EMP001",
  "password": "password123"
}
```
**Expected:** Token with most permissions (can approve profiles, assign customers, etc.)

### Scenario 3: Agent Login (Basic Access)
```json
{
  "employeeId": "EMP005",
  "password": "password123"
}
```
**Expected:** Token with basic permissions (can view customers, create visits, collect payments)

### Scenario 4: Invalid Credentials
```json
{
  "employeeId": "EMP999",
  "password": "wrongpassword"
}
```
**Expected:** 401/400 error: "Invalid employee ID or password"

## 🔍 Verification Steps

### 1. Check Application Health
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/actuator/health"
```

### 2. Test Login
```powershell
$loginBody = @{ employeeId = "EMP004"; password = "admin123" } | ConvertTo-Json
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method POST -ContentType "application/json" -Body $loginBody
$response | ConvertTo-Json
```

### 3. Verify Permissions
```powershell
$token = $response.token
$headers = @{ Authorization = "Bearer $token" }
$permissions = Invoke-RestMethod -Uri "http://localhost:8080/api/permissions/me" -Method GET -Headers $headers
$permissions | ConvertTo-Json -Depth 5
```

**Expected for ADMIN (EMP004):**
- Should have 15 permissions including:
  - VIEW_CUSTOMERS, ASSIGN_CUSTOMERS, MANAGE_CUSTOMERS
  - COLLECT_PAYMENT, VIEW_PAYMENTS
  - APPROVE_PROFILE
  - VIEW_REPORTS, EXPORT_REPORTS
  - USE_AI_AGENT
  - MANAGE_USERS, MANAGE_SETTINGS
  - VIEW_PERMISSIONS, VIEW_ROLES

## 🚨 Troubleshooting

### Issue: Application won't start
**Check:**
1. Port 8080 is not in use: `netstat -ano | findstr :8080`
2. Database is running and accessible
3. Database `ops_hub` exists
4. Check `application.yaml` for correct database credentials

### Issue: Login returns "Invalid employee ID or password"
**Check:**
1. Using correct Employee ID format (EMP004, not admin)
2. Data seeding completed (check application logs)
3. User exists in database: `SELECT * FROM users WHERE employee_id = 'EMP004'`
4. User is active: `SELECT active FROM users WHERE employee_id = 'EMP004'`

### Issue: "Insufficient permissions" error
**Check:**
1. RBAC data seeding completed
2. User has role assigned: `SELECT * FROM user_roles WHERE user_id = (SELECT id FROM users WHERE employee_id = 'EMP004')`
3. Role has permissions: `SELECT * FROM role_permissions WHERE role_id = (SELECT id FROM roles WHERE code = 'ADMIN')`

## 📝 Notes

- **Login uses `employeeId`**, not username or email
- **JWT token expires in 24 hours** (configurable in `application.yaml`)
- **Token must be included** in `Authorization: Bearer <token>` header
- **All test users are created automatically** on first application start
- **Passwords are BCrypt hashed** in database
- **Sensitive data (email, phone) is encrypted** in database
