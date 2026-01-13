"use client";

import { useState, useEffect } from "react";
import AppLayout from "@/components/Layout/AppLayout";
import PermissionGuard from "@/components/PermissionGuard";
import api from "@/lib/api";

export default function PaymentsPage() {
  const [payments, setPayments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showInitiateModal, setShowInitiateModal] = useState(false);
  const [formData, setFormData] = useState({
    customerId: "",
    amount: "",
    paymentMethod: "UPI",
    upiId: "",
    currency: "INR",
  });

  useEffect(() => {
    loadPayments();
  }, []);

  const loadPayments = async () => {
    try {
      setIsLoading(true);
      const data = await api.getMyPayments();
      setPayments(data);
      setError(null);
    } catch (err) {
      setError(err.message || "Failed to load payments");
      console.error("Error loading payments:", err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleInitiatePayment = async (e) => {
    e.preventDefault();
    try {
      const response = await api.initiatePayment({
        ...formData,
        customerId: parseInt(formData.customerId),
        amount: parseFloat(formData.amount),
      });
      setShowInitiateModal(false);
      setFormData({ customerId: "", amount: "", paymentMethod: "UPI", upiId: "", currency: "INR" });
      loadPayments();
      alert("Payment initiated successfully!");
    } catch (err) {
      alert(err.message || "Failed to initiate payment");
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case "SUCCESS":
        return "bg-green-100 text-green-800";
      case "FAILED":
        return "bg-red-100 text-red-800";
      case "PROCESSING":
      case "PENDING":
        return "bg-yellow-100 text-yellow-800";
      default:
        return "bg-slate-100 text-slate-800";
    }
  };

  return (
    <AppLayout title="Payments" subtitle="Manage payment transactions">
      <PermissionGuard permission="VIEW_PAYMENTS">
        <div className="space-y-6">
          {/* Header Actions */}
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-2xl font-bold text-slate-900">Payment Transactions</h2>
              <p className="text-slate-600 mt-1">View and manage payment records</p>
            </div>
            <PermissionGuard permission="COLLECT_PAYMENT">
              <button
                onClick={() => setShowInitiateModal(true)}
                className="px-4 py-2 bg-slate-900 text-white rounded-lg hover:bg-slate-800 transition-colors font-medium"
              >
                Initiate Payment
              </button>
            </PermissionGuard>
          </div>

          {/* Error Message */}
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
              {error}
            </div>
          )}

          {/* Payments Table */}
          <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
            {isLoading ? (
              <div className="p-12 text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-slate-900 mx-auto mb-4"></div>
                <p className="text-slate-600">Loading payments...</p>
              </div>
            ) : payments.length === 0 ? (
              <div className="p-12 text-center">
                <p className="text-slate-600">No payments found</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Reference
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Customer
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Amount
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Method
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Status
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Date
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-slate-200">
                    {payments.map((payment) => (
                      <tr key={payment.id} className="hover:bg-slate-50">
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-slate-900">
                          {payment.paymentReference}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-900">
                          {payment.customer?.customerCode || "N/A"}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-900">
                          {payment.amount} {payment.currency}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                          {payment.paymentMethod}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span
                            className={`px-2 py-1 text-xs font-medium rounded-full ${getStatusColor(
                              payment.paymentStatus
                            )}`}
                          >
                            {payment.paymentStatus}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                          {payment.paymentDate
                            ? new Date(payment.paymentDate).toLocaleDateString()
                            : "N/A"}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm">
                          <div className="flex items-center gap-2">
                            <button
                              onClick={async () => {
                                try {
                                  const receipt = await api.getPaymentReceipt(payment.paymentReference);
                                  alert(`Receipt:\nReference: ${receipt.paymentReference}\nAmount: ${receipt.amount} ${receipt.currency}\nStatus: ${receipt.status}`);
                                } catch (err) {
                                  alert(err.message || "Failed to load receipt");
                                }
                              }}
                              className="text-slate-900 hover:text-slate-700 font-medium"
                            >
                              View Receipt
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Initiate Payment Modal */}
          {showInitiateModal && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
              <div className="bg-white rounded-xl p-6 max-w-md w-full mx-4">
                <h3 className="text-xl font-bold text-slate-900 mb-4">Initiate Payment</h3>
                <form onSubmit={handleInitiatePayment} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">
                      Customer ID
                    </label>
                    <input
                      type="number"
                      required
                      value={formData.customerId}
                      onChange={(e) => setFormData({ ...formData, customerId: e.target.value })}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">Amount</label>
                    <input
                      type="number"
                      step="0.01"
                      required
                      value={formData.amount}
                      onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">
                      Payment Method
                    </label>
                    <select
                      value={formData.paymentMethod}
                      onChange={(e) => setFormData({ ...formData, paymentMethod: e.target.value })}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                    >
                      <option value="UPI">UPI</option>
                    </select>
                  </div>
                  {formData.paymentMethod === "UPI" && (
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-2">UPI ID</label>
                      <input
                        type="text"
                        required
                        value={formData.upiId}
                        onChange={(e) => setFormData({ ...formData, upiId: e.target.value })}
                        className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                        placeholder="user@upi"
                      />
                    </div>
                  )}
                  <div className="flex gap-3 pt-4">
                    <button
                      type="button"
                      onClick={() => setShowInitiateModal(false)}
                      className="flex-1 px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="flex-1 px-4 py-2 bg-slate-900 text-white rounded-lg hover:bg-slate-800 transition-colors"
                    >
                      Initiate
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
