"use client";

import { useState, useEffect } from "react";
import { api } from "@/lib/api";

export default function HierarchyFilter({ onFilterChange, className = "" }) {
  const [clusters, setClusters] = useState([]);
  const [circles, setCircles] = useState([]);
  const [zones, setZones] = useState([]);
  const [areas, setAreas] = useState([]);

  const [selectedCluster, setSelectedCluster] = useState(null);
  const [selectedCircle, setSelectedCircle] = useState(null);
  const [selectedZone, setSelectedZone] = useState(null);
  const [selectedArea, setSelectedArea] = useState(null);

  const [loading, setLoading] = useState({
    clusters: false,
    circles: false,
    zones: false,
    areas: false,
  });

  const [error, setError] = useState(null);

  // Load clusters on mount
  useEffect(() => {
    loadClusters();
  }, []);

  // Load circles when cluster is selected
  useEffect(() => {
    if (selectedCluster) {
      loadCircles(selectedCluster);
      // Reset downstream selections
      setSelectedCircle(null);
      setSelectedZone(null);
      setSelectedArea(null);
      setZones([]);
      setAreas([]);
    } else {
      setCircles([]);
    }
  }, [selectedCluster]);

  // Load zones when circle is selected
  useEffect(() => {
    if (selectedCircle) {
      loadZones(selectedCircle);
      // Reset downstream selections
      setSelectedZone(null);
      setSelectedArea(null);
      setAreas([]);
    } else {
      setZones([]);
    }
  }, [selectedCircle]);

  // Load areas when zone is selected
  useEffect(() => {
    if (selectedZone) {
      loadAreas(selectedZone);
      setSelectedArea(null);
    } else {
      setAreas([]);
    }
  }, [selectedZone]);

  // Notify parent when selection changes
  useEffect(() => {
    if (onFilterChange) {
      onFilterChange({
        cluster: selectedCluster,
        circle: selectedCircle,
        zone: selectedZone,
        area: selectedArea,
      });
    }
  }, [selectedCluster, selectedCircle, selectedZone, selectedArea, onFilterChange]);

  const loadClusters = async () => {
    setLoading(prev => ({ ...prev, clusters: true }));
    setError(null);
    try {
      const data = await api.getClusters();
      setClusters(data);
    } catch (err) {
      setError(err.message);
      console.error("Error loading clusters:", err);
    } finally {
      setLoading(prev => ({ ...prev, clusters: false }));
    }
  };

  const loadCircles = async (clusterId) => {
    setLoading(prev => ({ ...prev, circles: true }));
    try {
      const data = await api.getCircles(clusterId);
      setCircles(data);
    } catch (err) {
      console.error("Error loading circles:", err);
      setError(err.message);
    } finally {
      setLoading(prev => ({ ...prev, circles: false }));
    }
  };

  const loadZones = async (circleId) => {
    setLoading(prev => ({ ...prev, zones: true }));
    try {
      const data = await api.getZones(circleId);
      setZones(data);
    } catch (err) {
      console.error("Error loading zones:", err);
      setError(err.message);
    } finally {
      setLoading(prev => ({ ...prev, zones: false }));
    }
  };

  const loadAreas = async (zoneId) => {
    setLoading(prev => ({ ...prev, areas: true }));
    try {
      const data = await api.getAreas(zoneId);
      setAreas(data);
    } catch (err) {
      console.error("Error loading areas:", err);
      setError(err.message);
    } finally {
      setLoading(prev => ({ ...prev, areas: false }));
    }
  };

  const handleReset = () => {
    setSelectedCluster(null);
    setSelectedCircle(null);
    setSelectedZone(null);
    setSelectedArea(null);
    setCircles([]);
    setZones([]);
    setAreas([]);
  };

  return (
    <div className={`space-y-4 ${className}`}>
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Cluster Filter */}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">
            Cluster
          </label>
          <select
            value={selectedCluster || ""}
            onChange={(e) => setSelectedCluster(e.target.value ? Number(e.target.value) : null)}
            className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent transition-all outline-none text-slate-900 bg-white"
            disabled={loading.clusters}
          >
            <option value="">All Clusters</option>
            {clusters.map((cluster) => (
              <option key={cluster.id} value={cluster.id}>
                {cluster.name} ({cluster.code})
              </option>
            ))}
          </select>
          {loading.clusters && (
            <p className="text-xs text-slate-500 mt-1">Loading...</p>
          )}
        </div>

        {/* Circle Filter */}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">
            Circle
          </label>
          <select
            value={selectedCircle || ""}
            onChange={(e) => setSelectedCircle(e.target.value ? Number(e.target.value) : null)}
            className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent transition-all outline-none text-slate-900 bg-white disabled:bg-slate-50 disabled:text-slate-400"
            disabled={!selectedCluster || loading.circles}
          >
            <option value="">All Circles</option>
            {circles.map((circle) => (
              <option key={circle.id} value={circle.id}>
                {circle.name} ({circle.code})
              </option>
            ))}
          </select>
          {loading.circles && (
            <p className="text-xs text-slate-500 mt-1">Loading...</p>
          )}
        </div>

        {/* Zone Filter */}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">
            Zone
          </label>
          <select
            value={selectedZone || ""}
            onChange={(e) => setSelectedZone(e.target.value ? Number(e.target.value) : null)}
            className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent transition-all outline-none text-slate-900 bg-white disabled:bg-slate-50 disabled:text-slate-400"
            disabled={!selectedCircle || loading.zones}
          >
            <option value="">All Zones</option>
            {zones.map((zone) => (
              <option key={zone.id} value={zone.id}>
                {zone.name} ({zone.code})
              </option>
            ))}
          </select>
          {loading.zones && (
            <p className="text-xs text-slate-500 mt-1">Loading...</p>
          )}
        </div>

        {/* Area Filter */}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-2">
            Area
          </label>
          <select
            value={selectedArea || ""}
            onChange={(e) => setSelectedArea(e.target.value ? Number(e.target.value) : null)}
            className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-900 focus:border-transparent transition-all outline-none text-slate-900 bg-white disabled:bg-slate-50 disabled:text-slate-400"
            disabled={!selectedZone || loading.areas}
          >
            <option value="">All Areas</option>
            {areas.map((area) => (
              <option key={area.id} value={area.id}>
                {area.name} ({area.code})
              </option>
            ))}
          </select>
          {loading.areas && (
            <p className="text-xs text-slate-500 mt-1">Loading...</p>
          )}
        </div>
      </div>

      {/* Reset Button */}
      {(selectedCluster || selectedCircle || selectedZone || selectedArea) && (
        <div className="flex justify-end">
          <button
            onClick={handleReset}
            className="px-4 py-2 text-sm font-medium text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition-colors"
          >
            Clear Filters
          </button>
        </div>
      )}

      {/* Selected Filters Display */}
      {(selectedCluster || selectedCircle || selectedZone || selectedArea) && (
        <div className="bg-slate-50 border border-slate-200 rounded-lg p-4">
          <p className="text-sm font-medium text-slate-700 mb-2">Active Filters:</p>
          <div className="flex flex-wrap gap-2">
            {selectedCluster && (
              <span className="inline-flex items-center gap-1 px-3 py-1 bg-slate-900 text-white text-sm rounded-full">
                Cluster: {clusters.find(c => c.id === selectedCluster)?.name}
                <button
                  onClick={() => setSelectedCluster(null)}
                  className="ml-1 hover:text-slate-300"
                >
                  ×
                </button>
              </span>
            )}
            {selectedCircle && (
              <span className="inline-flex items-center gap-1 px-3 py-1 bg-slate-700 text-white text-sm rounded-full">
                Circle: {circles.find(c => c.id === selectedCircle)?.name}
                <button
                  onClick={() => setSelectedCircle(null)}
                  className="ml-1 hover:text-slate-300"
                >
                  ×
                </button>
              </span>
            )}
            {selectedZone && (
              <span className="inline-flex items-center gap-1 px-3 py-1 bg-slate-600 text-white text-sm rounded-full">
                Zone: {zones.find(z => z.id === selectedZone)?.name}
                <button
                  onClick={() => setSelectedZone(null)}
                  className="ml-1 hover:text-slate-300"
                >
                  ×
                </button>
              </span>
            )}
            {selectedArea && (
              <span className="inline-flex items-center gap-1 px-3 py-1 bg-slate-500 text-white text-sm rounded-full">
                Area: {areas.find(a => a.id === selectedArea)?.name}
                <button
                  onClick={() => setSelectedArea(null)}
                  className="ml-1 hover:text-slate-300"
                >
                  ×
                </button>
              </span>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
