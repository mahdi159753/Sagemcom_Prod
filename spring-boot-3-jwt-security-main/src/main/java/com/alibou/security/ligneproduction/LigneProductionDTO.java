package com.alibou.security.ligneproduction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneProductionDTO {

    private String code;                 // e.g. "LIG-001"
    private String nom;                  // e.g. "Ligne 1"
    private String chantier;             // e.g. "INTEG", "CMS2"
    private Integer produitId;           // FK to Produit
    private LigneStatut statut;
    private String shiftActuel;          // e.g. "Morning (06:00-14:00)"
    private String responsable;          // Supervisor name
    private Integer cadenceReelle;       // Actual units/hour
    private Integer cadenceObjectif;     // Target units/hour
    private Double trg;
    private Double trs;
    private Double fpy;
}
