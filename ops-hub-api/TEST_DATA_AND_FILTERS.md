# Test Data & Filter API Documentation

## Overview

Test data seeding and hierarchical filter endpoints have been implemented.

## Test Data Created

### Hierarchical Structure

#### 3 Clusters:
1. **BUP** - Bihar UP
2. **MH** - Maharashtra  
3. **DL** - Delhi

#### 5 Circles:
1. **UP** - Uttar Pradesh (in BUP cluster)
2. **BR** - Bihar (in BUP cluster)
3. **MUM** - Mumbai (in MH cluster)
4. **PUN** - Pune (in MH cluster)
5. **DEL** - Delhi NCR (in DL cluster)

#### 8 Zones:
1. **GZB** - Ghaziabad (in UP circle)
2. **LKO** - Lucknow (in UP circle)
3. **PTN** - Patna (in BR circle)
4. **MUM** - Mumbai City (in MUM circle)
5. **NVM** - Navi Mumbai (in MUM circle)
6. **PUN** - Pune City (in PUN circle)
7. **NDL** - North Delhi (in DEL circle)
8. **SDL** - South Delhi (in DEL circle)

#### 18 Areas:
- **Ghaziabad Zone**: Behrampur, Meerut, Aligarh
- **Lucknow Zone**: Gomti Nagar, Hazratganj
- **Patna Zone**: Danapur, Kankarbagh
- **Mumbai City Zone**: Andheri, Bandra
- **Navi Mumbai Zone**: Vashi, Kharghar
- **Pune City Zone**: Hinjewadi, Baner
- **North Delhi Zone**: Rohini, Pitampura
- **South Delhi Zone**: Saket, Vasant Kunj

### Test Users Created

| Employee ID | Username | Full Name | User Type | Role | Area | Password |
|------------|----------|-----------|-----------|------|------|----------|
| EMP001 | shivam | Shivam Kumar | AREA_LEAD | MANAGER | Behrampur | password123 |
| EMP002 | rahul | Rahul Sharma | ZONE_LEAD | MANAGER | Behrampur | password123 |
| EMP003 | priya | Priya Singh | CIRCLE_LEAD | MANAGER | Behrampur | password123 |
| EMP004 | admin | Admin User | ADMIN | ADMIN | Behrampur | admin123 |
| EMP005 | analyst1 | Analyst One | ANALYST | ANALYST | Meerut | password123 |
| EMP006 | manager1 | Manager One | ZONE_LEAD | MANAGER | Meerut | password123 |

### Manager Assignments:
- **Behrampur Area**: Managed by EMP001 (shivam - AREA_LEAD)
- **Ghaziabad Zone**: Managed by EMP002 (rahul - ZONE_LEAD)
- **UP Circle**: Managed by EMP003 (priya - CIRCLE_LEAD)

## Filter API Endpoints

### 1. Get All Clusters with Full Hierarchy
**GET** `/api/filters/clusters`

**Response:**
```json
[
  {
    "id": 1,
    "code": "BUP",
    "name": "Bihar UP",
    "circles": [
      {
        "id": 1,
        "code": "UP",
        "name": "Uttar Pradesh",
        "clusterId": 1,
        "zones": [
          {
            "id": 1,
            "code": "GZB",
            "name": "Ghaziabad",
            "circleId": 1,
            "areas": [
              {
                "id": 1,
                "code": "BHR",
                "name": "Behrampur",
                "zoneId": 1
              }
            ]
          }
        ]
      }
    ]
  }
]
```

### 2. Get Circles by Cluster
**GET** `/api/filters/circles?clusterId=1`

**Response:**
```json
[
  {
    "id": 1,
    "code": "UP",
    "name": "Uttar Pradesh",
    "clusterId": 1,
    "zones": [
      {
        "id": 1,
        "code": "GZB",
        "name": "Ghaziabad",
        "circleId": 1,
        "areas": [...]
      }
    ]
  }
]
```

### 3. Get Zones by Circle
**GET** `/api/filters/zones?circleId=1`

**Response:**
```json
[
  {
    "id": 1,
    "code": "GZB",
    "name": "Ghaziabad",
    "circleId": 1,
    "areas": [
      {
        "id": 1,
        "code": "BHR",
        "name": "Behrampur",
        "zoneId": 1
      }
    ]
  }
]
```

### 4. Get Areas by Zone
**GET** `/api/filters/areas?zoneId=1`

**Response:**
```json
[
  {
    "id": 1,
    "code": "BHR",
    "name": "Behrampur",
    "zoneId": 1
  },
  {
    "id": 2,
    "code": "MRT",
    "name": "Meerut",
    "zoneId": 1
  }
]
```

## Usage Examples

### Frontend Filter Implementation

```javascript
// 1. Load all clusters with full hierarchy
const clusters = await fetch('/api/filters/clusters');

// 2. When user selects a cluster, show its circles
const circles = await fetch(`/api/filters/circles?clusterId=${selectedClusterId}`);

// 3. When user selects a circle, show its zones
const zones = await fetch(`/api/filters/zones?circleId=${selectedCircleId}`);

// 4. When user selects a zone, show its areas
const areas = await fetch(`/api/filters/areas?zoneId=${selectedZoneId}`);
```

### Login Test Example

```bash
# Test login with Employee ID
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "EMP001",
    "password": "password123"
  }'
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

## Data Seeding

### Automatic Seeding
- Data is seeded automatically on application startup
- Seeder only runs if no data exists (checks if clusters table is empty)
- Prevents duplicate data on restart

### Manual Re-seeding
To re-seed data:
1. Delete all records from tables (in order: users → areas → zones → circles → clusters)
2. Restart the application

Or temporarily disable the check in `DataSeederService.java`:
```java
if (clusterRepository.count() > 0) {
    log.info("Data already exists. Skipping seed data.");
    return; // Remove or comment this to force re-seed
}
```

## Security Notes

1. **Passwords**: All passwords are hashed using BCrypt (strength factor: 12)
2. **Email/Phone**: Stored encrypted using AES-256 encryption
3. **JWT Tokens**: Tokens expire after 24 hours
4. **Filter Endpoints**: Public (no authentication required) for frontend flexibility

## Test Credentials Summary

| Employee ID | Password | User Type | Location |
|------------|----------|-----------|----------|
| EMP001 | password123 | AREA_LEAD | Behrampur |
| EMP002 | password123 | ZONE_LEAD | Behrampur |
| EMP003 | password123 | CIRCLE_LEAD | Behrampur |
| EMP004 | admin123 | ADMIN | Behrampur |
| EMP005 | password123 | ANALYST | Meerut |
| EMP006 | password123 | ZONE_LEAD | Meerut |

## Next Steps

1. **Frontend Integration**: Use filter endpoints to build cascading dropdowns
2. **User Management**: Create APIs to manage users, areas, zones, circles, clusters
3. **Authorization**: Add role-based access control based on userType and hierarchy
4. **Reporting**: Use hierarchy structure for filtered reporting
