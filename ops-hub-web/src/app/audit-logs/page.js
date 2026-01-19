"use client";

import { useEffect, useState } from "react";
import AppLayout from "@/components/Layout/AppLayout";
import PermissionGuard from "@/components/PermissionGuard";
import api from "@/lib/api";

export default function AuditLogsPage() {
  const [logs, setLogs] = useState([]);
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState({
    actionType: "",
    entityType: "",
  });
  const [loading, setLoading] = useState(true);

  const fetchLogs = async (pageNumber = 0, filterValues = filters) => {
    setLoading(true);
    try {
      const params = {
        page: pageNumber,
        size: 20,
        ...(filterValues.actionType ? { actionType: filterValues.actionType } : {}),
        ...(filterValues.entityType ? { entityType: filterValues.entityType } : {}),
      };
      const data = await api.getAuditLogs(params);
      setLogs(data?.content || []);
      setPage(data?.number || 0);
    } catch (error) {
      console.error("Failed to load audit logs", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, []);

  const handleFilterChange = (event) => {
    const { name, value } = event.target;
    setFilters((prev) => ({ ...prev, [name]: value }));
  };

  const handleApplyFilters = () => {
    fetchLogs(0, filters);
  };

  return (
    <PermissionGuard permission="VIEW_AUDIT_LOGS">
      <AppLayout title="Audit Trail" subtitle="Track who changed what and when">
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 mb-6">
          <div className="flex flex-wrap gap-4 items-end">
            <div>
              <label className="block text-xs text-slate-500 mb-1">Action Type</label>
              <input
                type="text"
                name="actionType"
                value={filters.actionType}
                onChange={handleFilterChange}
                className="border border-slate-200 rounded-lg px-3 py-2 text-sm"
                placeholder="CREATE, UPDATE..."
              />
            </div>
            <div>
              <label className="block text-xs text-slate-500 mb-1">Entity Type</label>
              <input
                type="text"
                name="entityType"
                value={filters.entityType}
                onChange={handleFilterChange}
                className="border border-slate-200 rounded-lg px-3 py-2 text-sm"
                placeholder="CUSTOMER, PAYMENT..."
              />
            </div>
            <button
              onClick={handleApplyFilters}
              className="px-4 py-2 text-sm font-medium bg-slate-900 text-white rounded-lg"
              type="button"
            >
              Apply Filters
            </button>
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <h3 className="text-lg font-semibold text-slate-900 mb-4">Audit Logs</h3>
          {loading ? (
            <p className="text-sm text-slate-500">Loading audit logs...</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead className="text-slate-500 text-xs uppercase">
                  <tr>
                    <th className="text-left py-2">User</th>
                    <th className="text-left py-2">Action</th>
                    <th className="text-left py-2">Entity</th>
                    <th className="text-left py-2">Status</th>
                    <th className="text-left py-2">When</th>
                  </tr>
                </thead>
                <tbody>
                  {logs.map((log) => (
                    <tr key={log.id} className="border-t border-slate-100">
                      <td className="py-2">
                        <div className="text-slate-900">{log.userName || "System"}</div>
                        <div className="text-xs text-slate-500">{log.employeeId || "-"}</div>
                      </td>
                      <td className="py-2">{log.actionType}</td>
                      <td className="py-2">
                        <div>{log.entityType}</div>
                        <div className="text-xs text-slate-500">ID: {log.entityId || "-"}</div>
                      </td>
                      <td className="py-2">{log.status}</td>
                      <td className="py-2">{log.createdAt ? new Date(log.createdAt).toLocaleString() : "-"}</td>
                    </tr>
                  ))}
                  {logs.length === 0 && (
                    <tr>
                      <td className="py-4 text-sm text-slate-500" colSpan={5}>
                        No audit logs found.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </AppLayout>
    </PermissionGuard>
  );
}
