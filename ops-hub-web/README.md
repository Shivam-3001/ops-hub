# Ops Hub Web - Frontend Application

Enterprise-grade operations dashboard frontend built with Next.js 16 and React 19.

## Tech Stack

- **Next.js 16.1.1** - React framework with App Router
- **React 19.2.3** - UI library
- **Tailwind CSS 4** - Styling
- **Next.js App Router** - File-based routing

## Features

### ✅ Implemented

1. **Authentication**
   - Employee ID and password login
   - JWT token management
   - Automatic session handling
   - Protected routes

2. **Permission-Based Access**
   - Permission-aware navigation
   - Conditional UI rendering
   - Role-based menu items

3. **Dashboard**
   - Overview statistics
   - Quick actions
   - Activity feed

4. **Customer Management**
   - View customers list
   - Search functionality
   - Customer details

5. **Customer Allocations**
   - View allocations
   - Allocate customers
   - Filter and manage

6. **Field Visits**
   - View visit history
   - Create new visits
   - Visit status tracking

7. **Payments**
   - View payment history
   - Initiate payments
   - Payment status tracking

8. **Reports**
   - View available reports
   - View report data
   - Export functionality (UI ready)

9. **AI Assistant**
   - Chat interface
   - Suggested actions
   - Action execution

10. **Profile Management**
    - View profile
    - Submit update requests
    - Track request status

## Project Structure

```
ops-hub-web/
├── src/
│   ├── app/                    # Next.js pages
│   │   ├── login/             # Login page
│   │   ├── dashboard/         # Dashboard page
│   │   ├── customers/         # Customers page
│   │   ├── allocations/       # Allocations page
│   │   ├── visits/            # Field visits page
│   │   ├── payments/          # Payments page
│   │   ├── reports/           # Reports page
│   │   ├── ai-assistant/      # AI Assistant page
│   │   └── profile/           # Profile page
│   ├── components/
│   │   ├── Layout/            # Layout components
│   │   │   ├── Sidebar.js
│   │   │   ├── Header.js
│   │   │   └── AppLayout.js
│   │   ├── PermissionGuard.js
│   │   └── ProtectedRoute.js
│   ├── contexts/
│   │   └── AuthContext.js    # Authentication context
│   ├── hooks/
│   │   └── usePermissions.js
│   └── lib/
│       ├── api.js            # API client
│       └── permissions.js
└── package.json
```

## Getting Started

### Prerequisites

- Node.js 18+ 
- npm or yarn

### Installation

```bash
cd ops-hub-web
npm install
```

### Environment Setup

Create `.env.local`:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

### Run Development Server

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

## Test Credentials

Use the same credentials as backend:

- **Admin**: EMP004 / admin123
- **Lead**: EMP001 / password123
- **Agent**: EMP005 / password123

## API Integration

The frontend integrates with the backend API at `http://localhost:8080/api`. Make sure the backend is running before using the frontend.

## Key Components

### API Client (`src/lib/api.js`)

Centralized API client with:
- Automatic token management
- Error handling
- 401 redirect to login
- Methods for all endpoints

### Auth Context (`src/contexts/AuthContext.js`)

Global authentication state:
- User data
- Permissions
- Login/logout
- Permission checking utilities

### Permission Guard (`src/components/PermissionGuard.js`)

Conditional rendering based on permissions:

```jsx
<PermissionGuard permission="VIEW_CUSTOMERS">
  <CustomerList />
</PermissionGuard>
```

### App Layout (`src/components/Layout/AppLayout.js`)

Main layout wrapper with:
- Sidebar navigation
- Header
- Protected route wrapper

## Permission-Based Features

All features respect user permissions:

- **VIEW_CUSTOMERS** - View customers
- **ASSIGN_CUSTOMERS** - Allocate customers
- **VIEW_VISITS** - View visits
- **CREATE_VISITS** - Create visits
- **VIEW_PAYMENTS** - View payments
- **COLLECT_PAYMENT** - Initiate payments
- **VIEW_REPORTS** - View reports
- **EXPORT_REPORTS** - Export reports
- **USE_AI_AGENT** - Use AI assistant
- **APPROVE_PROFILE** - Approve profile updates

## Pages

### `/login`
- Employee ID and password login
- Error handling
- Redirects to dashboard on success

### `/dashboard`
- Overview statistics
- Quick actions
- Activity feed

### `/customers`
- Customer list with search
- Permission: `VIEW_CUSTOMERS`

### `/allocations`
- View and manage allocations
- Permission: `ASSIGN_CUSTOMERS`

### `/visits`
- View and create visits
- Permissions: `VIEW_VISITS`, `CREATE_VISITS`

### `/payments`
- View and initiate payments
- Permissions: `VIEW_PAYMENTS`, `COLLECT_PAYMENT`

### `/reports`
- View reports and data
- Export functionality
- Permissions: `VIEW_REPORTS`, `EXPORT_REPORTS`

### `/ai-assistant`
- Chat interface
- Suggested actions
- Permission: `USE_AI_AGENT`

### `/profile`
- View and update profile
- Submit update requests

## Styling

- **Framework**: Tailwind CSS 4
- **Design**: Minimalist, modern, engaging
- **Colors**: Slate-based palette
- **Responsive**: Mobile-first

## Development

### Build

```bash
npm run build
```

### Start Production Server

```bash
npm start
```

### Lint

```bash
npm run lint
```

## Troubleshooting

### API Connection Issues

1. Ensure backend is running on port 8080
2. Check `NEXT_PUBLIC_API_URL` in `.env.local`
3. Verify CORS is enabled in backend

### Authentication Issues

1. Clear localStorage: `localStorage.clear()`
2. Check token is being stored
3. Verify backend authentication endpoint

### Permission Issues

1. Check user has required permissions
2. Verify permissions are loaded in AuthContext
3. Check PermissionGuard is used correctly

## Next Steps

- [ ] Add detailed pages (customer detail, payment detail)
- [ ] Implement export status tracking UI
- [ ] Add profile approval workflow for admins
- [ ] Enhance AI assistant UI
- [ ] Add data visualization charts
- [ ] Implement real-time updates
- [ ] Add form validation
- [ ] Add loading skeletons
- [ ] Add error boundaries
- [ ] Add toast notifications
