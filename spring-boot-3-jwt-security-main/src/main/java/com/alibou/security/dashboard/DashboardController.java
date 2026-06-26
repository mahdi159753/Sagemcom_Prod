package com.alibou.security.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/alerts")
    public ResponseEntity<List<DashboardAlertDTO>> getRecentAlerts() {
        return ResponseEntity.ok(service.getRecentAlerts());
    }

    @GetMapping("/downtime-pie")
    public ResponseEntity<List<DashboardPieChartDTO>> getDowntimeCausesPie() {
        return ResponseEntity.ok(service.getDowntimeCausesPie());
    }
}
