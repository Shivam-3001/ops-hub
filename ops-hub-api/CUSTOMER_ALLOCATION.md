# Customer Allocation Implementation Documentation

## Overview
This document describes the customer allocation system for the Ops Hub application. The system allows authorized users to assign customers to specific users (typically agents) and ensures that users can only view customers allocated to them.

## Architecture

### Components

#### 1. **CustomerAllocationService** (`service/CustomerAllocationService.java`)
- Handles customer allocation, reassignment, and deallocation
- Enforces permission and role checks
- Prevents duplicate active allocations
- Manages transactional operations
- Integrates with audit logging

#### 2. **CustomerService** (`service/CustomerService.java`)
- Provides customer retrieval with permission-based filtering
- Users with `VIEW_CUSTOMERS` permission see all customers
- Users without permission see only their allocated customers
- Enforces access control at service level

#### 3. **CustomerAllocationController** (`controller/CustomerAllocationController.java`)
- REST endpoints for allocation management
- Permission-protected endpoints using `@RequiresPermission`
- Supports allocation, reassignment, and deallocation

#### 4. **CustomerController** (`controller/CustomerController.java`)
- REST endpoints for customer retrieval
- Automatically filters based on user permissions

### Database Tables

- **customers**: Customer master data
- **customer_allocations**: Allocation records with status tracking
- **users**: User accounts
- **user_roles**: User-role mappings for role validation
- **audit_logs**: Logs all allocation actions

## Workflow

### 1. Allocation Flow

```
Authorized User → Allocate Customer → Validate Role → Create Allocation → Audit Log
```

**Steps:**
1. User with `ASSIGN_CUSTOMERS` permission submits allocation request
2. System validates target user has required role (e.g., AGENT)
3. System checks for existing active allocation (prevents duplicates)
4. New allocation created with status `ACTIVE`
5. Audit log entry created
6. Allocation saved

### 2. Reassignment Flow

```
Authorized User → Reassign Customer → Deactivate Old Allocations → Create New Allocation → Audit Log
```

**Steps:**
1. User with `ASSIGN_CUSTOMERS` permission submits reassignment request
2. System validates new user has required role
3. All existing active allocations for customer are deactivated
4. New allocation created with status `ACTIVE`
5. Old allocations marked as `INACTIVE` with deallocation reason
6. Audit log entry created

### 3. Deallocation Flow

```
Authorized User → Deallocate Customer → Deactivate Allocation → Audit Log
```

**Steps:**
1. User with `ASSIGN_CUSTOMERS` permission deallocates customer
2. Active allocation found and deactivated
3. Status changed to `INACTIVE` with deallocation reason
4. Audit log entry created

### 4. Customer Viewing Flow

```
User → Request Customers → Check Permissions → Return Filtered Results
```

**Steps:**
1. User requests customer list
2. System checks if user has `VIEW_CUSTOMERS` permission
3. If yes: Return all customers
4. If no: Return only customers allocated to user

## API Endpoints

### Allocate Customer (Requires ASSIGN_CUSTOMERS)
```
POST /api/customer-allocations
Authorization: Bearer <token>
Content-Type: application/json

Request Body:
{
  "customerId": 1,
  "userId": 5,
  "roleCode": "AGENT",
  "allocationType": "PRIMARY",
  "notes": "Initial allocation"
}

Response: 200 OK
{
  "id": 1,
  "customerId": 1,
  "customerCode": "CUST001",
  "userId": 5,
  "userEmployeeId": "EMP005",
  "roleCode": "AGENT",
  "allocationType": "PRIMARY",
  "status": "ACTIVE",
  ...
}
```

### Reassign Customer (Requires ASSIGN_CUSTOMERS)
```
POST /api/customer-allocations/reassign
Authorization: Bearer <token>
Content-Type: application/json

Request Body:
{
  "customerId": 1,
  "newUserId": 6,
  "roleCode": "AGENT",
  "allocationType": "PRIMARY",
  "reason": "Agent transfer",
  "notes": "Reassigned due to team change"
}

Response: 200 OK
{
  "id": 2,
  "customerId": 1,
  "newUserId": 6,
  "status": "ACTIVE",
  ...
}
```

### Deallocate Customer (Requires ASSIGN_CUSTOMERS)
```
DELETE /api/customer-allocations/customers/{customerId}/users/{userId}?reason=Manual deallocation
Authorization: Bearer <token>

Response: 204 No Content
```

### Get Customer Allocations (Requires ASSIGN_CUSTOMERS)
```
GET /api/customer-allocations/customers/{customerId}
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "id": 1,
    "customerId": 1,
    "userId": 5,
    "status": "ACTIVE",
    ...
  }
]
```

### Get My Allocations
```
GET /api/customer-allocations/my-allocations
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "id": 1,
    "customerId": 1,
    "customerCode": "CUST001",
    "status": "ACTIVE",
    ...
  }
]
```

### Get All Customers (Filtered by Permissions)
```
GET /api/customers
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "id": 1,
    "customerCode": "CUST001",
    "firstName": "John",
    ...
  }
]
```

## Permission Requirements

- **Allocate/Reassign/Deallocate**: `ASSIGN_CUSTOMERS` permission
- **View All Customers**: `VIEW_CUSTOMERS` permission
- **View Allocated Customers**: Any authenticated user (sees only their allocations)
- **View All Allocations**: `ASSIGN_CUSTOMERS` permission

## Role Validation

When allocating a customer, the system validates that the target user has the specified role:
- Checks `user_roles` table for role assignment
- Throws `IllegalArgumentException` if user doesn't have required role
- Example: Customer can only be allocated to users with `AGENT` role

## Security Features

1. **Permission-Based Authorization**: Uses `@RequiresPermission` annotation
2. **Role Validation**: Ensures users have required roles before allocation
3. **Duplicate Prevention**: Prevents multiple active allocations for same customer-user pair
4. **Access Control**: Users can only view customers they have access to
5. **Audit Trail**: All actions logged with full context
6. **Transactional Safety**: All operations are transactional

## Business Rules

1. **Single Active Allocation**: A customer can have multiple allocations, but only one active allocation per user
2. **Role Requirement**: Customers can only be allocated to users with specific roles
3. **Reassignment**: Reassigning deactivates all existing active allocations
4. **View Access**: Users without `VIEW_CUSTOMERS` permission see only allocated customers
5. **Allocation Types**: Supports PRIMARY, SECONDARY, TEMPORARY allocation types

## Audit Logging

All allocation actions are logged in `audit_logs` table with:
- Action type (CREATE, REASSIGN, DEALLOCATE)
- Entity type (CUSTOMER_ALLOCATION)
- Entity ID (allocation ID)
- Old and new values (JSON)
- User information
- IP address and user agent
- Request URL and method
- Status (SUCCESS/FAILURE)

## Error Handling

- **Invalid Customer/User**: Returns 400 Bad Request
- **Permission Denied**: Returns 403 Forbidden
- **Duplicate Allocation**: Returns 400 Bad Request
- **Invalid Role**: Returns 400 Bad Request with role information
- **Allocation Not Found**: Returns 404 Not Found
- **Transaction Failures**: Rolled back automatically

## Usage Examples

### Allocate Customer to Agent
```java
AllocateCustomerDTO dto = new AllocateCustomerDTO();
dto.setCustomerId(1L);
dto.setUserId(5L);
dto.setRoleCode("AGENT");
dto.setAllocationType("PRIMARY");
dto.setNotes("Initial assignment");

CustomerAllocation allocation = allocationService.allocateCustomer(dto, httpRequest);
```

### Reassign Customer
```java
ReassignCustomerDTO dto = new ReassignCustomerDTO();
dto.setCustomerId(1L);
dto.setNewUserId(6L);
dto.setRoleCode("AGENT");
dto.setAllocationType("PRIMARY");
dto.setReason("Agent transfer");
dto.setNotes("Reassigned due to team restructuring");

CustomerAllocation allocation = allocationService.reassignCustomer(dto, httpRequest);
```

### Get My Customers (Filtered)
```java
// User without VIEW_CUSTOMERS permission
List<Customer> myCustomers = customerService.getAllCustomers();
// Returns only customers allocated to current user

// User with VIEW_CUSTOMERS permission
List<Customer> allCustomers = customerService.getAllCustomers();
// Returns all customers
```

## Testing

Example test scenarios:

```java
@Test
@WithMockUser(authorities = "PERMISSION_ASSIGN_CUSTOMERS")
void testAllocateCustomer() {
    // Test allocation flow
}

@Test
@WithMockUser(authorities = "PERMISSION_ASSIGN_CUSTOMERS")
void testReassignCustomer() {
    // Test reassignment flow
}

@Test
void testGetMyCustomers() {
    // Test filtered customer retrieval
}
```

## Future Enhancements

1. **Bulk Allocation**: Allocate multiple customers at once
2. **Allocation History**: View complete history of allocations
3. **Auto-Allocation Rules**: Automatic allocation based on rules
4. **Allocation Analytics**: Track allocation metrics and performance
5. **Notification System**: Notify users when customers are allocated/reassigned
6. **Temporary Allocations**: Support time-based temporary allocations
