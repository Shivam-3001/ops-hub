"use client";

import { useState, useEffect } from "react";
import AppLayout from "@/components/Layout/AppLayout";
import PermissionGuard from "@/components/PermissionGuard";
import api from "@/lib/api";

export default function CustomersPage() {
  const [customers, setCustomers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");

  useEffect(() => {
    loadCustomers();
  }, []);

  const loadCustomers = async () => {
    try {
      setIsLoading(true);
      const data = await api.getCustomers();
      setCustomers(data);
      setError(null);
    } catch (err) {
      setError(err.message || "Failed to load customers");
      console.error("Error loading customers:", err);
    } finally {
      setIsLoading(false);
    }
  };

  const filteredCustomers = customers.filter((customer) => {
    if (!searchTerm) return true;
    const search = searchTerm.toLowerCase();
    return (
      customer.customerCode?.toLowerCase().includes(search) ||
      customer.firstName?.toLowerCase().includes(search) ||
      customer.lastName?.toLowerCase().includes(search)
    );
  });

  return (
    <AppLayout title="Customers" subtitle="Manage customer information">
      <PermissionGuard permission="VIEW_CUSTOMERS">
        <div className="space-y-6">
          {/* Search and Filters */}
          <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
            <div className="flex items-center gap-4">
              <div className="flex-1">
                <input
                  type="text"
                  placeholder="Search customers by code, name..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                />
              </div>
            </div>
          </div>

          {/* Error Message */}
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
              {error}
            </div>
          )}

          {/* Customers Table */}
          <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-200">
              <h2 className="text-lg font-semibold text-slate-900">
                Customers ({filteredCustomers.length})
              </h2>
            </div>

            {isLoading ? (
              <div className="p-12 text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-slate-900 mx-auto mb-4"></div>
                <p className="text-slate-600">Loading customers...</p>
              </div>
            ) : filteredCustomers.length === 0 ? (
              <div className="p-12 text-center">
                <p className="text-slate-600">No customers found</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Customer Code
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Name
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Location
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Status
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-slate-200">
                    {filteredCustomers.map((customer) => (
                      <tr key={customer.id} className="hover:bg-slate-50">
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-slate-900">
                          {customer.customerCode}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-900">
                          {customer.firstName} {customer.lastName}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                          {customer.area?.name || "N/A"}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span
                            className={`px-2 py-1 text-xs font-medium rounded-full ${
                              customer.status === "ACTIVE"
                                ? "bg-green-100 text-green-800"
                                : "bg-red-100 text-red-800"
                            }`}
                          >
                            {customer.status}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm">
                          <a
                            href={`/customers/${customer.id}`}
                            className="text-slate-900 hover:text-slate-700 font-medium"
                          >
                            View
                          </a>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </PermissionGuard>
    </AppLayout>
  );
}
