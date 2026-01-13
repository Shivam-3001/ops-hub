"use client";

import { useState, useEffect } from "react";
import AppLayout from "@/components/Layout/AppLayout";
import PermissionGuard from "@/components/PermissionGuard";
import api from "@/lib/api";
import { useAuth } from "@/contexts/AuthContext";

export default function ReportsPage() {
  const { hasPermission } = useAuth();
  const [reports, setReports] = useState([]);
  const [selectedReport, setSelectedReport] = useState(null);
  const [reportData, setReportData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingData, setIsLoadingData] = useState(false);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState({});
  const [parameters, setParameters] = useState({});
  const [myExports, setMyExports] = useState([]);
  const [showExportModal, setShowExportModal] = useState(false);
  const [exportFormat, setExportFormat] = useState("CSV");
  const [exportLoading, setExportLoading] = useState(false);

  useEffect(() => {
    loadReports();
    if (hasPermission("VIEW_REPORTS")) {
      loadMyExports();
    }
  }, []);

  const loadReports = async () => {
    try {
      setIsLoading(true);
      const data = await api.getReports();
      setReports(data);
      setError(null);
    } catch (err) {
      setError(err.message || "Failed to load reports");
      console.error("Error loading reports:", err);
    } finally {
      setIsLoading(false);
    }
  };

  const loadMyExports = async () => {
    try {
      const data = await api.getMyExports();
      setMyExports(data || []);
    } catch (err) {
      console.error("Error loading exports:", err);
    }
  };

  const loadReportData = async (reportId) => {
    try {
      setIsLoadingData(true);
      const data = await api.getReportData(reportId, parameters, filters);
      setReportData(data);
      setSelectedReport(reportId);
      setError(null);
    } catch (err) {
      setError(err.message || "Failed to load report data");
      console.error("Error loading report data:", err);
    } finally {
      setIsLoadingData(false);
    }
  };

  const handleExport = async () => {
    if (!selectedReport) return;
    
    try {
      setExportLoading(true);
      const exportData = await api.requestExport({
        reportId: selectedReport,
        format: exportFormat,
        parameters,
        filters,
      });
      
      setShowExportModal(false);
      alert(`Export requested successfully! Export ID: ${exportData.id}. Status: ${exportData.status}`);
      loadMyExports();
    } catch (err) {
      alert(err.message || "Failed to request export");
    } finally {
      setExportLoading(false);
    }
  };

  const handleDownloadExport = (exportId) => {
    api.downloadExport(exportId);
  };

  const formatValue = (value) => {
    if (value === null || value === undefined) return "N/A";
    if (typeof value === "boolean") return value ? "Yes" : "No";
    if (value instanceof Date) return new Date(value).toLocaleDateString();
    return String(value);
  };

  return (
    <AppLayout title="Reports & MIS" subtitle="View and export operational reports">
      <PermissionGuard permission="VIEW_REPORTS">
        <div className="space-y-6">
          {/* Header */}
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-2xl font-bold text-slate-900">Reports & MIS</h2>
              <p className="text-slate-600 mt-1">Operational and performance reports</p>
            </div>
            {selectedReport && hasPermission("EXPORT_REPORTS") && (
              <button
                onClick={() => setShowExportModal(true)}
                className="px-4 py-2 bg-slate-900 text-white rounded-lg hover:bg-slate-800 transition-colors font-medium"
              >
                Export Report
              </button>
            )}
          </div>

          {/* Error Message */}
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
              {error}
            </div>
          )}

          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
            {/* Reports List */}
            <div className="lg:col-span-1">
              <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 sticky top-4">
                <h3 className="text-lg font-semibold text-slate-900 mb-4">Available Reports</h3>
                {isLoading ? (
                  <div className="text-center py-8">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-slate-900 mx-auto mb-2"></div>
                    <p className="text-sm text-slate-600">Loading...</p>
                  </div>
                ) : reports.length === 0 ? (
                  <p className="text-slate-600 text-sm">No reports available</p>
                ) : (
                  <div className="space-y-2">
                    {reports.map((report) => (
                      <button
                        key={report.id}
                        onClick={() => loadReportData(report.id)}
                        className={`w-full text-left px-4 py-3 rounded-lg transition-colors ${
                          selectedReport === report.id
                            ? "bg-slate-900 text-white"
                            : "bg-slate-50 hover:bg-slate-100 text-slate-900"
                        }`}
                      >
                        <div className="font-medium">{report.name}</div>
                        {report.description && (
                          <div
                            className={`text-xs mt-1 ${
                              selectedReport === report.id ? "text-slate-200" : "text-slate-500"
                            }`}
                          >
                            {report.description}
                          </div>
                        )}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* Report Data */}
            <div className="lg:col-span-3">
              {selectedReport ? (
                <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
                  {isLoadingData ? (
                    <div className="text-center py-12">
                      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-slate-900 mx-auto mb-4"></div>
                      <p className="text-slate-600">Loading report data...</p>
                    </div>
                  ) : reportData ? (
                    <div>
                      <div className="flex items-center justify-between mb-6">
                        <div>
                          <h2 className="text-xl font-bold text-slate-900">{reportData.reportName}</h2>
                          <p className="text-sm text-slate-500 mt-1">
                            {reportData.recordCount || 0} records
                          </p>
                        </div>
                      </div>

                      {reportData.data && reportData.data.length > 0 ? (
                        <div className="overflow-x-auto">
                          <table className="w-full text-sm">
                            <thead className="bg-slate-50">
                              <tr>
                                {Object.keys(reportData.data[0]).map((key) => (
                                  <th
                                    key={key}
                                    className="px-4 py-3 text-left text-xs font-medium text-slate-500 uppercase border-b border-slate-200"
                                  >
                                    {key.replace(/([A-Z])/g, " $1").trim()}
                                  </th>
                                ))}
                              </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-200">
                              {reportData.data.slice(0, 100).map((row, idx) => (
                                <tr key={idx} className="hover:bg-slate-50">
                                  {Object.values(row).map((value, cellIdx) => (
                                    <td
                                      key={cellIdx}
                                      className="px-4 py-3 text-slate-900 whitespace-nowrap"
                                    >
                                      {formatValue(value)}
                                    </td>
                                  ))}
                                </tr>
                              ))}
                            </tbody>
                          </table>
                          {reportData.data.length > 100 && (
                            <p className="text-sm text-slate-500 mt-4 text-center">
                              Showing first 100 of {reportData.data.length} records. Export to see all
                              data.
                            </p>
                          )}
                        </div>
                      ) : (
                        <div className="text-center py-12">
                          <p className="text-slate-600">No data available for this report</p>
                        </div>
                      )}
                    </div>
                  ) : null}
                </div>
              ) : (
                <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-12 text-center">
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
                      d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
                    />
                  </svg>
                  <p className="text-slate-600">Select a report to view data</p>
                </div>
              )}
            </div>
          </div>

          {/* My Exports Section */}
          {hasPermission("EXPORT_REPORTS") && myExports.length > 0 && (
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
              <h3 className="text-lg font-semibold text-slate-900 mb-4">My Exports</h3>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-4 py-2 text-left text-xs font-medium text-slate-500 uppercase">
                        Export ID
                      </th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-slate-500 uppercase">
                        Report
                      </th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-slate-500 uppercase">
                        Format
                      </th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-slate-500 uppercase">
                        Status
                      </th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-slate-500 uppercase">
                        Created
                      </th>
                      <th className="px-4 py-2 text-left text-xs font-medium text-slate-500 uppercase">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200">
                    {myExports.slice(0, 10).map((exportItem) => (
                      <tr key={exportItem.id} className="hover:bg-slate-50">
                        <td className="px-4 py-2 text-slate-900">{exportItem.id}</td>
                        <td className="px-4 py-2 text-slate-900">{exportItem.reportName || "N/A"}</td>
                        <td className="px-4 py-2 text-slate-500">{exportItem.format}</td>
                        <td className="px-4 py-2">
                          <span
                            className={`px-2 py-1 text-xs font-medium rounded-full ${
                              exportItem.status === "COMPLETED"
                                ? "bg-green-100 text-green-800"
                                : exportItem.status === "PROCESSING"
                                ? "bg-yellow-100 text-yellow-800"
                                : "bg-red-100 text-red-800"
                            }`}
                          >
                            {exportItem.status}
                          </span>
                        </td>
                        <td className="px-4 py-2 text-slate-500">
                          {exportItem.createdAt
                            ? new Date(exportItem.createdAt).toLocaleString()
                            : "N/A"}
                        </td>
                        <td className="px-4 py-2">
                          {exportItem.status === "COMPLETED" && (
                            <button
                              onClick={() => handleDownloadExport(exportItem.id)}
                              className="text-slate-900 hover:text-slate-700 font-medium text-sm"
                            >
                              Download
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Export Modal */}
          {showExportModal && (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
              <div className="bg-white rounded-xl p-6 max-w-md w-full mx-4">
                <h3 className="text-xl font-bold text-slate-900 mb-4">Export Report</h3>
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-700 mb-2">
                      Export Format
                    </label>
                    <select
                      value={exportFormat}
                      onChange={(e) => setExportFormat(e.target.value)}
                      className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent outline-none"
                    >
                      <option value="CSV">CSV</option>
                      <option value="EXCEL">Excel (XLSX)</option>
                      <option value="PDF">PDF</option>
                    </select>
                  </div>
                  <div className="flex gap-3 pt-4">
                    <button
                      type="button"
                      onClick={() => setShowExportModal(false)}
                      className="flex-1 px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
                    >
                      Cancel
                    </button>
                    <button
                      onClick={handleExport}
                      disabled={exportLoading}
                      className="flex-1 px-4 py-2 bg-slate-900 text-white rounded-lg hover:bg-slate-800 transition-colors disabled:opacity-50"
                    >
                      {exportLoading ? "Exporting..." : "Export"}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </PermissionGuard>
    </AppLayout>
  );
}
