"use client";

import { useState, useEffect } from "react";
import AppLayout from "@/components/Layout/AppLayout";
import PermissionGuard from "@/components/PermissionGuard";
import api from "@/lib/api";
import { useAuth } from "@/contexts/AuthContext";

export default function ReportsPage() {
  const { user, hasPermission } = useAuth();
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
  const [clusters, setClusters] = useState([]);
  const [circles, setCircles] = useState([]);
  const [zones, setZones] = useState([]);
  const [areas, setAreas] = useState([]);
  const [hierarchyFilters, setHierarchyFilters] = useState({
    clusterId: "",
    circleId: "",
    zoneId: "",
    areaId: "",
  });
  const normalizedRole = user?.userType?.toUpperCase?.() || "";
  const canShowCluster = normalizedRole === "ADMIN";
  const canShowCircle = ["ADMIN", "CLUSTER_HEAD"].includes(normalizedRole);
  const canShowZone = ["ADMIN", "CLUSTER_HEAD", "CIRCLE_HEAD", "CIRCLE_LEAD"].includes(normalizedRole);
  const canShowArea = [
    "ADMIN",
    "CLUSTER_HEAD",
    "CIRCLE_HEAD",
    "CIRCLE_LEAD",
    "ZONE_HEAD",
    "AREA_HEAD",
    "STORE_HEAD",
  ].includes(normalizedRole);
  const reportFilters = selectedReport?.parameters?.filters || [];

  useEffect(() => {
    loadReports();
    if (hasPermission("VIEW_REPORTS")) {
      loadMyExports();
    }
  }, []);

  useEffect(() => {
    loadClusters();
  }, []);

  useEffect(() => {
    if (hierarchyFilters.clusterId) {
      loadCircles(hierarchyFilters.clusterId);
    } else {
      setCircles([]);
      setZones([]);
      setAreas([]);
    }
  }, [hierarchyFilters.clusterId]);

  useEffect(() => {
    if (hierarchyFilters.circleId) {
      loadZones(hierarchyFilters.circleId);
    } else {
      setZones([]);
      setAreas([]);
    }
  }, [hierarchyFilters.circleId]);

  useEffect(() => {
    if (hierarchyFilters.zoneId) {
      loadAreas(hierarchyFilters.zoneId);
    } else {
      setAreas([]);
    }
  }, [hierarchyFilters.zoneId]);

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

  const normalizeIdValue = (value) => {
    if (value === "" || value === null || value === undefined) return null;
    const numeric = Number(value);
    return Number.isNaN(numeric) ? value : numeric;
  };

  const normalizeFilters = (rawFilters, report) => {
    if (!rawFilters) return {};
    const filterDefs = report?.parameters?.filters || [];
    const normalized = {};
    Object.entries(rawFilters).forEach(([key, value]) => {
      if (value === "" || value === null || value === undefined) {
        return;
      }
      const def = filterDefs.find((item) => item.key === key);
      if (key.endsWith("Id")) {
        normalized[key] = normalizeIdValue(value);
      } else if (def?.type === "date") {
        if (key.toLowerCase().includes("to")) {
          normalized[key] = `${value}T23:59:59`;
        } else {
          normalized[key] = `${value}T00:00:00`;
        }
      } else {
        normalized[key] = value;
      }
    });
    return normalized;
  };

  const loadReportData = async (reportId, reportMeta = selectedReport) => {
    try {
      setIsLoadingData(true);
      const normalizedFilters = normalizeFilters(
        { ...filters, ...hierarchyFilters },
        reportMeta
      );
      const data = await api.getReportData(reportId, parameters, normalizedFilters);
      setReportData(data);
      setError(null);
    } catch (err) {
      setError(err.message || "Failed to load report data");
      console.error("Error loading report data:", err);
    } finally {
      setIsLoadingData(false);
    }
  };

  const handleSelectReport = (report) => {
    setSelectedReport(report);
    setReportData(null);
    setFilters({});
    setParameters({});
    loadReportData(report.id, report);
  };

  const handleFilterChange = (key, value) => {
    setFilters((prev) => ({
      ...prev,
      [key]: value,
    }));
  };

  const handleApplyFilters = () => {
    if (!selectedReport) return;
    loadReportData(selectedReport.id, selectedReport);
  };

  const handleClearFilters = () => {
    setFilters({});
    setHierarchyFilters({
      clusterId: "",
      circleId: "",
      zoneId: "",
      areaId: "",
    });
    if (selectedReport) {
      loadReportData(selectedReport.id, selectedReport);
    }
  };

  const loadClusters = async () => {
    try {
      const data = await api.getClusters();
      setClusters(data || []);
      if (!hierarchyFilters.clusterId && user?.clusterName) {
        const match = (data || []).find((item) => item.name === user.clusterName);
        if (match) {
          setHierarchyFilters((prev) => ({ ...prev, clusterId: String(match.id) }));
        }
      }
    } catch (err) {
      console.error("Error loading clusters:", err);
    }
  };

  const loadCircles = async (clusterId) => {
    try {
      const data = await api.getCircles(clusterId);
      setCircles(data || []);
      if (!hierarchyFilters.circleId && user?.circleName) {
        const match = (data || []).find((item) => item.name === user.circleName);
        if (match) {
          setHierarchyFilters((prev) => ({ ...prev, circleId: String(match.id) }));
        }
      }
    } catch (err) {
      console.error("Error loading circles:", err);
    }
  };

  const loadZones = async (circleId) => {
    try {
      const data = await api.getZones(circleId);
      setZones(data || []);
      if (!hierarchyFilters.zoneId && user?.zoneName) {
        const match = (data || []).find((item) => item.name === user.zoneName);
        if (match) {
          setHierarchyFilters((prev) => ({ ...prev, zoneId: String(match.id) }));
        }
      }
    } catch (err) {
      console.error("Error loading zones:", err);
    }
  };

  const loadAreas = async (zoneId) => {
    try {
      const data = await api.getAreas(zoneId);
      setAreas(data || []);
      if (!hierarchyFilters.areaId && user?.areaName) {
        const match = (data || []).find((item) => item.name === user.areaName);
        if (match) {
          setHierarchyFilters((prev) => ({ ...prev, areaId: String(match.id) }));
        }
      }
    } catch (err) {
      console.error("Error loading areas:", err);
    }
  };

  const handleClusterChange = (value) => {
    setHierarchyFilters({
      clusterId: value,
      circleId: "",
      zoneId: "",
      areaId: "",
    });
  };

  const handleCircleChange = (value) => {
    setHierarchyFilters((prev) => ({
      ...prev,
      circleId: value,
      zoneId: "",
      areaId: "",
    }));
  };

  const handleZoneChange = (value) => {
    setHierarchyFilters((prev) => ({
      ...prev,
      zoneId: value,
      areaId: "",
    }));
  };

  const handleAreaChange = (value) => {
    setHierarchyFilters((prev) => ({
      ...prev,
      areaId: value,
    }));
  };

  const handleExport = async () => {
    if (!selectedReport) return;
    
    try {
      setExportLoading(true);
      const exportData = await api.requestExport({
        reportId: selectedReport.id,
        exportFormat,
        parameters,
        filters: normalizeFilters({ ...filters, ...hierarchyFilters }, selectedReport),
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
                        onClick={() => handleSelectReport(report)}
                        className={`w-full text-left px-4 py-3 rounded-lg transition-colors ${
                          selectedReport?.id === report.id
                            ? "bg-slate-900 text-white"
                            : "bg-slate-50 hover:bg-slate-100 text-slate-900"
                        }`}
                      >
                        <div className="font-medium">{report.name}</div>
                        {report.description && (
                          <div
                            className={`text-xs mt-1 ${
                              selectedReport?.id === report.id ? "text-slate-200" : "text-slate-500"
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
                  {(reportFilters.length > 0 ||
                    canShowCluster ||
                    canShowCircle ||
                    canShowZone ||
                    canShowArea) && (
                    <div className="mb-6 rounded-lg border border-slate-200 bg-slate-50 p-4">
                      <div className="flex items-center justify-between mb-3">
                        <h3 className="text-sm font-semibold text-slate-900">Filters</h3>
                        <div className="flex gap-2">
                          <button
                            onClick={handleClearFilters}
                            className="px-3 py-1 text-xs border border-slate-300 rounded-md hover:bg-white"
                          >
                            Clear
                          </button>
                          <button
                            onClick={handleApplyFilters}
                            className="px-3 py-1 text-xs bg-slate-900 text-white rounded-md hover:bg-slate-800"
                          >
                            Apply
                          </button>
                        </div>
                      </div>
                      {(canShowCluster || canShowCircle || canShowZone || canShowArea) && (
                        <div className="grid grid-cols-1 md:grid-cols-4 gap-3 mb-4">
                          {canShowCluster && (
                            <div>
                              <label className="block text-xs font-medium text-slate-600 mb-1">
                                Cluster
                              </label>
                              <select
                                value={hierarchyFilters.clusterId}
                                onChange={(e) => handleClusterChange(e.target.value)}
                                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm bg-white"
                              >
                                <option value="">All</option>
                                {clusters.map((cluster) => (
                                  <option key={cluster.id} value={cluster.id}>
                                    {cluster.name}
                                  </option>
                                ))}
                              </select>
                            </div>
                          )}
                          {canShowCircle && (
                            <div>
                              <label className="block text-xs font-medium text-slate-600 mb-1">
                                Circle
                              </label>
                              <select
                                value={hierarchyFilters.circleId}
                                onChange={(e) => handleCircleChange(e.target.value)}
                                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm bg-white"
                                disabled={!hierarchyFilters.clusterId && canShowCluster}
                              >
                                <option value="">All</option>
                                {circles.map((circle) => (
                                  <option key={circle.id} value={circle.id}>
                                    {circle.name}
                                  </option>
                                ))}
                              </select>
                            </div>
                          )}
                          {canShowZone && (
                            <div>
                              <label className="block text-xs font-medium text-slate-600 mb-1">
                                Zone
                              </label>
                              <select
                                value={hierarchyFilters.zoneId}
                                onChange={(e) => handleZoneChange(e.target.value)}
                                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm bg-white"
                                disabled={!hierarchyFilters.circleId && canShowCircle}
                              >
                                <option value="">All</option>
                                {zones.map((zone) => (
                                  <option key={zone.id} value={zone.id}>
                                    {zone.name}
                                  </option>
                                ))}
                              </select>
                            </div>
                          )}
                          {canShowArea && (
                            <div>
                              <label className="block text-xs font-medium text-slate-600 mb-1">
                                Area
                              </label>
                              <select
                                value={hierarchyFilters.areaId}
                                onChange={(e) => handleAreaChange(e.target.value)}
                                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm bg-white"
                                disabled={!hierarchyFilters.zoneId && canShowZone}
                              >
                                <option value="">All</option>
                                {areas.map((area) => (
                                  <option key={area.id} value={area.id}>
                                    {area.name}
                                  </option>
                                ))}
                              </select>
                            </div>
                          )}
                        </div>
                      )}
                      <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                        {reportFilters.map((filter) => (
                          <div key={filter.key}>
                            <label className="block text-xs font-medium text-slate-600 mb-1">
                              {filter.label}
                            </label>
                            {filter.type === "select" ? (
                              <select
                                value={filters[filter.key] || ""}
                                onChange={(e) => handleFilterChange(filter.key, e.target.value)}
                                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm bg-white"
                              >
                                <option value="">All</option>
                                {(filter.options || []).map((option) => (
                                  <option key={option} value={option}>
                                    {option}
                                  </option>
                                ))}
                              </select>
                            ) : (
                              <input
                                type={filter.type === "date" ? "date" : "text"}
                                value={filters[filter.key] || ""}
                                onChange={(e) => handleFilterChange(filter.key, e.target.value)}
                                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                              />
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
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
