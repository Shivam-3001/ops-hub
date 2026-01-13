"use client";

import { useState, useEffect } from "react";
import AppLayout from "@/components/Layout/AppLayout";
import PermissionGuard from "@/components/PermissionGuard";
import api from "@/lib/api";
import { useAuth } from "@/contexts/AuthContext";

export default function VisitsPage() {
  const { hasPermission } = useAuth();
  const [visits, setVisits] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showVisitModal, setShowVisitModal] = useState(false);
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [selectedVisit, setSelectedVisit] = useState(null);
  const [visitFormData, setVisitFormData] = useState({
    customerId: "",
    visitDate: new Date().toISOString().split("T")[0],
    visitType: "FIELD_VISIT",
    purpose: "",
    notes: "",
    visitStatus: "COMPLETED",
  });
  const [reviewFormData, setReviewFormData] = useState({
    visitId: "",
    rating: 5,
    reviewText: "",
    reviewCategories: [],
    isPositive: true,
  });

  useEffect(() => {
    loadVisits();
    loadCustomers();
  }, []);

  const loadVisits = async () => {
    try {
      setIsLoading(true);
      const data = await api.getMyVisits();
      setVisits(data);
      setError(null);
    } catch (err) {
      setError(err.message || "Failed to load visits");
      console.error("Error loading visits:", err);
    } finally {
      setIsLoading(false);
    }
  };

  const loadCustomers = async () => {
    try {
      const data = await api.getMyAllocations();
      setCustomers(data.map((alloc) => alloc.customer || alloc));
    } catch (err) {
      console.error("Error loading customers:", err);
    }
  };

  const handleCreateVisit = async (e) => {
    e.preventDefault();
    try {
      await api.createVisit({
        ...visitFormData,
        customerId: parseInt(visitFormData.customerId),
      });
      setShowVisitModal(false);
      setVisitFormData({
        customerId: "",
        visitDate: new Date().toISOString().split("T")[0],
        visitType: "FIELD_VISIT",
        purpose: "",
        notes: "",
        visitStatus: "COMPLETED",
      });
      loadVisits();
      alert("Visit created successfully!");
    } catch (err) {
      alert(err.message || "Failed to create visit");
    }
  };

  const handleSubmitReview = async (e) => {
    e.preventDefault();
    try {
      await api.createReview({
        ...reviewFormData,
        visitId: parseInt(reviewFormData.visitId),
      });
      setShowReviewModal(false);
      setReviewFormData({
        visitId: "",
        rating: 5,
        reviewText: "",
        reviewCategories: [],
        isPositive: true,
      });
      loadVisits();
      alert("Review submitted successfully!");
    } catch (err) {
      alert(err.message || "Failed to submit review");
    }
  };

  const openReviewModal = (visit) => {
    setSelectedVisit(visit);
    setReviewFormData({
      visitId: visit.id,
      rating: 5,
      reviewText: "",
      reviewCategories: [],
      isPositive: true,
    });
    setShowReviewModal(true);
  };

  return (
    <AppLayout title="Field Visits" subtitle="Track customer visits and reviews">
      <PermissionGuard permission="VIEW_VISITS">
        <div className="space-y-6">
          {/* Header Actions */}
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-2xl font-bold text-slate-900">My Visits</h2>
              <p className="text-slate-600 mt-1">Field visit records and customer reviews</p>
            </div>
            {hasPermission("CREATE_VISITS") && (
              <button
                onClick={() => setShowVisitModal(true)}
                className="px-4 py-2 bg-slate-900 text-white rounded-lg hover:bg-slate-800 transition-colors font-medium"
              >
                Create Visit
              </button>
            )}
          </div>

          {/* Error Message */}
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
              {error}
            </div>
          )}

          {/* Visits Table */}
          <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
            {isLoading ? (
              <div className="p-12 text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-slate-900 mx-auto mb-4"></div>
                <p className="text-slate-600">Loading visits...</p>
              </div>
            ) : visits.length === 0 ? (
              <div className="p-12 text-center">
                <p className="text-slate-600">No visits found</p>
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
                        Visit Date
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Type
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Status
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Review
                      </th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-slate-200">
                    {visits.map((visit) => (
                      <tr key={visit.id} className="hover:bg-slate-50">
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-slate-900">
                          {visit.customerName || visit.customerCode || "N/A"}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-900">
                          {visit.visitDate
                            ? new Date(visit.visitDate).toLocaleDateString()
                            : "N/A"}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                          {visit.visitType || "FIELD_VISIT"}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span
                            className={`px-2 py-1 text-xs font-medium rounded-full ${
                              visit.visitStatus === "COMPLETED"
                                ? "bg-green-100 text-green-800"
                                : visit.visitStatus === "PENDING"
                                ? "bg-yellow-100 text-yellow-800"
                                : "bg-red-100 text-red-800"
                            }`}
                          >
                            {visit.visitStatus}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          {visit.hasReview ? (
                            <span className="text-green-600 text-sm font-medium">✓ Reviewed</span>
                          ) : (
                            <button
                              onClick={() => openReviewModal(visit)}
                              className="text-slate-900 hover:text-slate-700 text-sm font-medium"
                            >
                              Add Review
                            </button>
                          )}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm">
                          <button
                            onClick={() => {
                              // View visit details
                              alert(`Visit ID: ${visit.id}\nCustomer: ${visit.customerName}\nNotes: ${visit.notes || "No notes"}`);
                            }}
                            className="text-slate-900 hover:text-slate-700 font-medium"
                          >
                            View
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Create Visit Modal */}
          {showVisitModal && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
              <div className="bg-white rounded-xl p-6 max-w-md w-full mx-4 max-h-[90vh] overflow-y-auto">
                <h3 className="text-xl font-bold text-slate-900 mb-4">Create Visit</h3>
                <form onSubmit={handleCreateVisit} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">
                      Customer
                    </label>
                    <select
                      required
                      value={visitFormData.customerId}
                      onChange={(e) => setVisitFormData({ ...visitFormData, customerId: e.target.value })}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                    >
                      <option value="">Select Customer</option>
                      {customers.map((customer) => (
                        <option key={customer.id || customer.customerId} value={customer.id || customer.customerId}>
                          {customer.customerName || customer.customerCode || `Customer ${customer.id || customer.customerId}`}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">Visit Date</label>
                    <input
                      type="date"
                      required
                      value={visitFormData.visitDate}
                      onChange={(e) => setVisitFormData({ ...visitFormData, visitDate: e.target.value })}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">Visit Type</label>
                    <select
                      value={visitFormData.visitType}
                      onChange={(e) => setVisitFormData({ ...visitFormData, visitType: e.target.value })}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                    >
                      <option value="FIELD_VISIT">Field Visit</option>
                      <option value="FOLLOW_UP">Follow Up</option>
                      <option value="COLLECTION">Collection</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">Purpose</label>
                    <input
                      type="text"
                      value={visitFormData.purpose}
                      onChange={(e) => setVisitFormData({ ...visitFormData, purpose: e.target.value })}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                      placeholder="Visit purpose"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">Notes</label>
                    <textarea
                      value={visitFormData.notes}
                      onChange={(e) => setVisitFormData({ ...visitFormData, notes: e.target.value })}
                      rows={3}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                      placeholder="Visit notes"
                    />
                  </div>
                  <div className="flex gap-3 pt-4">
                    <button
                      type="button"
                      onClick={() => setShowVisitModal(false)}
                      className="flex-1 px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="flex-1 px-4 py-2 bg-slate-900 text-white rounded-lg hover:bg-slate-800 transition-colors"
                    >
                      Create
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}

          {/* Submit Review Modal */}
          {showReviewModal && selectedVisit && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
              <div className="bg-white rounded-xl p-6 max-w-md w-full mx-4 max-h-[90vh] overflow-y-auto">
                <h3 className="text-xl font-bold text-slate-900 mb-4">Submit Review</h3>
                <p className="text-sm text-slate-600 mb-4">
                  Customer: {selectedVisit.customerName || selectedVisit.customerCode}
                </p>
                <form onSubmit={handleSubmitReview} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">Rating</label>
                    <select
                      required
                      value={reviewFormData.rating}
                      onChange={(e) => setReviewFormData({ ...reviewFormData, rating: parseInt(e.target.value) })}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                    >
                      <option value={1}>1 - Poor</option>
                      <option value={2}>2 - Fair</option>
                      <option value={3}>3 - Good</option>
                      <option value={4}>4 - Very Good</option>
                      <option value={5}>5 - Excellent</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">Review Text</label>
                    <textarea
                      required
                      value={reviewFormData.reviewText}
                      onChange={(e) => setReviewFormData({ ...reviewFormData, reviewText: e.target.value })}
                      rows={4}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                      placeholder="Write your review..."
                    />
                  </div>
                  <div>
                    <label className="flex items-center">
                      <input
                        type="checkbox"
                        checked={reviewFormData.isPositive}
                        onChange={(e) => setReviewFormData({ ...reviewFormData, isPositive: e.target.checked })}
                        className="w-4 h-4 text-slate-900 border-slate-300 rounded focus:ring-slate-900"
                      />
                      <span className="ml-2 text-sm text-slate-600">Positive Review</span>
                    </label>
                  </div>
                  <div className="flex gap-3 pt-4">
                    <button
                      type="button"
                      onClick={() => setShowReviewModal(false)}
                      className="flex-1 px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="flex-1 px-4 py-2 bg-slate-900 text-white rounded-lg hover:bg-slate-800 transition-colors"
                    >
                      Submit Review
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
