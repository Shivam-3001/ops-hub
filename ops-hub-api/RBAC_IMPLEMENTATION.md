# RBAC Implementation Documentation

## Overview
This document describes the Role-Based Access Control (RBAC) implementation for the Ops Hub application. The system uses permission-based authorization rather than role-only checks, providing fine-grained access control.

## Architecture

### Backend Components

#### 1. **UserPrincipal** (`security/UserPrincipal.java`)
- Custom `UserDetails` implementation
- Stores user, permissions, and roles
- Provides methods for permission/role checking
- Used by Spring Security for authentication

#### 2. **CustomUserDetailsService** (`service/CustomUserDetailsService.java`)
- Implements Spring Security's `UserDetailsService`
- Loads user with roles and permissions from database
- Builds `UserPrincipal` with all user permissions

#### 3. **PermissionService** (`service/PermissionService.java`)
- Centralized permission checking service
- Provides utility methods for permission/role validation
- Can be injected into controllers/services for programmatic checks

#### 4. **RequiresPermission Annotation** (`security/RequiresPermission.java`)
- Method-level annotation for permission-based authorization
- Supports single or multiple permissions
- `requireAll` flag for AND/OR logic

#### 5. **PermissionAspect** (`security/PermissionAspect.java`)
- AOP aspect that intercepts `@RequiresPermission` annotations
- Validates user permissions before method execution
- Throws `AccessDeniedException` if permission check fails

#### 6. **JwtAuthenticationFilter** (`security/JwtAuthenticationFilter.java`)
- Updated to use `CustomUserDetailsService`
- Loads user with full permission set on authentication
- Sets `UserPrincipal` in security context

### Database Schema

#### Tables
- **users**: User accounts
- **roles**: Role definitions (ADMIN, LEAD, AGENT)
- **permissions**: Permission definitions
- **role_permissions**: Many-to-many mapping between roles and permissions
- **user_roles**: Many-to-many mapping between users and roles

#### Seed Data
The `RbacDataSeeder` creates:
- **15 Permissions**: VIEW_CUSTOMERS, ASSIGN_CUSTOMERS, COLLECT_PAYMENT, etc.
- **3 Roles**: ADMIN, LEAD, AGENT
- **Role-Permission Mappings**:
  - ADMIN: All permissions
  - LEAD: Most permissions (except user management)
  - AGENT: Basic operational permissions

## Usage Examples

### Backend - Method-Level Authorization

```java
@RestController
@RequestMapping("/customers")
public class CustomerController {

    @GetMapping
    @RequiresPermission("VIEW_CUSTOMERS")
    public ResponseEntity<List<Customer>> getAllCustomers() {
        // Only users with VIEW_CUSTOMERS permission can access
        return ResponseEntity.ok(customerService.findAll());
    }

    @PostMapping
    @RequiresPermission({"MANAGE_CUSTOMERS", "ASSIGN_CUSTOMERS"})
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        // User needs either MANAGE_CUSTOMERS OR ASSIGN_CUSTOMERS
        return ResponseEntity.ok(customerService.create(customer));
    }

    @PutMapping("/{id}")
    @RequiresPermission(value = {"MANAGE_CUSTOMERS", "ASSIGN_CUSTOMERS"}, requireAll = true)
    public ResponseEntity<Customer> updateCustomer(@PathVariable Long id, @RequestBody Customer customer) {
        // User needs BOTH permissions
        return ResponseEntity.ok(customerService.update(id, customer));
    }
}
```

### Backend - Programmatic Checks

```java
@Service
public class CustomerService {

    private final PermissionService permissionService;

    public void deleteCustomer(Long id) {
        if (!permissionService.hasPermission("MANAGE_CUSTOMERS")) {
            throw new AccessDeniedException("Insufficient permissions");
        }
        // Delete logic
    }

    public List<Customer> getCustomersForUser() {
        if (permissionService.hasAnyPermission("VIEW_CUSTOMERS", "MANAGE_CUSTOMERS")) {
            return customerRepository.findAll();
        }
        return customerRepository.findByAssignedUser(getCurrentUserId());
    }
}
```

### Frontend - React Components

```jsx
import PermissionGuard from '@/components/PermissionGuard';
import { usePermissions } from '@/hooks/usePermissions';

function CustomerPage() {
  const { hasPermission } = usePermissions();

  return (
    <div>
      <h1>Customers</h1>
      
      {/* Show button only if user has permission */}
      <PermissionGuard permission="MANAGE_CUSTOMERS">
        <button>Add Customer</button>
      </PermissionGuard>

      {/* Show export button if user has any of these permissions */}
      <PermissionGuard permission={["EXPORT_REPORTS", "VIEW_REPORTS"]}>
        <button>Export</button>
      </PermissionGuard>

      {/* Conditional rendering */}
      {hasPermission("VIEW_CUSTOMERS") && (
        <CustomerList />
      )}
    </div>
  );
}
```

### Frontend - Menu Visibility

```jsx
import { usePermissions } from '@/hooks/usePermissions';

function Sidebar() {
  const { hasPermission, hasAnyPermission } = usePermissions();

  return (
    <nav>
      {hasPermission("VIEW_CUSTOMERS") && (
        <Link href="/customers">Customers</Link>
      )}
      {hasPermission("VIEW_REPORTS") && (
        <Link href="/reports">Reports</Link>
      )}
      {hasAnyPermission("MANAGE_USERS", "VIEW_ROLES") && (
        <Link href="/admin">Admin</Link>
      )}
    </nav>
  );
}
```

## API Endpoints

### Get Current User Permissions
```
GET /api/permissions/me
Authorization: Bearer <token>

Response:
{
  "userId": 1,
  "employeeId": "EMP001",
  "username": "John Doe",
  "roles": ["ADMIN"],
  "permissions": ["VIEW_CUSTOMERS", "MANAGE_CUSTOMERS", ...]
}
```

### Get All Permissions (Requires VIEW_PERMISSIONS)
```
GET /api/permissions/all
Authorization: Bearer <token>
```

### Get All Roles (Requires VIEW_ROLES)
```
GET /api/permissions/roles
Authorization: Bearer <token>
```

## Permission Codes

### Customer Management
- `VIEW_CUSTOMERS`: View customer information
- `ASSIGN_CUSTOMERS`: Assign customers to users
- `MANAGE_CUSTOMERS`: Create, update, delete customers

### Payments
- `COLLECT_PAYMENT`: Collect payments from customers
- `VIEW_PAYMENTS`: View payment transactions

### Visits
- `VIEW_VISITS`: View customer visit records
- `CREATE_VISITS`: Create customer visit records

### Reports
- `VIEW_REPORTS`: View reports and analytics
- `EXPORT_REPORTS`: Export reports in various formats

### Profile Management
- `APPROVE_PROFILE`: Approve user profile updates

### AI Features
- `USE_AI_AGENT`: Access and use AI assistant features

### Administration
- `VIEW_PERMISSIONS`: View roles and permissions
- `VIEW_ROLES`: View role definitions
- `MANAGE_USERS`: Create, update, and delete users
- `MANAGE_SETTINGS`: Manage application settings

## Role Definitions

### ADMIN
- **Description**: Full system access with all permissions
- **Permissions**: All 15 permissions
- **Use Case**: System administrators

### LEAD
- **Description**: Team lead with management permissions
- **Permissions**: 13 permissions (excludes MANAGE_USERS, MANAGE_SETTINGS)
- **Use Case**: Team leads, managers

### AGENT
- **Description**: Field agent with basic operational permissions
- **Permissions**: 7 basic permissions
- **Use Case**: Field agents, analysts

## Security Features

1. **Permission-Based Authorization**: Fine-grained control at method level
2. **AOP Integration**: Automatic permission checking via annotations
3. **JWT Integration**: Permissions loaded on authentication
4. **Frontend Guards**: React components for UI-level access control
5. **Centralized Service**: Reusable permission checking logic
6. **Data-Driven**: Permissions managed in database, no code changes needed

## Best Practices

1. **Use Permissions, Not Roles**: Always check permissions, not roles
2. **Least Privilege**: Grant minimum required permissions
3. **Consistent Naming**: Use RESOURCE_ACTION format (e.g., VIEW_CUSTOMERS)
4. **Document Permissions**: Document what each permission allows
5. **Test Authorization**: Write tests for permission checks
6. **Audit Access**: Log permission checks for security auditing

## Extending the System

### Adding a New Permission

1. Add permission to database (or via seeder):
```java
Permission newPermission = createPermission(
    "NEW_PERMISSION", 
    "New Permission Name", 
    "Description", 
    "RESOURCE", 
    "ACTION"
);
```

2. Map to roles in `RbacDataSeeder`
3. Use in controllers/services with `@RequiresPermission`

### Adding a New Role

1. Create role in database
2. Map permissions to role
3. Assign role to users via `user_roles` table

## Error Handling

- **Access Denied**: Returns 403 Forbidden with clear error message
- **Unauthenticated**: Returns 401 Unauthorized
- **Missing Permission**: Logged and access denied

## Testing

```java
@SpringBootTest
class PermissionTest {
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testAdminCanAccessAll() {
        // Test admin access
    }
    
    @Test
    @WithMockUser(authorities = "PERMISSION_VIEW_CUSTOMERS")
    void testPermissionCheck() {
        // Test specific permission
    }
}
```
