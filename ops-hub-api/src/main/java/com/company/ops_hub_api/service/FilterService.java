package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.*;
import com.company.ops_hub_api.dto.*;
import com.company.ops_hub_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FilterService {

    private final ClusterRepository clusterRepository;
    private final CircleRepository circleRepository;
    private final ZoneRepository zoneRepository;
    private final AreaRepository areaRepository;

    public List<ClusterFilterDTO> getAllClustersWithHierarchy() {
        List<Cluster> clusters = clusterRepository.findAll();
        return clusters.stream()
                .filter(Cluster::getActive)
                .map(this::convertToClusterDTO)
                .collect(Collectors.toList());
    }

    public List<CircleFilterDTO> getCirclesByCluster(Long clusterId) {
        List<Circle> circles = circleRepository.findByClusterId(clusterId);
        return circles.stream()
                .filter(Circle::getActive)
                .map(this::convertToCircleDTO)
                .collect(Collectors.toList());
    }

    public List<ZoneFilterDTO> getZonesByCircle(Long circleId) {
        List<Zone> zones = zoneRepository.findByCircleId(circleId);
        return zones.stream()
                .filter(Zone::getActive)
                .map(this::convertToZoneDTO)
                .collect(Collectors.toList());
    }

    public List<AreaFilterDTO> getAreasByZone(Long zoneId) {
        List<Area> areas = areaRepository.findByZoneId(zoneId);
        return areas.stream()
                .filter(Area::getActive)
                .map(this::convertToAreaDTO)
                .collect(Collectors.toList());
    }

    private ClusterFilterDTO convertToClusterDTO(Cluster cluster) {
        List<CircleFilterDTO> circles = cluster.getCircles() != null
                ? cluster.getCircles().stream()
                    .filter(Circle::getActive)
                    .map(this::convertToCircleDTO)
                    .collect(Collectors.toList())
                : List.of();

        return ClusterFilterDTO.builder()
                .id(cluster.getId())
                .code(cluster.getCode())
                .name(cluster.getName())
                .circles(circles)
                .build();
    }

    private CircleFilterDTO convertToCircleDTO(Circle circle) {
        List<ZoneFilterDTO> zones = circle.getZones() != null
                ? circle.getZones().stream()
                    .filter(Zone::getActive)
                    .map(this::convertToZoneDTO)
                    .collect(Collectors.toList())
                : List.of();

        return CircleFilterDTO.builder()
                .id(circle.getId())
                .code(circle.getCode())
                .name(circle.getName())
                .clusterId(circle.getCluster().getId())
                .zones(zones)
                .build();
    }

    private ZoneFilterDTO convertToZoneDTO(Zone zone) {
        List<AreaFilterDTO> areas = zone.getAreas() != null
                ? zone.getAreas().stream()
                    .filter(Area::getActive)
                    .map(this::convertToAreaDTO)
                    .collect(Collectors.toList())
                : List.of();

        return ZoneFilterDTO.builder()
                .id(zone.getId())
                .code(zone.getCode())
                .name(zone.getName())
                .circleId(zone.getCircle().getId())
                .areas(areas)
                .build();
    }

    private AreaFilterDTO convertToAreaDTO(Area area) {
        return AreaFilterDTO.builder()
                .id(area.getId())
                .code(area.getCode())
                .name(area.getName())
                .zoneId(area.getZone().getId())
                .build();
    }
}
