package com.alibou.security.nc;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "non_conformities")
public class NonConformity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String reference; // e.g., manually entered like NC-2026-001

    private String origine; // USINE or CLIENT

    // Fields for USINE
    private String localisation; // e.g., Ligne 1 - Poste 3

    // Shared Fields
    private String gravite; // MINEUR, MAJEUR, CRITIQUE
    private String statut; // OUVERTE, EN_TRAITEMENT, A_VALIDER, CLOTUREE
    private String assigneeEmail; // Email de l'utilisateur assigné

    @Column(length = 2000)
    private String description;
    @Column(length = 2000)
    private String actionCorrective;

    // Fields for CLIENT
    private String procedureType; // e.g., 8D, QRQC
    private String lotConcerne;
    private Boolean valideeParClient;

    private String reportPdfPath;

    @Column(updatable = false)
    private LocalDateTime dateDeclaration;
    private LocalDateTime dateCloture;

    @PrePersist
    protected void onCreate() {
        if (dateDeclaration == null) {
            dateDeclaration = LocalDateTime.now();
        }
        // Ensure Client NCs are always CRITIQUE
        if ("CLIENT".equalsIgnoreCase(origine)) {
            this.gravite = "CRITIQUE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if ("CLIENT".equalsIgnoreCase(origine)) {
            this.gravite = "CRITIQUE";
        }
        if ("CLOTUREE".equalsIgnoreCase(statut) && dateCloture == null) {
            dateCloture = LocalDateTime.now();
        } else if (!"CLOTUREE".equalsIgnoreCase(statut)) {
            dateCloture = null; // reset if reopened
        }
    }
}
