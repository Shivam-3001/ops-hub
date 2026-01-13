# Frontend Implementation Guide

## Overview

The Ops Hub frontend is built with Next.js 16 and React 19, following the same structured approach as the backend. All components are permission-aware and integrate seamlessly with the backend API.

## Architecture

### Core Infrastructure

1. **API Client** (`src/lib/api.js`)
   - Centralized API client with authentication
   - Automatic token management
   - Error handling and 401 redirects
   - Methods for all backend endpoints

2. **Auth Context** (`src/contexts/AuthContext.js`)
   - Global authentication state management
   - User data and permissions
   - Login/logout functionality
   - Permission checking utilities

3. **Layout Components** (`src/components/Layout/`)
   - `Sidebar.js` - Navigation sidebar with permission-based menu items
   - `Header.js` - Top header with user info and logout
   - `AppLayout.js` - Main layout wrapper with protected routes

4. **Permission Guard** (`src/components/PermissionGuard.js`)
   - Conditional rendering based on permissions
   - Supports single or multiple permissions
   - Fallback content support

5. **Protected Route** (`src/components/ProtectedRoute.js`)
   - Redirects unauthenticated users to login
   - Loading state handling

## Pages Implemented

### 1. Login (`/login`)
- Employee ID and password authentication
- Error handling and validation
- Redirects to dashboard on success
- Beautiful, modern UI

### 2. Dashboard (`/dashboard`)
- Overview statistics
- Quick actions
- Activity feed
- Uses `AppLayout` wrapper

### 3. Customers (`/customers`)
- List all customers (permission: `VIEW_CUSTOMERS`)
- Search functionality
- Customer details view
- Permission-protected

### 4. Allocations (`/allocations`)
- View customer allocations (permission: `ASSIGN_CUSTOMERS`)
- Allocate new customers
- Filter and search

### 5. Field Visits (`/visits`)
- View visit history (permission: `VIEW_VISITS`)
- Create new visits (permission: `CREATE_VISITS`)
- Visit status tracking

### 6. Payments (`/payments`)
- View payment history (permission: `VIEW_PAYMENTS`)
- Initiate payments (permission: `COLLECT_PAYMENT`)
- Payment status tracking

### 7. Reports (`/reports`)
- View available reports (permission: `VIEW_REPORTS`)
- View report data
- Export reports (permission: `EXPORT_REPORTS`)

### 8. AI Assistant (`/ai-assistant`)
- Chat interface (permission: `USE_AI_AGENT`)
- Suggested actions
- Action execution with confirmation

### 9. Profile (`/profile`)
- View profile information
- Submit profile update requests
- View request status

## Permission-Based Navigation

The sidebar automatically shows/hides menu items based on user permissions:

```javascript
{
  label: 'Customers',
  href: '/customers',
  permission: 'VIEW_CUSTOMERS', // Only shown if user has this permission
}
```

## API Integration

All API calls use the centralized `api` client:

```javascript
import api from '@/lib/api';

// Login
const response = await api.login(employeeId, password);

// Get customers
const customers = await api.getCustomers();

// Initiate payment
await api.initiatePayment({ customerId, amount, paymentMethod: 'UPI', upiId });
```

## Authentication Flow

1. User enters Employee ID and password
2. `api.login()` called
3. Token stored in localStorage
4. User data stored in localStorage
5. Permissions loaded via `AuthContext`
6. Redirect to dashboard
7. All subsequent requests include token in Authorization header

## Permission Checking

### In Components

```javascript
import PermissionGuard from '@/components/PermissionGuard';

<PermissionGuard permission="VIEW_CUSTOMERS">
  <CustomerList />
</PermissionGuard>
```

### In Code

```javascript
import { useAuth } from '@/contexts/AuthContext';

const { hasPermission, hasAnyPermission } = useAuth();

if (hasPermission('COLLECT_PAYMENT')) {
  // Show payment button
}
```

## Styling

- **Framework**: Tailwind CSS 4
- **Design**: Minimalist, modern, engaging
- **Color Scheme**: Slate-based with accent colors
- **Responsive**: Mobile-first design

## Component Structure

```
src/
├── app/                    # Next.js app router pages
│   ├── login/
│   ├── dashboard/
│   ├── customers/
│   ├── payments/
│   ├── reports/
│   ├── ai-assistant/
│   └── profile/
├── components/
│   ├── Layout/            # Layout components
│   ├── PermissionGuard.js
│   └── ProtectedRoute.js
├── contexts/
│   └── AuthContext.js     # Authentication context
├── hooks/
│   └── usePermissions.js # Permission utilities
└── lib/
    ├── api.js            # API client
    └── permissions.js    # Permission utilities
```

## Environment Variables

Create `.env.local`:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

## Running the Application

```bash
cd ops-hub-web
npm install
npm run dev
```

The application will run on `http://localhost:3000`

## Features

### ✅ Implemented

- [x] Authentication with Employee ID
- [x] Permission-based navigation
- [x] Protected routes
- [x] Dashboard with stats
- [x] Customer management
- [x] Customer allocations
- [x] Field visits
- [x] Payment processing
- [x] Reports viewing
- [x] AI Assistant chat
- [x] Profile management

### 🚧 To Be Enhanced

- [ ] Export functionality UI
- [ ] Profile approval workflow UI (for admins)
- [ ] Customer detail pages
- [ ] Payment detail pages
- [ ] Visit detail pages with reviews
- [ ] Report export status tracking
- [ ] Real-time notifications
- [ ] Dark mode toggle
- [ ] Chart visualizations
- [ ] Data tables with pagination

## Best Practices

1. **Always use PermissionGuard** for permission-based UI
2. **Use AppLayout** for all authenticated pages
3. **Handle loading and error states** in all components
4. **Use the api client** for all API calls
5. **Follow the same structure** as backend components
6. **Keep components focused** - one responsibility per component
7. **Use TypeScript** (optional, but recommended for production)

## Testing

Test with different user roles:

- **Admin (EMP004)**: Should see all menu items
- **Lead (EMP001)**: Should see most items (excludes Settings)
- **Agent (EMP005)**: Should see basic items only

## Next Steps

1. Add more detailed pages (customer detail, payment detail, etc.)
2. Implement export status tracking UI
3. Add profile approval workflow for admins
4. Enhance AI assistant with better UI
5. Add data visualization charts
6. Implement real-time updates
7. Add form validation
8. Add loading skeletons
9. Add error boundaries
10. Add toast notifications
