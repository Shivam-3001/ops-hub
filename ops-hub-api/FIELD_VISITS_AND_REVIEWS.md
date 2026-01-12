# Field Visits and Reviews Implementation Documentation

## Overview
This document describes the field visit and review submission system for the Ops Hub application. The system allows users to submit visits for customers allocated to them and submit reviews linked to those visits.

## Architecture

### Components

#### 1. **CustomerVisitService** (`service/CustomerVisitService.java`)
- Handles visit submission with ownership validation
- Prevents duplicate visits (configurable)
- Validates customer allocation before allowing visits
- Manages visit status updates
- Integrates with audit logging

#### 2. **CustomerReviewService** (`service/CustomerReviewService.java`)
- Handles review submission linked to visits
- Validates visit ownership
- Prevents duplicate reviews for same visit
- Integrates with audit logging

#### 3. **CustomerVisitController** (`controller/CustomerVisitController.java`)
- REST endpoints for visit management
- Supports submission, retrieval, and status updates

#### 4. **CustomerReviewController** (`controller/CustomerReviewController.java`)
- REST endpoints for review management
- Supports submission and retrieval

### Database Tables

- **customer_visits**: Visit records with status, notes, and geo-location
- **customer_reviews**: Review records linked to visits
- **customer_allocations**: Used for ownership validation
- **audit_logs**: Logs all visit and review actions

## Workflow

### 1. Visit Submission Flow

```
User → Validate Allocation → Check Duplicate → Create Visit → Audit Log
```

**Steps:**
1. User submits visit request
2. System validates customer is allocated to user
3. System checks for duplicate visit on same day (if enabled)
4. Visit created with status, notes, and optional geo-location
5. Audit log entry created

### 2. Review Submission Flow

```
User → Validate Visit Ownership → Check Duplicate Review → Create Review → Audit Log
```

**Steps:**
1. User submits review for a visit
2. System validates visit belongs to user
3. System checks if review already exists for visit
4. Review created with rating and text
5. Audit log entry created

## API Endpoints

### Submit Visit
```
POST /api/customer-visits
Authorization: Bearer <token>
Content-Type: application/json

Request Body:
{
  "customerId": 1,
  "visitDate": "2024-01-15T10:30:00",
  "visitStatus": "COMPLETED",
  "notes": "Customer visit completed successfully",
  "visitType": "SCHEDULED",
  "purpose": "Follow-up visit",
  "latitude": 28.6139,
  "longitude": 77.2090,
  "address": "123 Main Street, City"
}

Response: 200 OK
{
  "id": 1,
  "customerId": 1,
  "customerCode": "CUST001",
  "visitDate": "2024-01-15T10:30:00",
  "visitStatus": "COMPLETED",
  ...
}
```

### Get My Visits
```
GET /api/customer-visits/my-visits
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "id": 1,
    "customerId": 1,
    "visitDate": "2024-01-15T10:30:00",
    ...
  }
]
```

### Get Customer Visits
```
GET /api/customer-visits/customers/{customerId}
Authorization: Bearer <token>

Response: 200 OK
[
  {
    "id": 1,
    "customerId": 1,
    "visitDate": "2024-01-15T10:30:00",
    ...
  }
]
```

### Update Visit Status
```
PATCH /api/customer-visits/{id}/status?status=COMPLETED
Authorization: Bearer <token>

Response: 200 OK
{
  "id": 1,
  "visitStatus": "COMPLETED",
  "completedAt": "2024-01-15T11:00:00",
  ...
}
```

### Submit Review
```
POST /api/customer-reviews
Authorization: Bearer <token>
Content-Type: application/json

Request Body:
{
  "visitId": 1,
  "rating": 5,
  "reviewText": "Excellent service and support",
  "reviewCategories": "[\"service\", \"support\"]",
  "isPositive": true
}

Response: 200 OK
{
  "id": 1,
  "visitId": 1,
  "rating": 5,
  "reviewText": "Excellent service and support",
  ...
}
```

### Get Review by Visit
```
GET /api/customer-reviews/visits/{visitId}
Authorization: Bearer <token>

Response: 200 OK
{
  "id": 1,
  "visitId": 1,
  "rating": 5,
  ...
}
```

## Security Features

1. **Ownership Validation**: Users can only submit visits for customers allocated to them
2. **Visit Ownership**: Users can only submit reviews for their own visits
3. **Access Control**: Users can only view their own visits and reviews
4. **Duplicate Prevention**: Configurable duplicate visit prevention per day
5. **Audit Trail**: All actions logged with full context

## Configuration

### Duplicate Visit Prevention

Configured in `application.yaml`:
```yaml
app:
  visits:
    prevent-duplicate-per-day: true  # Set to false to allow multiple visits per day
```

This can be overridden with environment variable:
```bash
PREVENT_DUPLICATE_VISITS=false
```

## Business Rules

1. **Allocation Requirement**: Users can only submit visits for customers allocated to them
2. **Duplicate Prevention**: By default, only one visit per customer per day per user (configurable)
3. **Review Ownership**: Users can only review their own visits
4. **One Review Per Visit**: Each visit can have only one review
5. **Status Management**: Visit status can be updated (SCHEDULED → IN_PROGRESS → COMPLETED)

## Visit Status Flow

```
SCHEDULED → IN_PROGRESS → COMPLETED
                ↓
            CANCELLED
```

- **SCHEDULED**: Visit is planned
- **IN_PROGRESS**: Visit has started
- **COMPLETED**: Visit is finished
- **CANCELLED**: Visit was cancelled

## Review Rating

- Rating scale: 1-5 (required)
- Positive/negative: Automatically determined (rating >= 3 is positive)
- Review text: Optional, up to 2000 characters
- Categories: Optional JSON array

## Audit Logging

All visit and review actions are logged in `audit_logs` table with:
- Action type (CREATE, UPDATE)
- Entity type (CUSTOMER_VISIT, CUSTOMER_REVIEW)
- Entity ID (visit/review ID)
- Old and new values (JSON)
- User information
- IP address and user agent
- Request URL and method
- Status (SUCCESS/FAILURE)

## Error Handling

- **Invalid Customer**: Returns 400 Bad Request
- **Not Allocated**: Returns 403 Forbidden (customer not allocated to user)
- **Duplicate Visit**: Returns 400 Bad Request (if prevention enabled)
- **Duplicate Review**: Returns 400 Bad Request
- **Visit Not Found**: Returns 404 Not Found
- **Access Denied**: Returns 403 Forbidden (visit/review doesn't belong to user)
- **Transaction Failures**: Rolled back automatically

## Offline Support (Extensibility)

The system is designed to support offline functionality:

1. **Local Storage**: Visit data can be stored locally and synced later
2. **Batch Submission**: Multiple visits can be submitted in batch
3. **Conflict Resolution**: Timestamp-based conflict resolution
4. **Sync Status**: Track sync status for offline submissions

**Future Enhancements:**
- Add `syncStatus` field to visits
- Add `offlineId` for local tracking
- Add batch submission endpoint
- Add sync endpoint for offline data

## Usage Examples

### Submit Visit
```java
SubmitVisitDTO dto = new SubmitVisitDTO();
dto.setCustomerId(1L);
dto.setVisitDate(LocalDateTime.now());
dto.setVisitStatus("COMPLETED");
dto.setNotes("Customer visit completed");
dto.setVisitType("SCHEDULED");
dto.setLatitude(new BigDecimal("28.6139"));
dto.setLongitude(new BigDecimal("77.2090"));

CustomerVisit visit = visitService.submitVisit(dto, httpRequest);
```

### Submit Review
```java
SubmitReviewDTO dto = new SubmitReviewDTO();
dto.setVisitId(1L);
dto.setRating(5);
dto.setReviewText("Excellent service");
dto.setIsPositive(true);

CustomerReview review = reviewService.submitReview(dto, httpRequest);
```

## Testing

Example test scenarios:

```java
@Test
void testSubmitVisitForAllocatedCustomer() {
    // Test visit submission for allocated customer
}

@Test
void testSubmitVisitForUnallocatedCustomer() {
    // Test that visit submission fails for unallocated customer
}

@Test
void testDuplicateVisitPrevention() {
    // Test duplicate visit prevention
}

@Test
void testSubmitReviewForOwnVisit() {
    // Test review submission for own visit
}
```

## Future Enhancements

1. **Offline Sync**: Full offline support with sync
2. **Visit Templates**: Pre-defined visit templates
3. **Photo Attachments**: Attach photos to visits
4. **Visit Analytics**: Track visit metrics and trends
5. **Automated Reminders**: Remind users of scheduled visits
6. **Route Optimization**: Optimize visit routes for field agents
