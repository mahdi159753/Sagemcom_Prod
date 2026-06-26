package com.alibou.security.downtime;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.alibou.security.downtime.prediction.PredictiveInsight;

import java.util.List;

@RestController
@RequestMapping("/api/v1/downtimes")
@RequiredArgsConstructor
public class DowntimeController {

    private final DowntimeService service;

    @PostMapping
    public ResponseEntity<Downtime> declareDowntime(@RequestBody Downtime downtime) {
        return ResponseEntity.ok(service.declareDowntime(downtime));
    }

    @GetMapping
    public ResponseEntity<List<Downtime>> getAllDowntimes() {
        return ResponseEntity.ok(service.getAllDowntimes());
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<Downtime> resolveDowntime(@PathVariable Integer id) {
        return ResponseEntity.ok(service.resolveDowntime(id));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Downtime> updateDowntime(@PathVariable Integer id, @RequestBody Downtime downtime) {
        return ResponseEntity.ok(service.updateDowntime(id, downtime));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDowntime(@PathVariable Integer id) {
        service.deleteDowntime(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<DowntimeStatsResponse> getStats() {
        return ResponseEntity.ok(service.getStats());
    }

    @GetMapping("/predict")
    public ResponseEntity<List<PredictiveInsight>> getPredictions() {
        return ResponseEntity.ok(service.getPredictiveInsights());
    }
}
