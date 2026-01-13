'use client';

import { useAuth } from '@/contexts/AuthContext';

/**
 * Permission Guard Component
 * Renders children only if user has required permissions
 * 
 * @param {Object} props
 * @param {string|string[]} props.permission - Single permission or array of permissions
 * @param {boolean} props.requireAll - If true, user must have ALL permissions; if false, ANY permission
 * @param {React.ReactNode} props.children - Content to render if permission check passes
 * @param {React.ReactNode} props.fallback - Content to render if permission check fails (optional)
 */
export default function PermissionGuard({ 
  permission, 
  requireAll = false, 
  children, 
  fallback = null 
}) {
  const { hasPermission, hasAnyPermission, hasAllPermissions, loading } = useAuth();

  if (loading) {
    return null; // Or a loading spinner
  }

  let hasAccess = false;

  if (Array.isArray(permission)) {
    hasAccess = requireAll 
      ? hasAllPermissions(...permission)
      : hasAnyPermission(...permission);
  } else {
    hasAccess = hasPermission(permission);
  }

  if (!hasAccess) {
    return fallback;
  }

  return children;
}
