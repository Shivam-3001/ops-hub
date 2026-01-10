# Ops Hub API

Enterprise-grade operations dashboard backend API built with Spring Boot.

## Tech Stack

- **Spring Boot 3.5.9**
- **Java 17**
- **MS SQL Server** (Database)
- **Spring Data JPA**
- **Spring Security** (OAuth2 ready)
- **Lombok**

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- MS SQL Server 2019 or higher
- MS SQL Server JDBC Driver (included via Maven)

## Database Setup

1. **Install MS SQL Server** (if not already installed)
   - Download from: https://www.microsoft.com/en-us/sql-server/sql-server-downloads
   - Or use SQL Server Docker container:
     ```bash
     docker run -e "ACCEPT_EULA=Y" -e "SA_PASSWORD=YourStrong@Passw0rd" -p 1433:1433 --name sqlserver -d mcr.microsoft.com/mssql/server:2022-latest
     ```

2. **Create Database**
   ```sql
   CREATE DATABASE ops_hub;
   GO
   ```

3. **Configure Connection** (in `application.yaml` or environment variables)
   ```yaml
   spring:
     datasource:
       url: jdbc:sqlserver://localhost:1433;databaseName=ops_hub;encrypt=true;trustServerCertificate=true
       username: sa
       password: YourStrong@Passw0rd
   ```

   Or use environment variables:
   ```bash
   export DB_USERNAME=sa
   export DB_PASSWORD=YourStrong@Passw0rd
   ```

## Configuration

### Database Connection

Edit `src/main/resources/application.yaml` or set environment variables:

- `DB_USERNAME`: Database username (default: `sa`)
- `DB_PASSWORD`: Database password (default: `YourStrong@Passw0rd`)

### Application Properties

- Server port: `8080`
- Context path: `/api`
- Actuator endpoints: `/actuator/health`, `/actuator/info`, `/actuator/metrics`

### JPA/Hibernate

- `ddl-auto: update` - Automatically creates/updates database schema (development)
- For production, change to `validate` and use migrations (Flyway/Liquibase)

## Running the Application

### Using Maven

```bash
cd ops-hub-api
mvn clean install
mvn spring-boot:run
```

### Using IDE

Run the `OpsHubApiApplication.java` class directly from your IDE.

## Testing

### Health Check

```bash
curl http://localhost:8080/api/actuator/health
```

### Dashboard Endpoint

```bash
curl http://localhost:8080/api/dashboard
```

Expected response:
```json
{
  "message": "Welcome to Ops Hub Dashboard!",
  "status": "operational",
  "database": "connected",
  "userCount": 0
}
```

## Project Structure

```
ops-hub-api/
├── src/main/java/com/company/ops_hub_api/
│   ├── config/          # Configuration classes
│   ├── controller/      # REST controllers
│   ├── domain/          # JPA entities
│   ├── dto/             # Data Transfer Objects
│   ├── exception/       # Exception handlers
│   ├── repository/      # JPA repositories
│   ├── security/        # Security configurations
│   ├── service/         # Business logic
│   └── util/            # Utility classes
└── src/main/resources/
    └── application.yaml # Application configuration
```

## Current Features

- ✅ MS SQL Server integration
- ✅ JPA/Hibernate configuration
- ✅ Basic security setup (permissive for development)
- ✅ Health check endpoint
- ✅ Dashboard API endpoint
- ✅ User entity with role-based structure

## Next Steps

- [ ] OAuth2 authentication implementation
- [ ] Two-Factor Authentication (2FA)
- [ ] Role-based access control (RBAC)
- [ ] User management APIs
- [ ] Reporting and analytics endpoints
- [ ] AI assistant integration

## Troubleshooting

### Database Connection Issues

1. Verify SQL Server is running:
   ```bash
   # Windows
   Get-Service MSSQLSERVER
   
   # Linux/Mac (Docker)
   docker ps | grep sqlserver
   ```

2. Test connection with SQL Server Management Studio or sqlcmd

3. Check firewall settings (default port: 1433)

4. Verify credentials in `application.yaml`

### Port Already in Use

Change the port in `application.yaml`:
```yaml
server:
  port: 8081
```

### Build Errors

Ensure Java 17 is installed:
```bash
java -version
```

Clean and rebuild:
```bash
mvn clean install
```
