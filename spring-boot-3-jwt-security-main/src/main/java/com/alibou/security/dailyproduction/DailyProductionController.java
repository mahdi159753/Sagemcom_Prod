package com.alibou.security.dailyproduction;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/daily-production")
@RequiredArgsConstructor
public class DailyProductionController {
    
    private final DailyProductionService service;

    @GetMapping("/kpi-summary")
    public ResponseEntity<KpiSummaryResponse> getKpiSummary(
            @RequestParam String chantier,
            @RequestParam String from,
            @RequestParam String to) {
        LocalDate dateFrom = LocalDate.parse(from);
        LocalDate dateTo = LocalDate.parse(to);
        return ResponseEntity.ok(service.getKpiSummary(chantier, dateFrom, dateTo));
    }

    @GetMapping
    public ResponseEntity<List<DailyProductionIndicator>> getIndicators(
            @RequestParam String date,
            @RequestParam String chantier) {
        LocalDate parsedDate = LocalDate.parse(date);
        return ResponseEntity.ok(service.getIndicators(parsedDate, chantier));
    }

    @PostMapping("/batch")
    public ResponseEntity<Void> backupIndicators(@RequestBody List<DailyProductionDTO> batch) {
        service.saveBatch(batch);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteIndicators(
            @RequestParam String date,
            @RequestParam String chantier) {
        LocalDate parsedDate = LocalDate.parse(date);
        service.deleteByDateAndChantier(parsedDate, chantier);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/indicator")
    public ResponseEntity<Void> deleteIndicator(
            @RequestParam String date,
            @RequestParam String chantier,
            @RequestParam String indicatorName) {
        LocalDate parsedDate = LocalDate.parse(date);
        service.deleteByIndicatorName(parsedDate, chantier, indicatorName);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ai/predict-oee")
    public ResponseEntity<List<OeePrediction>> predictOee(@RequestParam String chantier) {
        return ResponseEntity.ok(service.getOeePredictions(chantier));
    }
}
