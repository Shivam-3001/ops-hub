"use client";

import { useState } from "react";
import AppLayout from "@/components/Layout/AppLayout";
import PermissionGuard from "@/components/PermissionGuard";
import { useAuth } from "@/contexts/AuthContext";

export default function SettingsPage() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState("general");

  const tabs = [
    { id: "general", label: "General", icon: "⚙️" },
    { id: "notifications", label: "Notifications", icon: "🔔" },
    { id: "security", label: "Security", icon: "🔒" },
  ];

  return (
    <AppLayout title="Settings" subtitle="Manage your preferences">
      <PermissionGuard permission="MANAGE_SETTINGS">
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
                  <div className="space-y-6">
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-2">
                        Current Password
                      </label>
                      <input
                        type="password"
                        className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-2">
                        New Password
                      </label>
                      <input
                        type="password"
                        className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-2">
                        Confirm New Password
                      </label>
                      <input
                        type="password"
                        className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                      />
                    </div>
                    <div className="pt-4">
                      <button className="px-4 py-2 bg-slate-900 text-white rounded-lg hover:bg-slate-800 transition-colors">
                        Update Password
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </PermissionGuard>
    </AppLayout>
  );
}
