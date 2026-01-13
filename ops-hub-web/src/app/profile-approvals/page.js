"use client";

import { useState, useEffect } from "react";
import AppLayout from "@/components/Layout/AppLayout";
import PermissionGuard from "@/components/PermissionGuard";
import api from "@/lib/api";

export default function ProfileApprovalsPage() {
  const [requests, setRequests] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [reviewNotes, setReviewNotes] = useState("");

  useEffect(() => {
    loadPendingRequests();
  }, []);

  const loadPendingRequests = async () => {
    try {
      setIsLoading(true);
      const data = await api.getPendingProfileRequests();
      setRequests(data);
      setError(null);
    } catch (err) {
      setError(err.message || "Failed to load pending requests");
      console.error("Error loading pending requests:", err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleApprove = async (requestId) => {
    try {
      await api.approveProfileRequest(requestId, {
        reviewNotes: reviewNotes || "Approved",
      });
      setShowReviewModal(false);
      setSelectedRequest(null);
      setReviewNotes("");
      loadPendingRequests();
      alert("Profile update request approved successfully!");
    } catch (err) {
      alert(err.message || "Failed to approve request");
    }
  };

  const handleReject = async (requestId) => {
    if (!reviewNotes.trim()) {
      alert("Please provide rejection notes");
      return;
    }
    try {
      await api.rejectProfileRequest(requestId, {
        reviewNotes: reviewNotes,
      });
      setShowReviewModal(false);
      setSelectedRequest(null);
      setReviewNotes("");
      loadPendingRequests();
      alert("Profile update request rejected");
    } catch (err) {
      alert(err.message || "Failed to reject request");
    }
  };

  const openReviewModal = (request, action) => {
    setSelectedRequest({ ...request, action });
    setReviewNotes("");
    setShowReviewModal(true);
  };

  return (
    <AppLayout title="Profile Approvals" subtitle="Review and approve profile update requests">
      <PermissionGuard permission="APPROVE_PROFILE">
        <div className="space-y-6">
          {/* Header */}
          <div>
            <h2 className="text-2xl font-bold text-slate-900">Pending Profile Update Requests</h2>
            <p className="text-slate-600 mt-1">
              Review and approve or reject profile update requests
            </p>
          </div>

          {/* Error Message */}
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
              {error}
            </div>
          )}

          {/* Requests List */}
          <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
            {isLoading ? (
              <div className="p-12 text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-slate-900 mx-auto mb-4"></div>
                <p className="text-slate-600">Loading requests...</p>
              </div>
            ) : requests.length === 0 ? (
              <div className="p-12 text-center">
                <svg
                  className="w-16 h-16 mx-auto mb-4 text-slate-400"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                  />
                </svg>
                <p className="text-slate-600">No pending requests</p>
              </div>
            ) : (
              <div className="divide-y divide-slate-200">
                {requests.map((request) => (
                  <div key={request.id} className="p-6 hover:bg-slate-50 transition-colors">
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-3 mb-3">
                          <div className="w-12 h-12 bg-slate-100 rounded-full flex items-center justify-center">
                            <span className="text-slate-600 font-medium">
                              {request.userName?.charAt(0) || "U"}
                            </span>
                          </div>
                          <div>
                            <h3 className="font-semibold text-slate-900">
                              {request.userName || `User ${request.userEmployeeId}`}
                            </h3>
                            <p className="text-sm text-slate-500">
                              Employee ID: {request.userEmployeeId}
                            </p>
                          </div>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                          <div>
                            <label className="text-xs font-medium text-slate-500 uppercase">
                              Request Type
                            </label>
                            <p className="text-slate-900 mt-1">{request.requestType}</p>
                          </div>
                          <div>
                            <label className="text-xs font-medium text-slate-500 uppercase">
                              Field
                            </label>
                            <p className="text-slate-900 mt-1">{request.fieldName || "N/A"}</p>
                          </div>
                          <div>
                            <label className="text-xs font-medium text-slate-500 uppercase">
                              Old Value
                            </label>
                            <p className="text-slate-600 mt-1 text-sm">
                              {request.oldValue || "N/A"}
                            </p>
                          </div>
                          <div>
                            <label className="text-xs font-medium text-slate-500 uppercase">
                              New Value
                            </label>
                            <p className="text-slate-900 mt-1 font-medium">
                              {request.newValue || "N/A"}
                            </p>
                          </div>
                        </div>

                        {request.reason && (
                          <div className="mb-4">
                            <label className="text-xs font-medium text-slate-500 uppercase">
                              Reason
                            </label>
                            <p className="text-slate-600 mt-1 text-sm">{request.reason}</p>
                          </div>
                        )}

                        <div className="text-xs text-slate-500">
                          Requested: {new Date(request.requestedAt).toLocaleString()}
                        </div>
                      </div>

                      <div className="flex flex-col gap-2 ml-4">
                        <button
                          onClick={() => openReviewModal(request, "approve")}
                          className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors text-sm font-medium whitespace-nowrap"
                        >
                          Approve
                        </button>
                        <button
                          onClick={() => openReviewModal(request, "reject")}
                          className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors text-sm font-medium whitespace-nowrap"
                        >
                          Reject
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Review Modal */}
          {showReviewModal && selectedRequest && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
              <div className="bg-white rounded-xl p-6 max-w-md w-full mx-4">
                <h3 className="text-xl font-bold text-slate-900 mb-4">
                  {selectedRequest.action === "approve" ? "Approve" : "Reject"} Request
                </h3>
                <div className="mb-4">
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                    Review Notes
                    {selectedRequest.action === "reject" && (
                      <span className="text-red-600"> *</span>
                    )}
                  </label>
                  <textarea
                    value={reviewNotes}
                    onChange={(e) => setReviewNotes(e.target.value)}
                    rows={4}
                    required={selectedRequest.action === "reject"}
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                    placeholder={
                      selectedRequest.action === "approve"
                        ? "Optional notes for approval..."
                        : "Please provide reason for rejection..."
                    }
                  />
                </div>
                <div className="flex gap-3 pt-4">
                  <button
                    type="button"
                    onClick={() => {
                      setShowReviewModal(false);
                      setSelectedRequest(null);
                      setReviewNotes("");
                    }}
                    className="flex-1 px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={() => {
                      if (selectedRequest.action === "approve") {
                        handleApprove(selectedRequest.id);
                      } else {
                        handleReject(selectedRequest.id);
                      }
                    }}
                    className={`flex-1 px-4 py-2 rounded-lg transition-colors text-white font-medium ${
                      selectedRequest.action === "approve"
                        ? "bg-green-600 hover:bg-green-700"
                        : "bg-red-600 hover:bg-red-700"
                    }`}
                  >
                    {selectedRequest.action === "approve" ? "Approve" : "Reject"}
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </PermissionGuard>
    </AppLayout>
  );
}
