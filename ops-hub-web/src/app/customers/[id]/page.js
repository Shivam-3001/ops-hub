"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import AppLayout from "@/components/Layout/AppLayout";
import PermissionGuard from "@/components/PermissionGuard";
import api from "@/lib/api";

export default function CustomerDetailsPage() {
  const params = useParams();
  const router = useRouter();
  const customerId = params?.id;
  const [customer, setCustomer] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!customerId) return;
    const loadCustomer = async () => {
      try {
        setIsLoading(true);
        const data = await api.getCustomer(customerId);
        setCustomer(data);
        setError(null);
      } catch (err) {
        setError(err.message || "Failed to load customer");
      } finally {
        setIsLoading(false);
      }
    };
    loadCustomer();
  }, [customerId]);

  return (
    <AppLayout title="Customer Details" subtitle={`Customer ID: ${customerId || ""}`}>
      <PermissionGuard permission="VIEW_CUSTOMERS">
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-2xl font-bold text-slate-900">
                {customer?.customerCode || "Customer"}
              </h2>
              <p className="text-slate-600 mt-1">
                {customer?.firstName || ""} {customer?.lastName || ""}
              </p>
            </div>
            <button
              onClick={() => router.push("/customers")}
              className="px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
            >
              Back to Customers
            </button>
          </div>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
              {error}
            </div>
          )}

          {isLoading ? (
            <div className="p-12 text-center bg-white rounded-xl shadow-sm border border-slate-200">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-slate-900 mx-auto mb-4"></div>
              <p className="text-slate-600">Loading customer...</p>
            </div>
          ) : customer ? (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 space-y-4">
                <h3 className="text-lg font-semibold text-slate-900">Profile</h3>
                <div className="text-sm text-slate-600">
                  <div className="flex justify-between py-2 border-b border-slate-100">
                    <span className="font-medium text-slate-700">Customer Code</span>
                    <span>{customer.customerCode || "N/A"}</span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-slate-100">
                    <span className="font-medium text-slate-700">Status</span>
                    <span>{customer.status || "N/A"}</span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-slate-100">
                    <span className="font-medium text-slate-700">Pending Amount</span>
                    <span>{customer.pendingAmount ?? "N/A"}</span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-slate-100">
                    <span className="font-medium text-slate-700">Store</span>
                    <span>{customer.storeName || "N/A"}</span>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 space-y-4">
                <h3 className="text-lg font-semibold text-slate-900">Location</h3>
                <div className="text-sm text-slate-600">
                  <div className="flex justify-between py-2 border-b border-slate-100">
                    <span className="font-medium text-slate-700">Area</span>
                    <span>{customer.area?.name || "N/A"}</span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-slate-100">
                    <span className="font-medium text-slate-700">Zone</span>
                    <span>{customer.area?.zone?.name || "N/A"}</span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-slate-100">
                    <span className="font-medium text-slate-700">Circle</span>
                    <span>{customer.area?.zone?.circle?.name || "N/A"}</span>
                  </div>
                  <div className="flex justify-between py-2 border-b border-slate-100">
                    <span className="font-medium text-slate-700">Cluster</span>
                    <span>{customer.area?.zone?.circle?.cluster?.name || "N/A"}</span>
                  </div>
                  <div className="flex justify-between py-2">
                    <span className="font-medium text-slate-700">Address</span>
                    <span>
                      {[customer.addressLine1, customer.addressLine2, customer.city, customer.state]
                        .filter(Boolean)
                        .join(", ") || "N/A"}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          ) : null}
        </div>
      </PermissionGuard>
    </AppLayout>
  );
}
