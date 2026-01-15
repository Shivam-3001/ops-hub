"use client";

import { useState, useEffect } from "react";
import AppLayout from "@/components/Layout/AppLayout";
import api from "@/lib/api";
import { useAuth } from "@/contexts/AuthContext";

export default function ProfilePage() {
  const { user } = useAuth();
  const [profile, setProfile] = useState(null);
  const [requests, setRequests] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showUpdateModal, setShowUpdateModal] = useState(false);
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    addressLine1: "",
    city: "",
    state: "",
    postalCode: "",
  });

  useEffect(() => {
    loadProfile();
    loadRequests();
  }, []);

  const loadProfile = async () => {
    try {
      setIsLoading(true);
      // Try to get profile from API, but use user data from auth context as fallback
      try {
        const data = await api.getMyProfile();
        setProfile(data);
        if (data) {
          setFormData({
            firstName: data.firstName || "",
            lastName: data.lastName || "",
            email: data.email || "",
            phone: data.phone || "",
            addressLine1: data.addressLine1 || "",
            city: data.city || "",
            state: data.state || "",
            postalCode: data.postalCode || "",
          });
        }
      } catch (error) {
        // If profile API fails, use user data from auth context
        console.log("Profile API not available, using auth context data");
        if (user) {
          setProfile({
            firstName: user.fullName?.split(' ')[0] || "",
            lastName: user.fullName?.split(' ').slice(1).join(' ') || "",
            email: user.email || "",
            phone: user.phone || "",
            employeeId: user.employeeId || "",
            username: user.username || "",
            userType: user.userType || "",
            role: user.role || "",
            areaName: user.areaName || "",
            zoneName: user.zoneName || "",
            circleName: user.circleName || "",
            clusterName: user.clusterName || "",
          });
          setFormData({
            firstName: user.fullName?.split(' ')[0] || "",
            lastName: user.fullName?.split(' ').slice(1).join(' ') || "",
            email: user.email || "",
            phone: user.phone || "",
            addressLine1: "",
            city: "",
            state: "",
            postalCode: "",
          });
        }
      }
    } catch (error) {
      console.error("Error loading profile:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const loadRequests = async () => {
    try {
      const data = await api.getMyProfileRequests();
      setRequests(data || []);
    } catch (error) {
      console.error("Error loading requests:", error);
      setRequests([]);
    }
  };

  const handleSubmitUpdate = async (e) => {
    e.preventDefault();
    try {
      await api.submitProfileUpdate({
        requestType: profile ? "UPDATE" : "CREATE",
        ...formData,
      });
      setShowUpdateModal(false);
      loadRequests();
      alert("Profile update request submitted successfully!");
    } catch (error) {
      alert(error.message || "Failed to submit profile update");
    }
  };

  const getRequestStatusColor = (status) => {
    switch (status) {
      case "APPROVED":
        return "bg-green-100 text-green-800";
      case "REJECTED":
        return "bg-red-100 text-red-800";
      case "PENDING":
        return "bg-yellow-100 text-yellow-800";
      default:
        return "bg-slate-100 text-slate-800";
    }
  };

  // Use user data from auth context if profile is not loaded
  const displayProfile = profile || (user ? {
    firstName: user.fullName?.split(' ')[0] || "",
    lastName: user.fullName?.split(' ').slice(1).join(' ') || "",
    email: user.email || "",
    phone: user.phone || "",
    employeeId: user.employeeId || "",
    username: user.username || "",
    userType: user.userType || "",
    role: user.role || "",
    areaName: user.areaName || "",
    zoneName: user.zoneName || "",
    circleName: user.circleName || "",
    clusterName: user.clusterName || "",
  } : null);

  return (
    <AppLayout title="My Profile" subtitle="Manage your profile information">
      <div className="space-y-6">
        {/* Profile Information */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-bold text-slate-900">Profile Information</h2>
            <button
              onClick={() => setShowUpdateModal(true)}
              className="px-4 py-2 bg-slate-900 text-white rounded-lg hover:bg-slate-800 transition-colors text-sm font-medium"
            >
              Update Profile
            </button>
          </div>

          {isLoading ? (
            <div className="text-center py-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-slate-900 mx-auto mb-2"></div>
              <p className="text-sm text-slate-600">Loading...</p>
            </div>
          ) : displayProfile ? (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="text-sm font-medium text-slate-500">Employee ID</label>
                <p className="text-slate-900 mt-1 font-medium">{displayProfile.employeeId || "N/A"}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-slate-500">Username</label>
                <p className="text-slate-900 mt-1">{displayProfile.username || "N/A"}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-slate-500">First Name</label>
                <p className="text-slate-900 mt-1">{displayProfile.firstName || "N/A"}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-slate-500">Last Name</label>
                <p className="text-slate-900 mt-1">{displayProfile.lastName || "N/A"}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-slate-500">Email</label>
                <p className="text-slate-900 mt-1">{displayProfile.email || "N/A"}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-slate-500">Phone</label>
                <p className="text-slate-900 mt-1">{displayProfile.phone || "N/A"}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-slate-500">User Type</label>
                <p className="text-slate-900 mt-1">
                  <span className="px-2 py-1 bg-blue-100 text-blue-800 rounded text-sm font-medium">
                    {displayProfile.userType || displayProfile.role || "N/A"}
                  </span>
                </p>
              </div>
              <div>
                <label className="text-sm font-medium text-slate-500">Role</label>
                <p className="text-slate-900 mt-1">
                  <span className="px-2 py-1 bg-purple-100 text-purple-800 rounded text-sm font-medium">
                    {displayProfile.role || "N/A"}
                  </span>
                </p>
              </div>
              {displayProfile.areaName && (
                <div>
                  <label className="text-sm font-medium text-slate-500">Area</label>
                  <p className="text-slate-900 mt-1">{displayProfile.areaName}</p>
                </div>
              )}
              {displayProfile.zoneName && (
                <div>
                  <label className="text-sm font-medium text-slate-500">Zone</label>
                  <p className="text-slate-900 mt-1">{displayProfile.zoneName}</p>
                </div>
              )}
              {displayProfile.circleName && (
                <div>
                  <label className="text-sm font-medium text-slate-500">Circle</label>
                  <p className="text-slate-900 mt-1">{displayProfile.circleName}</p>
                </div>
              )}
              {displayProfile.clusterName && (
                <div>
                  <label className="text-sm font-medium text-slate-500">Cluster</label>
                  <p className="text-slate-900 mt-1">{displayProfile.clusterName}</p>
                </div>
              )}
              {(displayProfile.addressLine1 || displayProfile.city || displayProfile.state || displayProfile.postalCode) && (
                <div className="md:col-span-2">
                  <label className="text-sm font-medium text-slate-500">Address</label>
                  <p className="text-slate-900 mt-1">
                    {displayProfile.addressLine1 || ""}
                    {displayProfile.city && `, ${displayProfile.city}`}
                    {displayProfile.state && `, ${displayProfile.state}`}
                    {displayProfile.postalCode && ` ${displayProfile.postalCode}`}
                  </p>
                </div>
              )}
            </div>
          ) : (
            <div className="text-center py-8">
              <p className="text-slate-600 mb-4">No profile information available</p>
              <button
                onClick={() => setShowUpdateModal(true)}
                className="px-4 py-2 bg-slate-900 text-white rounded-lg hover:bg-slate-800 transition-colors text-sm font-medium"
              >
                Create Profile
              </button>
            </div>
          )}
        </div>

        {/* Update Requests */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <h2 className="text-xl font-bold text-slate-900 mb-4">Update Requests</h2>
          {requests.length === 0 ? (
            <p className="text-slate-600 text-sm">No update requests</p>
          ) : (
            <div className="space-y-4">
              {requests.map((request) => (
                <div
                  key={request.id}
                  className="border border-slate-200 rounded-lg p-4 hover:bg-slate-50 transition-colors"
                >
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-medium text-slate-900">
                      Request #{request.id}
                    </span>
                    <span
                      className={`px-2 py-1 text-xs font-medium rounded-full ${getRequestStatusColor(
                        request.status
                      )}`}
                    >
                      {request.status}
                    </span>
                  </div>
                  <p className="text-xs text-slate-500">
                    Submitted: {request.requestedAt ? new Date(request.requestedAt).toLocaleString() : "N/A"}
                  </p>
                  {request.reviewNotes && (
                    <p className="text-sm text-slate-600 mt-2">{request.reviewNotes}</p>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Update Modal */}
        {showUpdateModal && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-xl p-6 max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
              <h3 className="text-xl font-bold text-slate-900 mb-4">Update Profile</h3>
              <form onSubmit={handleSubmitUpdate} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">
                      First Name
                    </label>
                    <input
                      type="text"
                      value={formData.firstName}
                      onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">
                      Last Name
                    </label>
                    <input
                      type="text"
                      value={formData.lastName}
                      onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                    />
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">Email</label>
                  <input
                    type="email"
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">Phone</label>
                  <input
                    type="tel"
                    value={formData.phone}
                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">Address</label>
                  <input
                    type="text"
                    value={formData.addressLine1}
                    onChange={(e) => setFormData({ ...formData, addressLine1: e.target.value })}
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                  />
                </div>
                <div className="grid grid-cols-3 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">City</label>
                    <input
                      type="text"
                      value={formData.city}
                      onChange={(e) => setFormData({ ...formData, city: e.target.value })}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">State</label>
                    <input
                      type="text"
                      value={formData.state}
                      onChange={(e) => setFormData({ ...formData, state: e.target.value })}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">
                      Postal Code
                    </label>
                    <input
                      type="text"
                      value={formData.postalCode}
                      onChange={(e) => setFormData({ ...formData, postalCode: e.target.value })}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                    />
                  </div>
                </div>
                <div className="flex gap-3 pt-4">
                  <button
                    type="button"
                    onClick={() => setShowUpdateModal(false)}
                    className="flex-1 px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="flex-1 px-4 py-2 bg-slate-900 text-white rounded-lg hover:bg-slate-800 transition-colors"
                  >
                    Submit Request
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </AppLayout>
  );
}
