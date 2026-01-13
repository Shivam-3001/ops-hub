'use client';

import { usePermissions } from '@/hooks/usePermissions';

/**
 * Role Guard Component
 * Renders children only if user has required role(s)
 * 
 * @param {Object} props
 * @param {string|string[]} props.role - Single role or array of roles
 * @param {React.ReactNode} props.children - Content to render if role check passes
 * @param {React.ReactNode} props.fallback - Content to render if role check fails (optional)
 */
export default function RoleGuard({ 
  role, 
  children, 
  fallback = null 
}) {
  const { hasRole, hasAnyRole, loading } = usePermissions();

  if (loading) {
    return null; // Or a loading spinner
  }

  let hasAccess = false;

  if (Array.isArray(role)) {
    hasAccess = hasAnyRole(role);
  } else {
    hasAccess = hasRole(role);
  }

  if (!hasAccess) {
    return fallback;
  }

  return children;
}
