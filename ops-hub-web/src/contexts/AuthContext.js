'use client';

import { createContext, useContext, useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import api from '@/lib/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [permissions, setPermissions] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [mounted, setMounted] = useState(false);
  const router = useRouter();

  useEffect(() => {
    // Mark as mounted (client-side only)
    setMounted(true);
    
    // Check if user is already logged in (only on client)
    const userData = api.getUserData();
    const token = api.getToken();
    
    if (userData && token) {
      setUser(userData);
      loadPermissions();
    } else {
      setLoading(false);
    }
  }, []);

  const loadPermissions = async () => {
    try {
      const data = await api.getMyPermissions();
      setPermissions(data.permissions || []);
      setRoles(data.roles || []);
    } catch (error) {
      console.error('Failed to load permissions:', error);
      setPermissions([]);
      setRoles([]);
    } finally {
      setLoading(false);
    }
  };

  const login = async (employeeId, password) => {
    try {
      const response = await api.login(employeeId, password);
      setUser(response);
      await loadPermissions();
      return response;
    } catch (error) {
      throw error;
    }
  };

  const logout = async () => {
    try {
      await api.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      setUser(null);
      setPermissions([]);
      setRoles([]);
      router.push('/login');
    }
  };

  const hasPermission = (permission) => {
    return permissions.includes(permission);
  };

  const hasAnyPermission = (...permissionList) => {
    return permissionList.some(perm => permissions.includes(perm));
  };

  const hasAllPermissions = (...permissionList) => {
    return permissionList.every(perm => permissions.includes(perm));
  };

  const hasRole = (role) => {
    return roles.includes(role);
  };

  const value = {
    user,
    permissions,
    roles,
    loading: loading || !mounted, // Show loading until mounted to prevent hydration mismatch
    login,
    logout,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    hasRole,
    isAuthenticated: !!user,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
