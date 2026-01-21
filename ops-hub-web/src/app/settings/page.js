"use client";

import { useEffect, useMemo, useState } from "react";
import AppLayout from "@/components/Layout/AppLayout";
import { useAuth } from "@/contexts/AuthContext";
import api from "@/lib/api";

export default function SettingsPage() {
  const { user, hasPermission } = useAuth();
  const [activeTab, setActiveTab] = useState("general");
  const [managedUsers, setManagedUsers] = useState([]);
  const [usersLoading, setUsersLoading] = useState(false);
  const [usersError, setUsersError] = useState(null);
  const [showCreateUserModal, setShowCreateUserModal] = useState(false);
  const [createUserLoading, setCreateUserLoading] = useState(false);
  const [createUserError, setCreateUserError] = useState(null);
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [passwordError, setPasswordError] = useState(null);
  const [passwordSuccess, setPasswordSuccess] = useState(null);
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [createUserForm, setCreateUserForm] = useState({
    fullName: "",
    username: "",
    email: "",
    phone: "",
    password: "",
    role: "",
    active: true,
  });

  const tabs = useMemo(() => {
    const baseTabs = [
      { id: "general", label: "General", icon: "⚙️" },
      { id: "notifications", label: "Notifications", icon: "🔔" },
      { id: "security", label: "Security", icon: "🔒" },
    ];

    if (hasPermission("MANAGE_USERS")) {
      baseTabs.push({ id: "user-management", label: "User Management", icon: "👥" });
    }

    return baseTabs;
  }, [hasPermission]);

  useEffect(() => {
    if (activeTab === "user-management") {
      loadManagedUsers();
    }
  }, [activeTab]);

  const loadManagedUsers = async () => {
    try {
      setUsersLoading(true);
      setUsersError(null);
      const data = await api.getManagedUsers();
      setManagedUsers(data || []);
    } catch (error) {
      setUsersError(error.message || "Failed to load users");
    } finally {
      setUsersLoading(false);
    }
  };

  const handleCreateUser = async (event) => {
    event.preventDefault();
    setCreateUserLoading(true);
    setCreateUserError(null);

    try {
      const payload = {
        fullName: createUserForm.fullName.trim(),
        username: createUserForm.username.trim(),
        email: createUserForm.email.trim(),
        phone: createUserForm.phone.trim(),
        password: createUserForm.password,
        role: createUserForm.role,
        active: createUserForm.active,
      };
      await api.createManagedUser(payload);
      setShowCreateUserModal(false);
      setCreateUserForm({
        fullName: "",
        username: "",
        email: "",
        phone: "",
        password: "",
        role: "",
        active: true,
      });
      await loadManagedUsers();
    } catch (error) {
      setCreateUserError(error.message || "Failed to create user");
    } finally {
      setCreateUserLoading(false);
    }
  };

  const handleToggleUserStatus = async (targetUser) => {
    if (!targetUser) return;
    try {
      setUsersError(null);
      await api.updateManagedUserStatus(targetUser.id, !targetUser.active);
      await loadManagedUsers();
    } catch (error) {
      setUsersError(error.message || "Failed to update user status");
    }
  };

  const roleOptions = useMemo(() => {
    const currentType = (user?.userType || "").toUpperCase();
    const options = [];

    if (currentType === "ADMIN") {
      options.push(
        { value: "CLUSTER_HEAD", label: "Cluster Head" },
        { value: "CIRCLE_HEAD", label: "Circle Head" },
        { value: "ZONE_HEAD", label: "Zone Head" },
        { value: "AREA_HEAD", label: "Area Head" },
        { value: "STORE_HEAD", label: "Store Head" },
        { value: "AGENT", label: "Agent" }
      );
      return options;
    }

    if (currentType === "CLUSTER_HEAD") {
      options.push(
        { value: "CIRCLE_HEAD", label: "Circle Head" },
        { value: "ZONE_HEAD", label: "Zone Head" },
        { value: "AREA_HEAD", label: "Area Head" },
        { value: "STORE_HEAD", label: "Store Head" },
        { value: "AGENT", label: "Agent" }
      );
      return options;
    }

    return options;
  }, [user?.userType]);

  const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/;

  const handleChangePassword = async (event) => {
    event.preventDefault();
    setPasswordError(null);
    setPasswordSuccess(null);

    if (!passwordForm.currentPassword) {
      setPasswordError("Please enter your current password.");
      return;
    }

    if (!passwordRegex.test(passwordForm.newPassword)) {
      setPasswordError(
        "Password must be at least 8 characters and include uppercase, lowercase, number, and special character."
      );
      return;
    }

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordError("New password and confirm password do not match.");
      return;
    }

    try {
      setPasswordLoading(true);
      await api.changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
        confirmPassword: passwordForm.confirmPassword,
      });
      setPasswordSuccess("Password updated successfully.");
      setPasswordForm({
        currentPassword: "",
        newPassword: "",
        confirmPassword: "",
      });
    } catch (error) {
      setPasswordError(error.message || "Failed to update password.");
    } finally {
      setPasswordLoading(false);
    }
  };

  return (
    <AppLayout title="Settings" subtitle="Manage your preferences">
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Settings Sidebar */}
        <div className="lg:col-span-1">
          <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
            <nav className="space-y-2">
              {tabs.map((tab) => (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`w-full text-left px-4 py-3 rounded-lg transition-colors ${
                    activeTab === tab.id
                      ? "bg-slate-900 text-white"
                      : "text-slate-600 hover:bg-slate-50"
                  }`}
                >
                  <span className="mr-2">{tab.icon}</span>
                  {tab.label}
                </button>
              ))}
            </nav>
          </div>
        </div>

          {/* Settings Content */}
          <div className="lg:col-span-3">
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
              {activeTab === "general" && (
                <div>
                  <h2 className="text-xl font-bold text-slate-900 mb-6">General Settings</h2>
                  <div className="space-y-6">
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-2">
                        Language
                      </label>
                      <select className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none">
                        <option>English</option>
                        <option>Hindi</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-2">
                        Timezone
                      </label>
                      <select className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none">
                        <option>Asia/Kolkata (IST)</option>
                        <option>UTC</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-2">
                        Date Format
                      </label>
                      <select className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none">
                        <option>DD/MM/YYYY</option>
                        <option>MM/DD/YYYY</option>
                        <option>YYYY-MM-DD</option>
                      </select>
                    </div>
                    <div className="pt-4">
                      <button className="px-4 py-2 bg-slate-900 text-white rounded-lg hover:bg-slate-800 transition-colors">
                        Save Changes
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {activeTab === "notifications" && (
                <div>
                  <h2 className="text-xl font-bold text-slate-900 mb-6">Notification Settings</h2>
                  <div className="space-y-4">
                    {[
                      { label: "Email Notifications", description: "Receive notifications via email" },
                      { label: "Profile Update Alerts", description: "Get notified when profile updates are reviewed" },
                      { label: "Payment Alerts", description: "Receive alerts for payment status changes" },
                      { label: "Report Generation", description: "Get notified when reports are ready" },
                    ].map((item, idx) => (
                      <div key={idx} className="flex items-center justify-between p-4 border border-slate-200 rounded-lg">
                        <div>
                          <div className="font-medium text-slate-900">{item.label}</div>
                          <div className="text-sm text-slate-500">{item.description}</div>
                        </div>
                        <label className="relative inline-flex items-center cursor-pointer">
                          <input type="checkbox" className="sr-only peer" defaultChecked />
                          <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-slate-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-slate-900"></div>
                        </label>
                      </div>
                    ))}
                    <div className="pt-4">
                      <button className="px-4 py-2 bg-slate-900 text-white rounded-lg hover:bg-slate-800 transition-colors">
                        Save Changes
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {activeTab === "security" && (
                <div>
                  <h2 className="text-xl font-bold text-slate-900 mb-6">Security Settings</h2>
                  <form className="space-y-6" onSubmit={handleChangePassword}>
                    {passwordError && (
                      <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
                        {passwordError}
                      </div>
                    )}
                    {passwordSuccess && (
                      <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg">
                        {passwordSuccess}
                      </div>
                    )}
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-2">
                        Current Password
                      </label>
                      <input
                        type="password"
                        value={passwordForm.currentPassword}
                        onChange={(e) =>
                          setPasswordForm({ ...passwordForm, currentPassword: e.target.value })
                        }
                        className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-2">
                        New Password
                      </label>
                      <input
                        type="password"
                        value={passwordForm.newPassword}
                        onChange={(e) =>
                          setPasswordForm({ ...passwordForm, newPassword: e.target.value })
                        }
                        className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                      />
                      <p className="text-xs text-slate-500 mt-2">
                        Minimum 8 characters, 1 lowercase, 1 uppercase, 1 number, 1 special
                        character.
                      </p>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-2">
                        Confirm New Password
                      </label>
                      <input
                        type="password"
                        value={passwordForm.confirmPassword}
                        onChange={(e) =>
                          setPasswordForm({ ...passwordForm, confirmPassword: e.target.value })
                        }
                        className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                      />
                    </div>
                    <div className="pt-4">
                      <button
                        type="submit"
                        disabled={passwordLoading}
                        className="px-4 py-2 bg-slate-900 text-white rounded-lg hover:bg-slate-800 transition-colors disabled:opacity-70"
                      >
                        {passwordLoading ? "Updating..." : "Update Password"}
                      </button>
                    </div>
                  </form>
                </div>
              )}

              {activeTab === "user-management" && hasPermission("MANAGE_USERS") && (
                <div className="space-y-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <h2 className="text-xl font-bold text-slate-900">User Management</h2>
                      <p className="text-sm text-slate-500">
                        Create users and manage account status
                      </p>
                    </div>
                    <button
                      onClick={() => setShowCreateUserModal(true)}
                      className="px-4 py-2 bg-slate-900 text-white rounded-lg hover:bg-slate-800 transition-colors text-sm font-medium"
                    >
                      Create User
                    </button>
                  </div>

                  {usersError && (
                    <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
                      {usersError}
                    </div>
                  )}

                  <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
                    <div className="px-6 py-4 border-b border-slate-200">
                      <h3 className="text-lg font-semibold text-slate-900">
                        Managed Users ({managedUsers.length})
                      </h3>
                    </div>

                    {usersLoading ? (
                      <div className="p-8 text-center">
                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-slate-900 mx-auto mb-2"></div>
                        <p className="text-sm text-slate-600">Loading users...</p>
                      </div>
                    ) : managedUsers.length === 0 ? (
                      <div className="p-8 text-center">
                        <p className="text-sm text-slate-600">No users available</p>
                      </div>
                    ) : (
                      <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-slate-200">
                          <thead className="bg-slate-50">
                            <tr>
                              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                                Name
                              </th>
                              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                                Username
                              </th>
                              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                                Email
                              </th>
                              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                                Role
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
                            {managedUsers.map((managedUser) => (
                              <tr key={managedUser.id} className="hover:bg-slate-50">
                                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-slate-900">
                                  {managedUser.fullName || managedUser.username}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-600">
                                  {managedUser.username}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-600">
                                  {managedUser.email}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-600">
                                  {managedUser.role}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm">
                                  <span
                                    className={`px-2 py-1 text-xs font-medium rounded-full ${
                                      managedUser.active
                                        ? "bg-green-100 text-green-800"
                                        : "bg-slate-100 text-slate-600"
                                    }`}
                                  >
                                    {managedUser.active ? "Active" : "Inactive"}
                                  </span>
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm">
                                  <button
                                    onClick={() => handleToggleUserStatus(managedUser)}
                                    className="text-slate-900 hover:text-slate-700 font-medium"
                                  >
                                    {managedUser.active ? "Deactivate" : "Activate"}
                                  </button>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>
      </div>

      {showCreateUserModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
            <h3 className="text-xl font-bold text-slate-900 mb-4">Create User</h3>
            <form onSubmit={handleCreateUser} className="space-y-4">
              {createUserError && (
                <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-2 rounded-lg text-sm">
                  {createUserError}
                </div>
              )}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                    Name
                  </label>
                  <input
                    type="text"
                    required
                    value={createUserForm.fullName}
                    onChange={(e) => setCreateUserForm({ ...createUserForm, fullName: e.target.value })}
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                    Username
                  </label>
                  <input
                    type="text"
                    required
                    value={createUserForm.username}
                    onChange={(e) => setCreateUserForm({ ...createUserForm, username: e.target.value })}
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                    Email
                  </label>
                  <input
                    type="email"
                    required
                    value={createUserForm.email}
                    onChange={(e) => setCreateUserForm({ ...createUserForm, email: e.target.value })}
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                    Phone
                  </label>
                  <input
                    type="tel"
                    required
                    value={createUserForm.phone}
                    onChange={(e) => setCreateUserForm({ ...createUserForm, phone: e.target.value })}
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                    Password
                  </label>
                  <input
                    type="password"
                    required
                    value={createUserForm.password}
                    onChange={(e) => setCreateUserForm({ ...createUserForm, password: e.target.value })}
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-2">
                    Role
                  </label>
                  <select
                    required
                    value={createUserForm.role}
                    onChange={(e) => setCreateUserForm({ ...createUserForm, role: e.target.value })}
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                  >
                    <option value="" disabled>
                      Select role
                    </option>
                    {roleOptions.map((role) => (
                      <option key={role.value} value={role.value}>
                        {role.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="flex items-center gap-3 pt-6">
                  <input
                    id="create-user-active"
                    type="checkbox"
                    checked={createUserForm.active}
                    onChange={(e) => setCreateUserForm({ ...createUserForm, active: e.target.checked })}
                    className="h-4 w-4 text-slate-900 border-slate-300 rounded"
                  />
                  <label htmlFor="create-user-active" className="text-sm text-slate-700">
                    Active
                  </label>
                </div>
              </div>

              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setShowCreateUserModal(false)}
                  className="flex-1 px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createUserLoading}
                  className="flex-1 px-4 py-2 bg-slate-900 text-white rounded-lg hover:bg-slate-800 transition-colors disabled:opacity-70"
                >
                  {createUserLoading ? "Creating..." : "Create User"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppLayout>
  );
}
