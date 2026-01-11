# Implementation Summary: User Hierarchy & Authentication System

## Overview

Implemented a complete hierarchical user management system with JWT-based authentication, BCrypt password hashing, and application-level encryption support.

## Database Structure

### Hierarchy (Bottom to Top)
```
User → Area → Zone → Circle → Cluster
```

Example: **shivam** (User) → **behrampur** (Area) → **ghaziabad** (Zone) → **up** (Circle) → **bup** (Cluster)

## Entities Created

### 1. Cluster
- `id` (Long, Auto-generated)
- `code` (String, Unique) - e.g., "BUP"
- `name` (String) - e.g., "Bihar UP"
- `description` (String, Optional)
- `active` (Boolean)
- `circles` (OneToMany relationship)
- `createdAt`, `updatedAt` (Audit fields)

### 2. Circle
- `id` (Long, Auto-generated)
- `code` (String, Unique) - e.g., "UP"
- `name` (String) - e.g., "Uttar Pradesh"
- `description` (String, Optional)
- `cluster` (ManyToOne → Cluster)
- `manager` (ManyToOne → User) - Circle Lead/Manager
- `active` (Boolean)
- `zones` (OneToMany relationship)
- `createdAt`, `updatedAt` (Audit fields)

### 3. Zone
- `id` (Long, Auto-generated)
- `code` (String, Unique) - e.g., "GZB"
- `name` (String) - e.g., "Ghaziabad"
- `description` (String, Optional)
- `circle` (ManyToOne → Circle)
- `manager` (ManyToOne → User) - Zone Lead/Manager
- `active` (Boolean)
- `areas` (OneToMany relationship)
- `createdAt`, `updatedAt` (Audit fields)

### 4. Area
- `id` (Long, Auto-generated)
- `code` (String, Unique) - e.g., "BHR"
- `name` (String) - e.g., "Behrampur"
- `description` (String, Optional)
- `zone` (ManyToOne → Zone)
- `manager` (ManyToOne → User) - Area Lead/Manager
- `active` (Boolean)
- `users` (OneToMany relationship)
- `createdAt`, `updatedAt` (Audit fields)

### 5. User (Updated)
- `id` (Long, Auto-generated)
- `employeeId` (String, Unique) - **Custom employee/user ID for login**
- `username` (String, Unique)
- `passwordHash` (String) - BCrypt hashed
- `email` (String) - Can be encrypted using EncryptionUtil
- `fullName` (String, Optional)
- `phone` (String, Optional) - Can be encrypted using EncryptionUtil
- `userType` (String) - AREA_LEAD, ZONE_LEAD, CIRCLE_LEAD, CLUSTER_LEAD, ANALYST, ADMIN, etc.
- `area` (ManyToOne → Area) - **Required relationship**
- `role` (String) - ADMIN, MANAGER, ANALYST (for access control)
- `active` (Boolean)
- `twoFactorEnabled` (Boolean)
- `createdAt`, `updatedAt` (Audit fields)

## Repositories Created

- `ClusterRepository` - Find by code, check existence
- `CircleRepository` - Find by code, find by clusterId
- `ZoneRepository` - Find by code, find by circleId
- `AreaRepository` - Find by code, find by zoneId
- `UserRepository` (Updated) - Find by employeeId, username, email; find by areaId

## Authentication System

### JWT Implementation
- **JWT Utility** (`JwtUtil.java`)
  - Generates JWT tokens with employeeId, username, userType, userId
  - Validates tokens
  - Extracts claims (username, employeeId, userType, userId)
  - Configurable expiration (default: 24 hours)

### Password Security
- **BCrypt Password Encoding** (Strength factor: 12)
- Passwords are hashed before storage
- Password verification during login

### Application-Level Encryption
- **Encryption Utility** (`EncryptionUtil.java`)
  - AES-256 encryption/decryption
  - Can be used to encrypt sensitive data (email, phone) before saving to DB
  - Decrypt when reading from DB
  - Configure encryption key in `application.yaml`

### Security Configuration
- **JWT Authentication Filter** - Validates JWT tokens on each request
- **Security Config** - Updated to use JWT filter
- Public endpoints: `/auth/**`, `/actuator/**`, `/error`
- Protected endpoints: All others (require JWT token)

## API Endpoints

### Authentication

#### POST `/api/auth/login`
**Request:**
```json
{
  "employeeId": "EMP001",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "employeeId": "EMP001",
  "username": "shivam",
  "fullName": "Shivam Kumar",
  "userType": "AREA_LEAD",
  "role": "MANAGER",
  "areaName": "Behrampur",
  "zoneName": "Ghaziabad",
  "circleName": "Uttar Pradesh",
  "clusterName": "Bihar UP"
}
```

#### POST `/api/auth/logout`
- Stateless logout (client removes token)

### Protected Endpoints
All other endpoints require JWT token in Authorization header:
```
Authorization: Bearer <token>
```

## Configuration

### application.yaml
```yaml
app:
  jwt:
    secret: ${JWT_SECRET:your-256-bit-secret-key-change-this-in-production}
    expiration: 86400000 # 24 hours
  encryption:
    secret: ${ENCRYPTION_SECRET:ChangeThisEncryptionKeyInProduction123456}
```

**Important:** Change both secrets in production!

## Next Steps

1. **Create Users Service** - Service to create users with:
   - BCrypt password hashing
   - Email/phone encryption using EncryptionUtil
   - Area assignment validation

2. **Create Hierarchy Management** - Services to manage:
   - Cluster creation/updates
   - Circle creation/updates
   - Zone creation/updates
   - Area creation/updates
   - Manager assignments

3. **User Type Validation** - Validate userType matches hierarchy position:
   - AREA_LEAD users should be managers of areas
   - ZONE_LEAD users should be managers of zones
   - etc.

4. **Frontend Integration** - Update frontend to:
   - Use employeeId for login
   - Store JWT token
   - Send token in Authorization header
   - Handle token expiration

## Dependencies Added

- `jjwt-api` (v0.12.3) - JWT API
- `jjwt-impl` (v0.12.3) - JWT Implementation
- `jjwt-jackson` (v0.12.3) - JWT Jackson support
- BCrypt (included in Spring Security)

## Notes

1. **Email/Phone Encryption**: Currently, email and phone are not automatically encrypted. Use `EncryptionUtil.encrypt()` when creating/updating users and `EncryptionUtil.decrypt()` when reading.

2. **User Creation**: When creating users, ensure:
   - Password is hashed using `PasswordEncoder`
   - Email/phone are encrypted using `EncryptionUtil` (optional but recommended)
   - Area relationship is set
   - EmployeeId is unique

3. **Manager Relationships**: Circle, Zone, and Area entities have optional `manager` relationships to User. Assign managers when creating/updating these entities.

4. **JWT Token**: Token contains employeeId, username, userType, and userId. Use these for authorization logic.
