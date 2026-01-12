# Test Credentials and Login Flow

## Test Credentials

The following test users are created automatically when the application starts (if database is empty):

### Admin User
- **Employee ID**: `EMP004`
- **Password**: `admin123`
- **Role**: ADMIN
- **User Type**: ADMIN
- **Permissions**: All permissions (VIEW_CUSTOMERS, ASSIGN_CUSTOMERS, COLLECT_PAYMENT, APPROVE_PROFILE, VIEW_REPORTS, EXPORT_REPORTS, USE_AI_AGENT, etc.)

### Lead Users (MANAGER role)
- **Employee ID**: `EMP001`
- **Password**: `password123`
- **Role**: MANAGER
- **User Type**: AREA_LEAD
- **Permissions**: Most permissions (excludes MANAGE_USERS, MANAGE_SETTINGS)

- **Employee ID**: `EMP002`
- **Password**: `password123`
- **Role**: MANAGER
- **User Type**: ZONE_LEAD
- **Permissions**: Most permissions (excludes MANAGE_USERS, MANAGE_SETTINGS)

- **Employee ID**: `EMP003`
- **Password**: `password123`
- **Role**: MANAGER
- **User Type**: CIRCLE_LEAD
- **Permissions**: Most permissions (excludes MANAGE_USERS, MANAGE_SETTINGS)

- **Employee ID**: `EMP006`
- **Password**: `password123`
- **Role**: MANAGER
- **User Type**: ZONE_LEAD
- **Permissions**: Most permissions (excludes MANAGE_USERS, MANAGE_SETTINGS)

### Agent User
- **Employee ID**: `EMP005`
- **Password**: `password123`
- **Role**: ANALYST
- **User Type**: ANALYST
- **Permissions**: Basic operational permissions (VIEW_CUSTOMERS, COLLECT_PAYMENT, VIEW_REPORTS, USE_AI_AGENT, VIEW_VISITS, CREATE_VISITS, VIEW_PAYMENTS)

## Login Flow

### 1. Login Endpoint
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

Request Body:
{
  "employeeId": "EMP004",
  "password": "admin123"
}
```

### 2. Successful Response
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
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

### 3. Using the Token
Include the token in subsequent requests:
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

## Quick Test Commands

### Using cURL

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"employeeId\":\"EMP004\",\"password\":\"admin123\"}"
```

**Get My Permissions (after login):**
```bash
curl -X GET http://localhost:8080/api/permissions/me \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**Get Dashboard:**
```bash
curl -X GET http://localhost:8080/api/dashboard \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### Using PowerShell (Windows)

**Login:**
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
Write-Host "Token: $token"
```

**Get My Permissions:**
```powershell
$headers = @{
    Authorization = "Bearer $token"
}

$permissions = Invoke-RestMethod -Uri "http://localhost:8080/api/permissions/me" `
    -Method GET `
    -Headers $headers

$permissions | ConvertTo-Json
```

## Testing Different User Roles

### Test Admin User (All Permissions)
```json
{
  "employeeId": "EMP004",
  "password": "admin123"
}
```

### Test Lead User (Most Permissions)
```json
{
  "employeeId": "EMP001",
  "password": "password123"
}
```

### Test Agent User (Basic Permissions)
```json
{
  "employeeId": "EMP005",
  "password": "password123"
}
```

## Expected Permissions by Role

### ADMIN (EMP004)
- All 15 permissions including:
  - VIEW_CUSTOMERS, ASSIGN_CUSTOMERS, MANAGE_CUSTOMERS
  - COLLECT_PAYMENT, VIEW_PAYMENTS
  - VIEW_REPORTS, EXPORT_REPORTS
  - APPROVE_PROFILE
  - USE_AI_AGENT
  - MANAGE_USERS, MANAGE_SETTINGS
  - VIEW_PERMISSIONS, VIEW_ROLES

### LEAD (EMP001, EMP002, EMP003, EMP006)
- 13 permissions (excludes MANAGE_USERS, MANAGE_SETTINGS)
- Includes: VIEW_CUSTOMERS, ASSIGN_CUSTOMERS, COLLECT_PAYMENT, APPROVE_PROFILE, etc.

### AGENT (EMP005)
- 7 basic permissions:
  - VIEW_CUSTOMERS
  - COLLECT_PAYMENT
  - VIEW_REPORTS
  - USE_AI_AGENT
  - VIEW_VISITS
  - CREATE_VISITS
  - VIEW_PAYMENTS

## Testing Workflow

1. **Start the Application**
   ```bash
   cd ops-hub-api
   .\mvnw.cmd spring-boot:run
   ```

2. **Wait for Application to Start**
   - Look for: "Started OpsHubApiApplication"
   - Check logs for: "Data seeding completed successfully!"

3. **Test Login**
   - Use any of the test credentials above
   - Save the JWT token from response

4. **Test Permissions**
   - Call `/api/permissions/me` with the token
   - Verify permissions match the user's role

5. **Test Protected Endpoints**
   - Try accessing endpoints that require specific permissions
   - Verify access is granted/denied correctly

## Common Issues

### Issue: "Invalid employee ID or password"
- **Solution**: Make sure you're using the correct Employee ID (e.g., EMP004, not admin)
- Check that data seeding completed successfully

### Issue: "User account is inactive"
- **Solution**: All test users are created as active, but check database if issue persists

### Issue: "Insufficient permissions"
- **Solution**: Use a user with the required permission (e.g., ADMIN for all permissions)

### Issue: Database connection error
- **Solution**: Ensure MS SQL Server is running and database `ops_hub` exists
- Check connection string in `application.yaml`
