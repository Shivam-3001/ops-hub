package com.company.ops_hub_api.util;

import com.company.ops_hub_api.domain.Area;
import com.company.ops_hub_api.domain.User;

import java.util.HashMap;
import java.util.Map;

public final class HierarchyUtil {
    public static final String ADMIN = "ADMIN";
    public static final String CLUSTER_HEAD = "CLUSTER_HEAD";
    public static final String CIRCLE_HEAD = "CIRCLE_HEAD";
    public static final String ZONE_HEAD = "ZONE_HEAD";
    public static final String AREA_HEAD = "AREA_HEAD";
    public static final String STORE_HEAD = "STORE_HEAD";
    public static final String AGENT = "AGENT";

    private HierarchyUtil() {}

    public static String normalizeUserType(User user) {
        return normalizeUserType(user != null ? user.getUserType() : null);
    }

    public static String normalizeUserType(String userType) {
        if (userType == null) {
            return "";
        }
        String normalized = userType.trim().toUpperCase();
        switch (normalized) {
            case "CLUSTER_HEAD":
            case "CLUSTER_LEAD":
                return CLUSTER_HEAD;
            case "CIRCLE_HEAD":
            case "CIRCLE_LEAD":
                return CIRCLE_HEAD;
            case "ZONE_HEAD":
            case "ZONE_LEAD":
                return ZONE_HEAD;
            case "AREA_HEAD":
            case "AREA_LEAD":
                return AREA_HEAD;
            case "STORE_LEAD":
            case "STORE":
            case "STORE_HEAD":
                return STORE_HEAD;
            case "ANALYST":
            case "FIELD_AGENT":
            case "AGENT":
                return AGENT;
            default:
                return normalized;
        }
    }

    public static int hierarchyLevel(String userType) {
        if (userType == null) {
            return -1;
        }
        switch (normalizeUserType(userType)) {
            case ADMIN:
                return 6;
            case CLUSTER_HEAD:
                return 5;
            case CIRCLE_HEAD:
                return 4;
            case ZONE_HEAD:
                return 3;
            case AREA_HEAD:
                return 2;
            case STORE_HEAD:
                return 1;
            case AGENT:
                return 0;
            default:
                return -1;
        }
    }

    public static boolean isAbove(String approverType, String targetType) {
        return hierarchyLevel(approverType) > hierarchyLevel(targetType);
    }

    public static Long getAreaId(User user) {
        return user != null && user.getArea() != null ? user.getArea().getId() : null;
    }

    public static Long getZoneId(User user) {
        return user != null && user.getArea() != null && user.getArea().getZone() != null
                ? user.getArea().getZone().getId() : null;
    }

    public static Long getCircleId(User user) {
        return user != null && user.getArea() != null && user.getArea().getZone() != null
                && user.getArea().getZone().getCircle() != null
                ? user.getArea().getZone().getCircle().getId() : null;
    }

    public static Long getClusterId(User user) {
        return user != null && user.getArea() != null && user.getArea().getZone() != null
                && user.getArea().getZone().getCircle() != null
                && user.getArea().getZone().getCircle().getCluster() != null
                ? user.getArea().getZone().getCircle().getCluster().getId() : null;
    }

    public static Long getZoneId(Area area) {
        return area != null && area.getZone() != null ? area.getZone().getId() : null;
    }

    public static Long getCircleId(Area area) {
        return area != null && area.getZone() != null && area.getZone().getCircle() != null
                ? area.getZone().getCircle().getId() : null;
    }

    public static Long getClusterId(Area area) {
        return area != null && area.getZone() != null && area.getZone().getCircle() != null
                && area.getZone().getCircle().getCluster() != null
                ? area.getZone().getCircle().getCluster().getId() : null;
    }

    public static Map<String, Object> buildGeographyContext(User user) {
        Map<String, Object> geo = new HashMap<>();
        if (user == null || user.getArea() == null) {
            return geo;
        }
        geo.put("areaId", getAreaId(user));
        geo.put("areaName", user.getArea().getName());
        geo.put("zoneId", getZoneId(user));
        geo.put("zoneName", user.getArea().getZone() != null ? user.getArea().getZone().getName() : null);
        geo.put("circleId", getCircleId(user));
        geo.put("circleName", user.getArea().getZone() != null && user.getArea().getZone().getCircle() != null
                ? user.getArea().getZone().getCircle().getName() : null);
        geo.put("clusterId", getClusterId(user));
        geo.put("clusterName", user.getArea().getZone() != null && user.getArea().getZone().getCircle() != null
                && user.getArea().getZone().getCircle().getCluster() != null
                ? user.getArea().getZone().getCircle().getCluster().getName() : null);
        return geo;
    }
}
