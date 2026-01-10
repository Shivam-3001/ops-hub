# Database Setup Instructions

## Issue: Database Connection Fails

The application is currently connecting to the `master` database temporarily. You need to:

1. **Create the `ops_hub` database**
2. **Verify SQL Server credentials**
3. **Update the connection string**

## Steps to Fix

### Option 1: Using SQL Server Management Studio (SSMS)

1. Open SQL Server Management Studio
2. Connect to: `localhost\SQLSERVER1`
   - Authentication: SQL Server Authentication
   - Login: `sa`
   - Password: `avastino` (or your actual password)
3. Execute this SQL script:
   ```sql
   CREATE DATABASE ops_hub;
   GO
   
   -- Verify the database was created
   SELECT name FROM sys.databases WHERE name = 'ops_hub';
   GO
   ```

### Option 2: Using sqlcmd (Command Line)

```powershell
sqlcmd -S localhost\SQLSERVER1 -U sa -P avastino -Q "CREATE DATABASE ops_hub;"
```

### Option 3: Using PowerShell

```powershell
Invoke-Sqlcmd -ServerInstance "localhost\SQLSERVER1" -Username "sa" -Password "avastino" -Query "CREATE DATABASE ops_hub;"
```

## After Creating the Database

1. Update `application.yaml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:sqlserver://localhost;instanceName=SQLSERVER1;databaseName=ops_hub;encrypt=true;trustServerCertificate=true
   
   jpa:
     hibernate:
       ddl-auto: update  # Change from 'none' to 'update'
   ```

2. Restart the application

## Troubleshooting Authentication Issues

If you're getting "Login failed for user 'sa'", try:

### 1. Enable SQL Server Authentication (if using Windows Auth)

```sql
-- Connect using Windows Authentication first, then run:
ALTER LOGIN sa ENABLE;
GO
ALTER LOGIN sa WITH PASSWORD = 'avastino';
GO
```

### 2. Verify SQL Server Authentication Mode

1. Open SSMS → Right-click server → Properties
2. Go to "Security" tab
3. Ensure "SQL Server and Windows Authentication mode" is selected
4. Restart SQL Server service if changed

### 3. Check if 'sa' account is enabled

```sql
SELECT name, is_disabled FROM sys.sql_logins WHERE name = 'sa';
-- If is_disabled = 1, enable it:
ALTER LOGIN sa ENABLE;
GO
```

## Verify Connection

Test the connection manually:

```powershell
sqlcmd -S localhost\SQLSERVER1 -U sa -P avastino -d ops_hub -Q "SELECT @@VERSION;"
```

If this works, the application should connect successfully.

## Alternative: Use Windows Authentication

If SQL Server Authentication is problematic, you can use Windows Authentication:

1. Update `application.yaml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:sqlserver://localhost;instanceName=SQLSERVER1;databaseName=ops_hub;encrypt=true;trustServerCertificate=true;integratedSecurity=true
       # Remove username and password for Windows Auth
   ```

2. Ensure the SQL Server JDBC driver supports integrated security (may need additional setup)
