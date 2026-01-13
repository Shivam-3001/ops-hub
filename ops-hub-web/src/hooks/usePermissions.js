'use client';

import { useState, useEffect } from 'react';
import { getUserPermissions, setUserPermissions, clearUserPermissions } from '@/lib/permissions';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

/**
 * React hook for managing user permissions
 * Fetches permissions from API and provides utility functions
 */
export function usePermissions() {
  const [permissions, setPermissions] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchPermissions();
  }, []);

  const fetchPermissions = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('token');
      
      if (!token) {
        setError('No authentication token found');
        setLoading(false);
        return;
      }

      const response = await fetch(`${API_BASE_URL}/permissions/me`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error('Failed to fetch permissions');
      }

      const data = await response.json();
      setPermissions(data);
      setUserPermissions(data);
      setError(null);
    } catch (err) {
      console.error('Error fetching permissions:', err);
      setError(err.message);
      // Try to use cached permissions
      const cached = getUserPermissions();
      if (cached) {
        setPermissions(cached);
      }
    } finally {
      setLoading(false);
    }
  };

  const hasPermission = (permissionCode) => {
    if (!permissions || !permissions.permissions) {
      return false;
    }
    return permissions.permissions.includes(permissionCode);
  };

  const hasAnyPermission = (permissionCodes) => {
    if (!permissions || !permissions.permissions) {
      return false;
    }
    return permissionCodes.some(code => permissions.permissions.includes(code));
  };

  const hasAllPermissions = (permissionCodes) => {
    if (!permissions || !permissions.permissions) {
      return false;
    }
    return permissionCodes.every(code => permissions.permissions.includes(code));
  };

  const hasRole = (roleCode) => {
    if (!permissions || !permissions.roles) {
      return false;
    }
    return permissions.roles.includes(roleCode);
  };

  const hasAnyRole = (roleCodes) => {
    if (!permissions || !permissions.roles) {
      return false;
    }
    return roleCodes.some(code => permissions.roles.includes(code));
  };

  const refreshPermissions = () => {
    fetchPermissions();
  };

  const clearPermissions = () => {
    setPermissions(null);
    clearUserPermissions();
  };

  return {
    permissions,
    loading,
    error,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    hasRole,
    hasAnyRole,
    refreshPermissions,
    clearPermissions,
  };
}
