# API Library

This directory contains API utility functions.

## api.js

Centralized API client for backend communication.

### Available Methods

#### Authentication
- `api.login(employeeId, password)` - Login and get JWT token

#### Filters
- `api.getClusters()` - Get all clusters with hierarchy
- `api.getCircles(clusterId)` - Get circles by cluster
- `api.getZones(circleId)` - Get zones by circle
- `api.getAreas(zoneId)` - Get areas by zone

#### Dashboard
- `api.getDashboard(token)` - Get dashboard data (requires JWT token)

### Configuration

Set `NEXT_PUBLIC_API_URL` environment variable to change API base URL.
Default: `http://localhost:8080/api`

### Usage

```javascript
import { api } from "@/lib/api";

// Login
const response = await api.login("EMP001", "password123");

// Get filters
const clusters = await api.getClusters();
const circles = await api.getCircles(1);
const zones = await api.getZones(1);
const areas = await api.getAreas(1);
```
