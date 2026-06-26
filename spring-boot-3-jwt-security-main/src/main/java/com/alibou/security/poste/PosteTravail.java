package com.alibou.security.poste;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "poste_travail")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosteTravail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;               // e.g. "P001-ASM"

    @Column(nullable = false)
    private String libelle;            // e.g. "Assemblage Carte Mere"

    @Column(nullable = false)
    private String ligne;              // e.g. "Ligne 1"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PosteType type;            // ASSEMBLAGE / TEST / CONTROLE_QUALITE / EMBALLAGE

    private String ficheInstruction;   // e.g. "FI-ASM-001"
    private String ficheVersion;       // e.g. "v2.1 PROD"
    private String operateur;          // assigned operator name

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PosteStatut statut = PosteStatut.ACTIF;

    // Real-time KPI values (updated by PREPARATEUR via indicateur saisie)
    @Builder.Default private Double trg = 0.0;
    @Builder.Default private Double trs = 0.0;
    @Builder.Default private Double fpy = 0.0;
}
