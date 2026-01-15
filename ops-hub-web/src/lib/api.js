const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

/**
 * Enhanced API Client with Authentication
 * Handles all API calls with automatic token management
 */
class ApiClient {
  constructor() {
    this.baseUrl = API_BASE_URL;
  }

  /**
   * Get authentication token from localStorage
   */
  getToken() {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('auth_token');
    }
    return null;
  }

  /**
   * Set authentication token
   */
  setToken(token) {
    if (typeof window !== 'undefined') {
      localStorage.setItem('auth_token', token);
    }
  }

  /**
   * Remove authentication token
   */
  clearToken() {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('auth_token');
      localStorage.removeItem('user_data');
    }
  }

  /**
   * Get user data from localStorage
   */
  getUserData() {
    if (typeof window !== 'undefined') {
      const userData = localStorage.getItem('user_data');
      return userData ? JSON.parse(userData) : null;
    }
    return null;
  }

  /**
   * Set user data
   */
  setUserData(userData) {
    if (typeof window !== 'undefined') {
      localStorage.setItem('user_data', JSON.stringify(userData));
    }
  }

  /**
   * Make authenticated API request
   */
  async request(endpoint, options = {}) {
    const token = this.getToken();
    const headers = {
      'Content-Type': 'application/json',
      ...options.headers,
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const url = `${this.baseUrl}${endpoint}`;
    const config = {
      ...options,
      headers,
    };

    try {
      const response = await fetch(url, config);
      
      // Handle 401 Unauthorized - token expired or invalid
      // Don't redirect if we're on the login page or if this is a login request
      if (response.status === 401) {
        const isLoginPage = typeof window !== 'undefined' && window.location.pathname.includes('/login');
        const isLoginRequest = endpoint.includes('/auth/login');
        
        // For login requests, let the error be handled by the login page (don't clear token or redirect)
        if (isLoginRequest) {
          // Let the error fall through to be handled below
        } else {
          // For other requests, clear token and redirect
          this.clearToken();
          if (!isLoginPage && typeof window !== 'undefined') {
            window.location.href = '/login';
          }
          throw new Error('Session expired. Please login again.');
        }
      }

      if (!response.ok) {
        let error;
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.includes("application/json")) {
          try {
            error = await response.json();
          } catch (e) {
            error = { message: `Request failed with status ${response.status}` };
          }
        } else {
          const text = await response.text().catch(() => "");
          error = { 
            message: text || `Request failed with status ${response.status}: ${response.statusText}` 
          };
        }
        throw new Error(error.message || `Request failed: ${response.statusText}`);
      }

      return response.json();
    } catch (error) {
      console.error('API request failed:', error);
      throw error;
    }
  }

  // ==================== Authentication ====================
  
  async login(employeeId, password) {
    const response = await this.request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ employeeId, password }),
    });
    
    if (response.token) {
      this.setToken(response.token);
      this.setUserData(response);
    }
    
    return response;
  }

  async logout() {
    try {
      await this.request('/auth/logout', { method: 'POST' });
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      this.clearToken();
    }
  }

  // ==================== Permissions ====================

  async getMyPermissions() {
    return this.request('/permissions/me');
  }

  // ==================== User Management ====================

  async getManagedUsers() {
    return this.request('/user-management/users');
  }

  async createManagedUser(data) {
    return this.request('/user-management/users', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async updateManagedUserStatus(userId, active) {
    return this.request(`/user-management/users/${userId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ active }),
    });
  }

  // ==================== Dashboard ====================

  async getDashboard() {
    return this.request('/dashboard');
  }

  // ==================== Customers ====================

  async getCustomers(filters = {}) {
    const queryParams = new URLSearchParams(filters).toString();
    return this.request(`/customers${queryParams ? `?${queryParams}` : ''}`);
  }

  async getCustomer(customerId) {
    return this.request(`/customers/${customerId}`);
  }

  // ==================== Customer Allocations ====================

  async getAllocations(customerId) {
    return this.request(`/customer-allocations/customers/${customerId}`);
  }

  async getAllActiveAllocations() {
    return this.request('/customer-allocations');
  }

  async getMyAllocations() {
    return this.request('/customer-allocations/my-allocations');
  }

  async allocateCustomer(data) {
    return this.request('/customer-allocations', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async reassignCustomer(data) {
    return this.request('/customer-allocations/reassign', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async deallocateCustomer(customerId, userId, reason = '') {
    return this.request(`/customer-allocations/customers/${customerId}/users/${userId}?reason=${encodeURIComponent(reason)}`, {
      method: 'DELETE',
    });
  }

  // ==================== Field Visits ====================

  async getVisits(customerId) {
    return this.request(`/customer-visits/customers/${customerId}`);
  }

  async getMyVisits() {
    return this.request('/customer-visits/my-visits');
  }

  async createVisit(data) {
    return this.request('/customer-visits', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async createReview(data) {
    return this.request('/customer-reviews', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async getMyReviews() {
    return this.request('/customer-reviews/my-reviews');
  }

  async getCustomerReviews(customerId) {
    return this.request(`/customer-reviews/customers/${customerId}`);
  }

  async getReviewByVisitId(visitId) {
    return this.request(`/customer-reviews/visits/${visitId}`);
  }

  async getVisitById(visitId) {
    return this.request(`/customer-visits/${visitId}`);
  }

  async updateVisitStatus(visitId, status) {
    return this.request(`/customer-visits/${visitId}/status?status=${status}`, {
      method: 'PATCH',
    });
  }

  // ==================== Payments ====================

  async initiatePayment(data) {
    return this.request('/payments', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async getPaymentByReference(reference) {
    return this.request(`/payments/reference/${reference}`);
  }

  async getPaymentReceipt(reference) {
    return this.request(`/payments/${reference}/receipt`);
  }

  async getMyPayments() {
    return this.request('/payments/my-payments');
  }

  async getCustomerPayments(customerId) {
    return this.request(`/payments/customers/${customerId}`);
  }

  // ==================== Reports ====================

  async getReports() {
    return this.request('/reports');
  }

  async getReport(reportId) {
    return this.request(`/reports/${reportId}`);
  }

  async getReportData(reportId, parameters = {}, filters = {}) {
    return this.request(`/reports/${reportId}/data`, {
      method: 'POST',
      body: JSON.stringify({ parameters, filters }),
    });
  }

  // ==================== Exports ====================

  async requestExport(data) {
    return this.request('/reports/exports', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async getExport(exportId) {
    return this.request(`/reports/exports/${exportId}`);
  }

  async getMyExports() {
    return this.request('/reports/exports/my-exports');
  }

  async getReportExports(reportId) {
    return this.request(`/reports/${reportId}/exports`);
  }

  async downloadExport(exportId) {
    const token = this.getToken();
    const url = `${this.baseUrl}/reports/exports/${exportId}/download`;
    window.open(`${url}?token=${token}`, '_blank');
  }

  // ==================== AI Agent ====================

  async getAiContext(currentPage, currentModule, additionalContext = {}) {
    const params = new URLSearchParams();
    if (currentPage) params.append('currentPage', currentPage);
    if (currentModule) params.append('currentModule', currentModule);
    return this.request(`/ai/context?${params.toString()}`);
  }

  async sendAiMessage(data) {
    return this.request('/ai/message', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async executeAiAction(data) {
    return this.request('/ai/action', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async getAiConversations() {
    return this.request('/ai/conversations');
  }

  async getConversationActions(conversationId) {
    return this.request(`/ai/conversations/${conversationId}/actions`);
  }

  // ==================== Profile ====================

  async getMyProfile() {
    return this.request('/profile/my-profile');
  }

  async submitProfileUpdate(data) {
    return this.request('/profile-update-requests', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async getMyProfileRequests() {
    return this.request('/profile-update-requests/my-requests');
  }

  async getPendingProfileRequests() {
    return this.request('/profile-update-requests/pending');
  }

  async approveProfileRequest(requestId, data) {
    return this.request('/profile-update-requests/approve', {
      method: 'POST',
      body: JSON.stringify({ requestId, ...data }),
    });
  }

  async rejectProfileRequest(requestId, data) {
    return this.request('/profile-update-requests/reject', {
      method: 'POST',
      body: JSON.stringify({ requestId, ...data }),
    });
  }

  // ==================== Filters ====================

  async getClusters() {
    return this.request('/filters/clusters');
  }

  async getCircles(clusterId) {
    return this.request(`/filters/circles?clusterId=${clusterId}`);
  }

  async getZones(circleId) {
    return this.request(`/filters/zones?circleId=${circleId}`);
  }

  async getAreas(zoneId) {
    return this.request(`/filters/areas?zoneId=${zoneId}`);
  }
}

// Export singleton instance
export const api = new ApiClient();
export default api;
