# Reporting and Export System

## Overview

The Ops Hub reporting system provides secure, permission-based report viewing and asynchronous export functionality with data-level access control based on user roles and customer allocations.

## Key Features

1. **Permission-Based Access**: Only users with `VIEW_REPORTS` can view reports, and only users with `EXPORT_REPORTS` can export
2. **Asynchronous Exports**: All exports are processed asynchronously to prevent blocking requests
3. **Data-Level Access Control**: Report data is filtered based on user role and customer allocation
4. **Export Tracking**: All exports are tracked with status, history, and metadata
5. **Multiple Formats**: Supports CSV, Excel, JSON, and PDF exports

## Architecture

### Components

1. **ReportService** (`com.company.ops_hub_api.service.ReportService`)
   - Handles report viewing with permission checks
   - Applies data-level access control
   - Executes report queries with user-specific filters

2. **ExportService** (`com.company.ops_hub_api.service.ExportService`)
   - Handles asynchronous report exports
   - Tracks export status and history
   - Generates files in multiple formats

3. **ReportDataFilter** (`com.company.ops_hub_api.service.ReportDataFilter`)
   - Filters report data based on user role and allocation
   - Builds SQL WHERE clauses for access control
   - Applies in-memory filtering when needed

4. **ReportController** (`com.company.ops_hub_api.controller.ReportController`)
   - REST endpoints for reports and exports
   - Protected with `@RequiresPermission` annotations

## Permission Requirements

### View Reports
- **Permission**: `VIEW_REPORTS`
- **Required for**: Viewing available reports, accessing report data

### Export Reports
- **Permission**: `EXPORT_REPORTS`
- **Required for**: Requesting report exports

## Data-Level Access Control

Report data is filtered based on user role and customer allocation:

### Admin / Cluster Lead
- **Access**: All customers (no filter)

### Circle Lead
- **Access**: Customers in their circle

### Zone Lead
- **Access**: Customers in their zone

### Area Lead / Agent
- **Access**: Only allocated customers

### Implementation

The `ReportDataFilter` component:
1. Determines accessible customer IDs based on user role
2. Injects SQL WHERE clauses for database-level filtering
3. Applies in-memory filtering for post-query data

## Report Access Control

Reports can be restricted by role using the `access_roles` field (JSON array):

```json
["ADMIN", "MANAGER", "LEAD"]
```

If `access_roles` is null or empty, the report is accessible to all users with `VIEW_REPORTS` permission.

## Export Workflow

### 1. Request Export

```http
POST /api/reports/exports
Authorization: Bearer <token>
Content-Type: application/json

{
  "reportId": 1,
  "exportFormat": "CSV",
  "parameters": {},
  "filters": {}
}
```

**Response:**
```json
{
  "id": 123,
  "reportId": 1,
  "exportFormat": "CSV",
  "exportStatus": "PENDING",
  "exportedAt": "2026-01-12T10:00:00",
  "downloadUrl": "/api/reports/exports/123/download"
}
```

### 2. Check Export Status

```http
GET /api/reports/exports/123
Authorization: Bearer <token>
```

**Response (Processing):**
```json
{
  "id": 123,
  "exportStatus": "PROCESSING",
  "exportedAt": "2026-01-12T10:00:00"
}
```

**Response (Completed):**
```json
{
  "id": 123,
  "exportStatus": "COMPLETED",
  "filePath": "./exports/customer_report_20260112_100000.csv",
  "fileName": "customer_report_20260112_100000.csv",
  "fileSize": 102400,
  "recordCount": 500,
  "completedAt": "2026-01-12T10:00:05",
  "downloadUrl": "/api/reports/exports/123/download"
}
```

### 3. Download Export

```http
GET /api/reports/exports/123/download
Authorization: Bearer <token>
```

Returns the exported file.

## Export Status Flow

```
PENDING → PROCESSING → COMPLETED
                ↓
              FAILED
```

- **PENDING**: Export request created, queued for processing
- **PROCESSING**: Export is being generated
- **COMPLETED**: Export file generated successfully
- **FAILED**: Export generation failed

## API Endpoints

### Reports

- `GET /api/reports` - Get all available reports
- `GET /api/reports/{reportId}` - Get report details
- `POST /api/reports/{reportId}/data` - Get report data

### Exports

- `POST /api/reports/exports` - Request report export
- `GET /api/reports/exports/{exportId}` - Get export status
- `GET /api/reports/exports/my-exports` - Get user's exports
- `GET /api/reports/{reportId}/exports` - Get exports for a report
- `GET /api/reports/exports/{exportId}/download` - Download export file

## Export Formats

### CSV
- Simple comma-separated values
- Suitable for Excel import
- Fast generation

### Excel (.xlsx)
- **Note**: Currently generates CSV with .xlsx extension
- **Production**: Install Apache POI for proper Excel support

### JSON
- Pretty-printed JSON format
- Includes full report metadata
- Suitable for API consumption

### PDF
- **Note**: Currently generates text file
- **Production**: Install iText or Apache PDFBox for proper PDF support

## Configuration

### Export Directory

Configure export file storage location in `application.yaml`:

```yaml
app:
  exports:
    directory: ./exports  # Default: ./exports
```

For production, use an absolute path or cloud storage (S3, Azure Blob, etc.).

## Data Filtering Examples

### Example 1: Customer Report

**User**: Area Lead (EMP001)
**Allocated Customers**: [101, 102, 103]

**SQL Query Applied:**
```sql
SELECT * FROM customers WHERE id IN (101, 102, 103)
```

### Example 2: Payment Report

**User**: Zone Lead
**Zone**: Ghaziabad

**SQL Query Applied:**
```sql
SELECT p.*, c.* 
FROM payments p
JOIN customers c ON p.customer_id = c.id
WHERE c.zone_id = (SELECT zone_id FROM users WHERE id = :userId)
```

## Best Practices

1. **Always Check Permissions**: Never bypass permission checks
2. **Filter at Database Level**: Use SQL WHERE clauses for efficiency
3. **Async Exports**: Always use async exports for large reports
4. **Monitor Export Status**: Check export status before downloading
5. **Clean Up Old Exports**: Implement cleanup job for old export files
6. **Secure File Storage**: Store exports in secure, access-controlled locations
7. **Rate Limiting**: Consider rate limiting for export requests
8. **File Size Limits**: Set maximum file size limits for exports

## Security Considerations

1. **Permission Enforcement**: All endpoints check permissions
2. **Data-Level Access**: Users only see data they're authorized to access
3. **Export Access**: Users can only download their own exports
4. **SQL Injection Prevention**: Use parameterized queries (implemented in production)
5. **File Path Validation**: Validate file paths to prevent directory traversal

## Audit Logging

All report and export actions are logged:

- **VIEW_REPORT**: When user views report data
- **EXPORT_REQUESTED**: When export is requested
- **EXPORT_COMPLETED**: When export completes successfully
- **EXPORT_FAILED**: When export fails

## Future Enhancements

- [ ] Scheduled reports (cron-based)
- [ ] Email export delivery
- [ ] Report templates
- [ ] Custom report builder
- [ ] Report caching
- [ ] Cloud storage integration (S3, Azure Blob)
- [ ] Real-time export progress updates (WebSocket)
- [ ] Export file expiration and cleanup
- [ ] Report sharing and collaboration
- [ ] Advanced filtering and sorting

## Troubleshooting

### Export Stuck in PROCESSING

1. Check application logs for errors
2. Verify export service is running
3. Check file system permissions
4. Review export record in database

### Export Returns Empty Data

1. Verify user has allocated customers (for Area Lead/Agent)
2. Check report query SQL
3. Verify data-level access control is working
4. Check report parameters and filters

### Permission Denied

1. Verify user has `VIEW_REPORTS` or `EXPORT_REPORTS` permission
2. Check user's role assignments
3. Verify report's `access_roles` configuration
