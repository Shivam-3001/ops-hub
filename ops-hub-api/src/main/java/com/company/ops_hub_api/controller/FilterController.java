package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.dto.*;
import com.company.ops_hub_api.service.FilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/filters")
@RequiredArgsConstructor
public class FilterController {

    private final FilterService filterService;

    @GetMapping("/clusters")
    public ResponseEntity<List<ClusterFilterDTO>> getAllClustersWithHierarchy() {
        List<ClusterFilterDTO> clusters = filterService.getAllClustersWithHierarchy();
        return ResponseEntity.ok(clusters);
    }

    @GetMapping("/circles")
    public ResponseEntity<List<CircleFilterDTO>> getCirclesByCluster(
            @RequestParam(required = false) Long clusterId) {
        if (clusterId != null) {
            return ResponseEntity.ok(filterService.getCirclesByCluster(clusterId));
        }
        // If no clusterId provided, return empty list
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/zones")
    public ResponseEntity<List<ZoneFilterDTO>> getZonesByCircle(
            @RequestParam(required = false) Long circleId) {
        if (circleId != null) {
            return ResponseEntity.ok(filterService.getZonesByCircle(circleId));
        }
        // If no circleId provided, return empty list
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/areas")
    public ResponseEntity<List<AreaFilterDTO>> getAreasByZone(
            @RequestParam(required = false) Long zoneId) {
        if (zoneId != null) {
            return ResponseEntity.ok(filterService.getAreasByZone(zoneId));
        }
        // If no zoneId provided, return empty list
        return ResponseEntity.ok(List.of());
    }
}
