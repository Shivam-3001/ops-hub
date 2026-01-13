"use client";

import { useState, useEffect } from "react";
import AppLayout from "@/components/Layout/AppLayout";
import PermissionGuard from "@/components/PermissionGuard";
import api from "@/lib/api";

export default function AllocationsPage() {
  const [allocations, setAllocations] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [users, setUsers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showAllocateModal, setShowAllocateModal] = useState(false);
  const [formData, setFormData] = useState({
    customerId: "",
    userId: "",
    roleCode: "",
    allocationType: "PRIMARY",
    notes: "",
  });

  useEffect(() => {
    loadAllocations();
    loadCustomers();
    loadUsers();
  }, []);

  const loadAllocations = async () => {
    try {
      setIsLoading(true);
      const data = await api.getMyAllocations();
      setAllocations(data);
      setError(null);
    } catch (err) {
      setError(err.message || "Failed to load allocations");
      console.error("Error loading allocations:", err);
    } finally {
      setIsLoading(false);
    }
  };

  const loadCustomers = async () => {
    try {
      const data = await api.getCustomers();
      setCustomers(data || []);
    } catch (err) {
      console.error("Error loading customers:", err);
    }
  };

  const loadUsers = async () => {
    try {
      // Note: This would require a users endpoint
      // For now, we'll use a placeholder
      setUsers([]);
    } catch (err) {
      console.error("Error loading users:", err);
    }
  };

  const handleAllocate = async (e) => {
    e.preventDefault();
    try {
      await api.allocateCustomer({
        ...formData,
        customerId: parseInt(formData.customerId),
        userId: parseInt(formData.userId),
      });
      setShowAllocateModal(false);
      setFormData({ customerId: "", userId: "", roleCode: "", allocationType: "PRIMARY", notes: "" });
      loadAllocations();
      alert("Customer allocated successfully!");
    } catch (err) {
      alert(err.message || "Failed to allocate customer");
    }
  };

  return (
    <AppLayout title="Customer Allocations" subtitle="Manage customer assignments">
      <PermissionGuard permission="ASSIGN_CUSTOMERS">
        <div className="space-y-6">
          {/* Header Actions */}
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-2xl font-bold text-slate-900">My Allocations</h2>
              <p className="text-slate-600 mt-1">Customers assigned to you</p>
            </div>
            <button
              onClick={() => setShowAllocateModal(true)}
              className="px-4 py-2 bg-slate-900 text-white rounded-lg hover:bg-slate-800 transition-colors font-medium"
            >
              Allocate Customer
            </button>
          </div>

          {/* Error Message */}
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
              {error}
            </div>
          )}

          {/* Allocations Table */}
          <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
            {isLoading ? (
              <div className="p-12 text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-slate-900 mx-auto mb-4"></div>
                <p className="text-slate-600">Loading allocations...</p>
              </div>
            ) : allocations.length === 0 ? (
              <div className="p-12 text-center">
                <p className="text-slate-600">No allocations found</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Customer
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Role
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Type
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Allocated At
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Status
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-slate-200">
                    {allocations.map((allocation) => (
                      <tr key={allocation.id} className="hover:bg-slate-50">
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-slate-900">
                          {allocation.customer?.customerCode || "N/A"}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-900">
                          {allocation.roleCode}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                          {allocation.allocationType}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                          {allocation.allocatedAt
                            ? new Date(allocation.allocatedAt).toLocaleDateString()
                            : "N/A"}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span
                            className={`px-2 py-1 text-xs font-medium rounded-full ${
                              allocation.status === "ACTIVE"
                                ? "bg-green-100 text-green-800"
                                : "bg-red-100 text-red-800"
                            }`}
                          >
                            {allocation.status}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Allocate Modal */}
          {showAllocateModal && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
              <div className="bg-white rounded-xl p-6 max-w-md w-full mx-4">
                <h3 className="text-xl font-bold text-slate-900 mb-4">Allocate Customer</h3>
                <form onSubmit={handleAllocate} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">
                      Customer
                    </label>
                    <select
                      required
                      value={formData.customerId}
                      onChange={(e) => setFormData({ ...formData, customerId: e.target.value })}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                    >
                      <option value="">Select Customer</option>
                      {customers.map((customer) => (
                        <option key={customer.id} value={customer.id}>
                          {customer.customerCode} - {customer.firstName} {customer.lastName}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">User ID</label>
                    <input
                      type="number"
                      required
                      value={formData.userId}
                      onChange={(e) => setFormData({ ...formData, userId: e.target.value })}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                      placeholder="Enter user ID"
                    />
                    <p className="text-xs text-slate-500 mt-1">
                      Note: User selection dropdown coming soon
                    </p>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">Role Code</label>
                    <input
                      type="text"
                      required
                      value={formData.roleCode}
                      onChange={(e) => setFormData({ ...formData, roleCode: e.target.value })}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                      placeholder="e.g., COLLECTOR"
                    />
                  </div>
                  <div className="flex gap-3 pt-4">
                    <button
                      type="button"
                      onClick={() => setShowAllocateModal(false)}
                      className="flex-1 px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="flex-1 px-4 py-2 bg-slate-900 text-white rounded-lg hover:bg-slate-800 transition-colors"
                    >
                      Allocate
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      </PermissionGuard>
    </AppLayout>
  );
}
