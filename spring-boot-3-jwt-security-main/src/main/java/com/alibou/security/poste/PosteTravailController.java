package com.alibou.security.poste;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/postes")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PosteTravailController {

    private final PosteTravailService service;

    // ── GET ALL postes ────────────────────────────────────────────────────────
    // ALL authenticated roles can read
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PosteTravailResponse>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PosteTravailResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    // ── GET BY LIGNE ──────────────────────────────────────────────────────────
    @GetMapping("/ligne/{ligne}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PosteTravailResponse>> getByLigne(@PathVariable String ligne) {
        return ResponseEntity.ok(service.findByLigne(ligne));
    }

    // ── GET BY STATUT ─────────────────────────────────────────────────────────
    @GetMapping("/statut/{statut}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PosteTravailResponse>> getByStatut(
            @PathVariable PosteStatut statut) {
        return ResponseEntity.ok(service.findByStatut(statut));
    }

    // ── CREATE — ADMIN or RESPONSABLE_PRODUCTION only ─────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_PRODUCTION')")
    public ResponseEntity<PosteTravailResponse> create(
            @RequestBody PosteTravailDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    // ── UPDATE — ADMIN or RESPONSABLE_PRODUCTION only ─────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_PRODUCTION')")
    public ResponseEntity<PosteTravailResponse> update(
            @PathVariable Long id,
            @RequestBody PosteTravailDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    // ── TOGGLE STATUT — ADMIN or RESPONSABLE_PRODUCTION only ──────────────────
    @PatchMapping("/{id}/toggle-statut")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_PRODUCTION')")
    public ResponseEntity<PosteTravailResponse> toggleStatut(@PathVariable Long id) {
        return ResponseEntity.ok(service.toggleStatut(id));
    }

    // ── UPDATE KPI — PREPARATEUR, RESPONSABLE, ADMIN ──────────────────────────
    // Called when a PREPARATEUR saisit indicateurs for a poste
    @PatchMapping("/{id}/kpi")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESPONSABLE_PRODUCTION', 'PREPARATEUR')")
    public ResponseEntity<PosteTravailResponse> updateKpi(
            @PathVariable Long id,
            @RequestBody Map<String, Double> kpi) {
        return ResponseEntity.ok(
                service.updateKpi(id, kpi.get("trg"), kpi.get("trs"), kpi.get("fpy")));
    }

    // ── DELETE — ADMIN only ────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

