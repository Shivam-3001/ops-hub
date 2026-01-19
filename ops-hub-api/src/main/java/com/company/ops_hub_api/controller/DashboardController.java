package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.dto.DashboardResponseDTO;
import com.company.ops_hub_api.security.RequiresPermission;
import com.company.ops_hub_api.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @RequiresPermission("VIEW_CUSTOMERS")
    public ResponseEntity<DashboardResponseDTO> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboard());
    }
}
