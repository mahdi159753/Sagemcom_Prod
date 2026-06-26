package com.alibou.security.ligneproduction;

import com.alibou.security.produit.Produit;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ligne_production")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneProduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;                    // e.g. "LIG-001"

    @Column(nullable = false)
    private String nom;                     // e.g. "Ligne 1"

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produit_id")
    private Produit produit;                // Current product being manufactured

    @Column(length = 50)
    private String chantier;                // e.g. "INTEG", "CMS2"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LigneStatut statut = LigneStatut.EN_PRODUCTION;

    private String shiftActuel;             // e.g. "Morning (06:00-14:00)"
    private String responsable;             // Line supervisor name

    @Builder.Default
    private Integer cadenceReelle = 0;      // Actual units/hour

    @Builder.Default
    private Integer cadenceObjectif = 200;  // Target units/hour

    // ── Aggregated KPIs (from workstations) ───────────────────────────
    @Builder.Default private Double trg = 0.0;   // Taux de Rendement Global
    @Builder.Default private Double trs = 0.0;   // Taux de Rendement Synthétique
    @Builder.Default private Double fpy = 0.0;   // First Pass Yield

    // ── Last stop info ────────────────────────────────────────────────
    private LocalDateTime dernierArret;     // Last stop timestamp
    @Column(length = 500)
    private String causeArret;              // Last stop cause

    // ── Timestamps ────────────────────────────────────────────────────
    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
