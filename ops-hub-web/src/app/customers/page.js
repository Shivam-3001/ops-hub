"use client";

import { useState, useEffect } from "react";
import AppLayout from "@/components/Layout/AppLayout";
import PermissionGuard from "@/components/PermissionGuard";
import api from "@/lib/api";
import { useAuth } from "@/contexts/AuthContext";

export default function CustomersPage() {
  const { user, hasPermission } = useAuth();
  const [customers, setCustomers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [uploadFile, setUploadFile] = useState(null);
  const [uploadPreview, setUploadPreview] = useState(null);
  const [uploadHistory, setUploadHistory] = useState([]);
  const [uploadLoading, setUploadLoading] = useState(false);
  const [uploadError, setUploadError] = useState(null);

  useEffect(() => {
    loadCustomers();
    if (hasPermission("MANAGE_CUSTOMERS")) {
      loadUploadHistory();
    }
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

  const loadUploadHistory = async () => {
    try {
      const data = await api.getCustomerUploadHistory();
      setUploadHistory(data || []);
    } catch (err) {
      console.error("Error loading upload history:", err);
    }
  };

  const handlePreviewUpload = async () => {
    if (!uploadFile) {
      setUploadError("Please select a file first.");
      return;
    }
    setUploadLoading(true);
    setUploadError(null);
    try {
      const preview = await api.previewCustomerUpload(uploadFile);
      setUploadPreview(preview);
    } catch (err) {
      setUploadError(err.message || "Failed to preview upload");
    } finally {
      setUploadLoading(false);
    }
  };

  const handleConfirmUpload = async () => {
    if (!uploadFile) {
      setUploadError("Please select a file first.");
      return;
    }
    setUploadLoading(true);
    setUploadError(null);
    try {
      await api.uploadCustomers(uploadFile);
      setUploadPreview(null);
      setUploadFile(null);
      await loadCustomers();
      await loadUploadHistory();
    } catch (err) {
      setUploadError(err.message || "Failed to upload customers");
    } finally {
      setUploadLoading(false);
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
          {hasPermission("MANAGE_CUSTOMERS") &&
            (user?.userType?.toUpperCase() === "CIRCLE_HEAD" ||
              user?.userType?.toUpperCase() === "CIRCLE_LEAD") && (
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
              <h2 className="text-lg font-semibold text-slate-900 mb-4">Upload Delinquent Customers</h2>
              <p className="text-sm text-slate-600 mb-4">
                Upload Excel/CSV to load customers before allocation. Required columns: customer_name,
                phone, email, pending_amount, cluster, circle, zone, area, store (optional).
              </p>
              <div className="flex flex-wrap items-center gap-4">
                <input
                  type="file"
                  accept=".xlsx,.csv"
                  onChange={(e) => setUploadFile(e.target.files?.[0] || null)}
                  className="text-sm"
                />
                <button
                  onClick={handlePreviewUpload}
                  className="px-4 py-2 text-sm font-medium bg-slate-900 text-white rounded-lg"
                  type="button"
                  disabled={uploadLoading}
                >
                  Preview
                </button>
                <button
                  onClick={handleConfirmUpload}
                  className="px-4 py-2 text-sm font-medium border border-slate-200 rounded-lg"
                  type="button"
                  disabled={uploadLoading || !uploadPreview}
                >
                  Upload
                </button>
              </div>
              {uploadError && (
                <div className="mt-3 text-sm text-red-600">{uploadError}</div>
              )}

              {uploadPreview && (
                <div className="mt-6">
                  <div className="text-sm text-slate-600 mb-2">
                    Rows: {uploadPreview.totalRows} | Valid: {uploadPreview.validRows} | Invalid:{" "}
                    {uploadPreview.invalidRows}
                  </div>
                  <div className="overflow-x-auto">
                    <table className="min-w-full text-sm">
                      <thead className="bg-slate-50">
                        <tr>
                          <th className="px-3 py-2 text-left">Row</th>
                          <th className="px-3 py-2 text-left">Customer</th>
                          <th className="px-3 py-2 text-left">Phone</th>
                          <th className="px-3 py-2 text-left">Pending</th>
                          <th className="px-3 py-2 text-left">Area</th>
                          <th className="px-3 py-2 text-left">Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {uploadPreview.rows?.slice(0, 10).map((row) => (
                          <tr key={row.rowNumber} className="border-t border-slate-100">
                            <td className="px-3 py-2">{row.rowNumber}</td>
                            <td className="px-3 py-2">{row.customerName}</td>
                            <td className="px-3 py-2">{row.phone}</td>
                            <td className="px-3 py-2">{row.pendingAmount}</td>
                            <td className="px-3 py-2">{row.area}</td>
                            <td className="px-3 py-2">
                              <span className={row.valid ? "text-green-600" : "text-red-600"}>
                                {row.valid ? "Valid" : "Invalid"}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          )}

          {uploadHistory.length > 0 && (
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
              <h3 className="text-lg font-semibold text-slate-900 mb-4">Upload History</h3>
              <div className="space-y-3">
                {uploadHistory.slice(0, 5).map((upload) => (
                  <div
                    key={upload.id}
                    className="flex flex-wrap items-center justify-between gap-2 border border-slate-100 rounded-lg px-4 py-3"
                  >
                    <div>
                      <div className="text-sm font-medium text-slate-900">{upload.fileName}</div>
                      <div className="text-xs text-slate-500">
                        {upload.totalRows} rows • {upload.successfulRows} success • {upload.failedRows} failed
                      </div>
                    </div>
                    <div className="text-xs text-slate-500">{upload.status}</div>
                  </div>
                ))}
              </div>
            </div>
          )}
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
                              customer.status === "PAID" || customer.status === "CLOSED"
                                ? "bg-green-100 text-green-800"
                                : customer.status === "PAYMENT_PENDING"
                                ? "bg-amber-100 text-amber-800"
                                : "bg-blue-100 text-blue-800"
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
