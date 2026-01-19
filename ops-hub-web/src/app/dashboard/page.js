"use client";

import { useState, useEffect } from "react";
import AppLayout from "@/components/Layout/AppLayout";
import api from "@/lib/api";

export default function DashboardPage() {
  const [dashboard, setDashboard] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      const data = await api.getDashboard();
      setDashboard(data);
    } catch (error) {
      console.error("Error fetching dashboard data:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const StatCard = ({ title, value, icon, trend, color, subtitle }) => (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 hover:shadow-md transition-shadow">
      <div className="flex items-center justify-between mb-4">
        <div className={`w-12 h-12 rounded-lg ${color} flex items-center justify-center`}>
          <span className="text-2xl">{icon}</span>
        </div>
        {trend && (
          <span className={`text-sm font-medium ${trend > 0 ? "text-green-600" : "text-red-600"}`}>
            {trend > 0 ? "↑" : "↓"} {Math.abs(trend)}%
          </span>
        )}
      </div>
      <h3 className="text-2xl font-bold text-slate-900 mb-1">{value}</h3>
      <p className="text-sm text-slate-500">{title}</p>
      {subtitle && <p className="text-xs text-slate-400 mt-1">{subtitle}</p>}
    </div>
  );

  return (
    <AppLayout title="Dashboard" subtitle="Welcome back">
      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {dashboard?.cards?.map((card) => (
          <StatCard
            key={card.id}
            title={card.title}
            value={card.value}
            icon={card.icon}
            color={card.color || "bg-slate-100"}
            trend={card.trend}
            subtitle={card.subtitle}
          />
        ))}
        {!dashboard?.cards?.length && isLoading && (
          <StatCard title="Loading" value="..." icon="⏳" color="bg-slate-100" />
        )}
      </div>

      {/* Metrics and Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <h3 className="text-lg font-semibold text-slate-900 mb-4">Key Metrics</h3>
          <div className="space-y-4">
            {dashboard?.metrics?.map((metric) => (
              <div key={metric.label} className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-slate-900">{metric.label}</p>
                  <p className="text-xs text-slate-500">{metric.subtitle}</p>
                </div>
                <p className="text-lg font-semibold text-slate-900">{metric.value}</p>
              </div>
            ))}
            {!dashboard?.metrics?.length && (
              <p className="text-sm text-slate-400">No metrics available yet.</p>
            )}
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <h3 className="text-lg font-semibold text-slate-900 mb-4">Recent Activity</h3>
          <div className="space-y-4">
            {dashboard?.recentActivity?.map((activity, idx) => (
              <div key={`${activity.title}-${idx}`} className="flex items-start gap-3 pb-4 border-b border-slate-100 last:border-0 last:pb-0">
                <div className="w-10 h-10 bg-slate-100 rounded-full flex items-center justify-center flex-shrink-0">
                  <span className="text-sm">📝</span>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-slate-900">{activity.title}</p>
                  <p className="text-xs text-slate-500 mt-1">{activity.description}</p>
                </div>
              </div>
            ))}
            {!dashboard?.recentActivity?.length && (
              <p className="text-sm text-slate-400">No recent activity yet.</p>
            )}
          </div>
        </div>
      </div>

      {/* Alerts Section */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 mb-8">
        <h3 className="text-lg font-semibold text-slate-900 mb-4">Role Alerts</h3>
        <div className="space-y-3">
          {dashboard?.alerts?.length ? (
            dashboard.alerts.map((alert, idx) => (
              <div key={`${alert.title}-${idx}`} className="p-3 rounded-lg border border-amber-200 bg-amber-50">
                <div className="text-sm font-medium text-amber-900">{alert.title}</div>
                <div className="text-xs text-amber-700 mt-1">{alert.description}</div>
              </div>
            ))
          ) : (
            <p className="text-sm text-slate-400">No alerts for your role.</p>
          )}
        </div>
      </div>

      {/* Quick Actions */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
        <h3 className="text-lg font-semibold text-slate-900 mb-4">Quick Actions</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[
            { label: "Generate Report", icon: "📊", href: "/reports" },
            { label: "View Customers", icon: "👤", href: "/customers" },
            { label: "AI Assistant", icon: "🤖", href: "/ai-assistant" },
            { label: "My Profile", icon: "⚙️", href: "/profile" },
          ].map((action, idx) => (
            <a
              key={idx}
              href={action.href}
              className="p-4 border border-slate-200 rounded-lg hover:border-slate-900 hover:bg-slate-50 transition-all text-left"
            >
              <div className="text-2xl mb-2">{action.icon}</div>
              <div className="text-sm font-medium text-slate-900">{action.label}</div>
            </a>
          ))}
        </div>
      </div>
    </AppLayout>
  );
}
