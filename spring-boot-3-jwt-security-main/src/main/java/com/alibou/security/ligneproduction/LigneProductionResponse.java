package com.alibou.security.ligneproduction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneProductionResponse {

    private Long id;
    private String code;
    private String nom;
    private String chantier;

    // Product info (flattened)
    private Integer produitId;
    private String produitNom;
    private String produitReference;
    private String produitCategory;

    private LigneStatut statut;
    private String shiftActuel;
    private String responsable;

    private Integer cadenceReelle;
    private Integer cadenceObjectif;
    private Double efficiency;       // computed: (cadenceReelle / cadenceObjectif) * 100

    private Double trg;
    private Double trs;
    private Double fpy;

    private LocalDateTime dernierArret;
    private String causeArret;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Build a response from entity.
     */
    public static LigneProductionResponse from(LigneProduction entity) {
        double eff = 0.0;
        if (entity.getCadenceObjectif() != null && entity.getCadenceObjectif() > 0
                && entity.getCadenceReelle() != null) {
            eff = Math.round((entity.getCadenceReelle() * 100.0) / entity.getCadenceObjectif() * 10.0) / 10.0;
        }

        return LigneProductionResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .nom(entity.getNom())
                .chantier(entity.getChantier())
                .produitId(entity.getProduit() != null ? entity.getProduit().getId() : null)
                .produitNom(entity.getProduit() != null ? entity.getProduit().getName() : null)
                .produitReference(entity.getProduit() != null ? entity.getProduit().getReference() : null)
                .produitCategory(entity.getProduit() != null ? entity.getProduit().getCategory() : null)
                .statut(entity.getStatut())
                .shiftActuel(entity.getShiftActuel())
                .responsable(entity.getResponsable())
                .cadenceReelle(entity.getCadenceReelle())
                .cadenceObjectif(entity.getCadenceObjectif())
                .efficiency(eff)
                .trg(entity.getTrg())
                .trs(entity.getTrs())
                .fpy(entity.getFpy())
                .dernierArret(entity.getDernierArret())
                .causeArret(entity.getCauseArret())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
