package com.alibou.security.ligneproduction;

import com.alibou.security.produit.Produit;
import com.alibou.security.produit.ProduitRepository;
import com.alibou.security.notification.NotificationService;
import com.alibou.security.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LigneProductionService {

    private final LigneProductionRepository repository;
    private final ProduitRepository produitRepository;
    private final NotificationService notificationService;

    // ── GET ALL ──────────────────────────────────────────────────────────────
    public List<LigneProductionResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(LigneProductionResponse::from)
                .collect(Collectors.toList());
    }

    // ── GET BY ID ────────────────────────────────────────────────────────────
    public LigneProductionResponse findById(Long id) {
        LigneProduction ligne = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ligne de production non trouvée: " + id));
        return LigneProductionResponse.from(ligne);
    }

    // ── GET BY STATUT ────────────────────────────────────────────────────────
    public List<LigneProductionResponse> findByStatut(LigneStatut statut) {
        return repository.findByStatut(statut)
                .stream()
                .map(LigneProductionResponse::from)
                .collect(Collectors.toList());
    }

    // ── GET BY CHANTIER ──────────────────────────────────────────────────────
    public List<LigneProductionResponse> findByChantier(String chantier) {
        return repository.findByChantier(chantier)
                .stream()
                .map(LigneProductionResponse::from)
                .collect(Collectors.toList());
    }

    // ── CREATE ───────────────────────────────────────────────────────────────
    public LigneProductionResponse create(LigneProductionDTO dto) {
        if (repository.existsByCode(dto.getCode())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Une ligne avec le code '" + dto.getCode() + "' existe déjà");
        }

        Produit produit = null;
        if (dto.getProduitId() != null) {
            produit = produitRepository.findById(dto.getProduitId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Produit non trouvé: " + dto.getProduitId()));
        }

        LigneProduction ligne = LigneProduction.builder()
                .code(dto.getCode())
                .nom(dto.getNom())
                .chantier(dto.getChantier())
                .produit(produit)
                .statut(dto.getStatut() != null ? dto.getStatut() : LigneStatut.EN_PRODUCTION)
                .shiftActuel(dto.getShiftActuel())
                .responsable(dto.getResponsable())
                .cadenceReelle(dto.getCadenceReelle() != null ? dto.getCadenceReelle() : 0)
                .cadenceObjectif(dto.getCadenceObjectif() != null ? dto.getCadenceObjectif() : 200)
                .trg(dto.getTrg() != null ? dto.getTrg() : 0.0)
                .trs(dto.getTrs() != null ? dto.getTrs() : 0.0)
                .fpy(dto.getFpy() != null ? dto.getFpy() : 0.0)
                .build();

        return LigneProductionResponse.from(repository.save(ligne));
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────
    public LigneProductionResponse update(Long id, LigneProductionDTO dto) {
        LigneProduction existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ligne de production non trouvée: " + id));

        // Check code uniqueness only if changed
        if (!existing.getCode().equals(dto.getCode()) && repository.existsByCode(dto.getCode())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Une ligne avec le code '" + dto.getCode() + "' existe déjà");
        }

        if (dto.getProduitId() != null) {
            Produit produit = produitRepository.findById(dto.getProduitId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Produit non trouvé: " + dto.getProduitId()));
            existing.setProduit(produit);
        }

        existing.setCode(dto.getCode());
        existing.setNom(dto.getNom());
        existing.setChantier(dto.getChantier());
        if (dto.getStatut() != null) existing.setStatut(dto.getStatut());
        existing.setShiftActuel(dto.getShiftActuel());
        existing.setResponsable(dto.getResponsable());
        if (dto.getCadenceReelle() != null) existing.setCadenceReelle(dto.getCadenceReelle());
        if (dto.getCadenceObjectif() != null) existing.setCadenceObjectif(dto.getCadenceObjectif());
        if (dto.getTrg() != null) existing.setTrg(dto.getTrg());
        if (dto.getTrs() != null) existing.setTrs(dto.getTrs());
        if (dto.getFpy() != null) existing.setFpy(dto.getFpy());

        return LigneProductionResponse.from(repository.save(existing));
    }

    // ── CHANGE STATUS ────────────────────────────────────────────────────────
    // When status → ARRETEE → record stop time + cause
    // When status → EN_PRODUCTION → clear cause (line resumed)
    public LigneProductionResponse changeStatut(Long id, LigneStatut newStatut, String cause) {
        LigneProduction ligne = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ligne de production non trouvée: " + id));

        ligne.setStatut(newStatut);

        if (newStatut == LigneStatut.ARRETEE) {
            ligne.setDernierArret(LocalDateTime.now());
            ligne.setCauseArret(cause != null ? cause : "Cause non spécifiée");
            // KPIs drop to 0 when stopped
            ligne.setTrg(0.0);
            ligne.setCadenceReelle(0);
        } else if (newStatut == LigneStatut.CHANGEMENT_SERIE) {
            ligne.setCauseArret("Changement de série");
            ligne.setCadenceReelle(0);
        }

        return LigneProductionResponse.from(repository.save(ligne));
    }

    // ── UPDATE KPI ───────────────────────────────────────────────────────────
    public LigneProductionResponse updateKpi(Long id, Map<String, Double> kpi) {
        LigneProduction ligne = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ligne de production non trouvée: " + id));

        if (kpi.containsKey("trg")) {
            Double trg = kpi.get("trg");
            ligne.setTrg(trg);
            if (trg < 70.0) {
                notificationService.sendAlert("Alerte TRG ! La ligne " + ligne.getCode() + " a un TRG de " + trg + "%", NotificationType.DOWNTIME);
            }
        }
        if (kpi.containsKey("trs")) ligne.setTrs(kpi.get("trs"));
        if (kpi.containsKey("fpy")) ligne.setFpy(kpi.get("fpy"));
        if (kpi.containsKey("cadenceReelle")) ligne.setCadenceReelle(kpi.get("cadenceReelle").intValue());

        return LigneProductionResponse.from(repository.save(ligne));
    }

    // ── DELETE ───────────────────────────────────────────────────────────────
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Ligne de production non trouvée: " + id);
        }
        repository.deleteById(id);
    }
}
