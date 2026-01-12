# Quick Start Guide - Testing Login and Application

## Test Credentials

### 🔑 Admin User (Full Access)
- **Employee ID**: `EMP004`
- **Password**: `admin123`
- **Role**: ADMIN
- **Has All Permissions**

### 👔 Lead Users (Manager Access)
- **Employee ID**: `EMP001` | **Password**: `password123` | **Type**: AREA_LEAD
- **Employee ID**: `EMP002` | **Password**: `password123` | **Type**: ZONE_LEAD
- **Employee ID**: `EMP003` | **Password**: `password123` | **Type**: CIRCLE_LEAD
- **Employee ID**: `EMP006` | **Password**: `password123` | **Type**: ZONE_LEAD

### 👤 Agent User (Basic Access)
- **Employee ID**: `EMP005`
- **Password**: `password123`
- **Role**: ANALYST
- **Has Basic Permissions**

## Login Flow

### Step 1: Start the Application
```powershell
cd ops-hub-api
.\mvnw.cmd spring-boot:run
```

Wait for:
- `Started OpsHubApiApplication`
- `Data seeding completed successfully!`
- `RBAC data seeding complete.`

### Step 2: Test Login

**Using PowerShell:**
```powershell
$loginBody = @{
    employeeId = "EMP004"
    password = "admin123"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
    -Method POST `
    -ContentType "application/json" `
    -Body $loginBody

Write-Host "Token: $($loginResponse.token)"
Write-Host "User: $($loginResponse.fullName)"
Write-Host "Role: $($loginResponse.role)"
```

**Using cURL:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"employeeId\":\"EMP004\",\"password\":\"admin123\"}"
```

### Step 3: Use the Token

Save the token from login response, then use it in subsequent requests:

```powershell
$token = "YOUR_TOKEN_HERE"

$headers = @{
    Authorization = "Bearer $token"
}

# Get my permissions
$permissions = Invoke-RestMethod -Uri "http://localhost:8080/api/permissions/me" `
    -Method GET `
    -Headers $headers

$permissions | ConvertTo-Json -Depth 5
```

## Expected Login Response

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

## Testing Endpoints

### 1. Health Check
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/actuator/health"
```

### 2. Login
```powershell
$loginBody = @{
    employeeId = "EMP004"
    password = "admin123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
    -Method POST `
    -ContentType "application/json" `
    -Body $loginBody

$token = $response.token
```

### 3. Get My Permissions
```powershell
$headers = @{ Authorization = "Bearer $token" }
Invoke-RestMethod -Uri "http://localhost:8080/api/permissions/me" `
    -Method GET `
    -Headers $headers
```

### 4. Get Dashboard (if implemented)
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/dashboard" `
    -Method GET `
    -Headers $headers
```

### 5. Get All Customers (requires VIEW_CUSTOMERS)
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/customers" `
    -Method GET `
    -Headers $headers
```

## Common Test Scenarios

### Test 1: Admin Login
- Employee ID: `EMP004`
- Password: `admin123`
- Should get all permissions

### Test 2: Lead Login
- Employee ID: `EMP001`
- Password: `password123`
- Should get most permissions (excludes MANAGE_USERS)

### Test 3: Agent Login
- Employee ID: `EMP005`
- Password: `password123`
- Should get basic permissions only

### Test 4: Invalid Credentials
- Employee ID: `EMP999`
- Password: `wrongpassword`
- Should return 401/400 error

## Troubleshooting

### Application won't start
1. Check if port 8080 is available
2. Check database connection in `application.yaml`
3. Ensure MS SQL Server is running
4. Check database `ops_hub` exists

### Login fails
1. Check if data seeding completed (check logs)
2. Verify Employee ID format (e.g., EMP004, not admin)
3. Check password is correct
4. Verify user is active in database

### Permission errors
1. Check if RBAC data seeding completed
2. Verify user has assigned role in `user_roles` table
3. Check role has permissions in `role_permissions` table
