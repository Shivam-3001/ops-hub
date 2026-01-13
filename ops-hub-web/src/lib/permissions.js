/**
 * Permission utilities for frontend
 * Provides functions to check user permissions and roles
 */

/**
 * Check if user has a specific permission
 * @param {string} permissionCode - Permission code to check
 * @param {Object} userPermissions - User permissions object from API
 * @returns {boolean}
 */
export function hasPermission(permissionCode, userPermissions) {
  if (!userPermissions || !userPermissions.permissions) {
    return false;
  }
  return userPermissions.permissions.includes(permissionCode);
}

/**
 * Check if user has any of the specified permissions
 * @param {string[]} permissionCodes - Array of permission codes
 * @param {Object} userPermissions - User permissions object from API
 * @returns {boolean}
 */
export function hasAnyPermission(permissionCodes, userPermissions) {
  if (!userPermissions || !userPermissions.permissions) {
    return false;
  }
  return permissionCodes.some(code => userPermissions.permissions.includes(code));
}

/**
 * Check if user has all of the specified permissions
 * @param {string[]} permissionCodes - Array of permission codes
 * @param {Object} userPermissions - User permissions object from API
 * @returns {boolean}
 */
export function hasAllPermissions(permissionCodes, userPermissions) {
  if (!userPermissions || !userPermissions.permissions) {
    return false;
  }
  return permissionCodes.every(code => userPermissions.permissions.includes(code));
}

/**
 * Check if user has a specific role
 * @param {string} roleCode - Role code to check
 * @param {Object} userPermissions - User permissions object from API
 * @returns {boolean}
 */
export function hasRole(roleCode, userPermissions) {
  if (!userPermissions || !userPermissions.roles) {
    return false;
  }
  return userPermissions.roles.includes(roleCode);
}

/**
 * Check if user has any of the specified roles
 * @param {string[]} roleCodes - Array of role codes
 * @param {Object} userPermissions - User permissions object from API
 * @returns {boolean}
 */
export function hasAnyRole(roleCodes, userPermissions) {
  if (!userPermissions || !userPermissions.roles) {
    return false;
  }
  return roleCodes.some(code => userPermissions.roles.includes(code));
}

/**
 * Get user permissions from localStorage or session
 * @returns {Object|null}
 */
export function getUserPermissions() {
  if (typeof window === 'undefined') {
    return null;
  }
  
  const stored = localStorage.getItem('userPermissions');
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch (e) {
      console.error('Error parsing user permissions:', e);
      return null;
    }
  }
  return null;
}

/**
 * Store user permissions in localStorage
 * @param {Object} permissions - User permissions object
 */
export function setUserPermissions(permissions) {
  if (typeof window !== 'undefined') {
    localStorage.setItem('userPermissions', JSON.stringify(permissions));
  }
}

/**
 * Clear user permissions from localStorage
 */
export function clearUserPermissions() {
  if (typeof window !== 'undefined') {
    localStorage.removeItem('userPermissions');
  }
}
