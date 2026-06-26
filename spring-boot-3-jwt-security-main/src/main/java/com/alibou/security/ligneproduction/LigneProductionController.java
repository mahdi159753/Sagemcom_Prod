package com.alibou.security.ligneproduction;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/lignes")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class LigneProductionController {

    private final LigneProductionService service;

    // ── GET ALL ──────────────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LigneProductionResponse>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    // ── GET BY ID ────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LigneProductionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    // ── GET BY STATUT ────────────────────────────────────────────────────────
    @GetMapping("/statut/{statut}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LigneProductionResponse>> getByStatut(
            @PathVariable LigneStatut statut) {
        return ResponseEntity.ok(service.findByStatut(statut));
    }

    // ── GET BY CHANTIER ──────────────────────────────────────────────────────
    @GetMapping("/chantier/{chantier}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LigneProductionResponse>> getByChantier(
            @PathVariable String chantier) {
        return ResponseEntity.ok(service.findByChantier(chantier));
    }

    // ── CREATE — ADMIN or RESPONSABLE_PRODUCTION ─────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_PRODUCTION')")
    public ResponseEntity<LigneProductionResponse> create(@RequestBody LigneProductionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    // ── UPDATE — ADMIN or RESPONSABLE_PRODUCTION ─────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_PRODUCTION')")
    public ResponseEntity<LigneProductionResponse> update(
            @PathVariable Long id, @RequestBody LigneProductionDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    // ── CHANGE STATUS — ADMIN or RESPONSABLE_PRODUCTION ──────────────────────
    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_PRODUCTION')")
    public ResponseEntity<LigneProductionResponse> changeStatut(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        LigneStatut newStatut = LigneStatut.valueOf(body.get("statut"));
        String cause = body.getOrDefault("cause", null);
        return ResponseEntity.ok(service.changeStatut(id, newStatut, cause));
    }

    // ── UPDATE KPI — ADMIN, RESPONSABLE_PRODUCTION, PREPARATEUR ──────────────
    @PatchMapping("/{id}/kpi")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_PRODUCTION', 'PREPARATEUR')")
    public ResponseEntity<LigneProductionResponse> updateKpi(
            @PathVariable Long id,
            @RequestBody Map<String, Double> kpi) {
        return ResponseEntity.ok(service.updateKpi(id, kpi));
    }

    // ── DELETE — ADMIN only ──────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
