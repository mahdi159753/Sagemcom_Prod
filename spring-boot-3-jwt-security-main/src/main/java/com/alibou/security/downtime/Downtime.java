package com.alibou.security.downtime;

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
@Table(name = "downtimes")
public class Downtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String ligne;
    private String chantier;   // INTEG, CMS2, CMS1, ASSEMBLY
    private String produit;
    private String type;
    
    @Column(length = 2000)
    private String description;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private String operateur;
    
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private String statut; // ONGOING, RESOLVED
    
    @PrePersist
    protected void onCreate() {
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
        if (statut == null) {
            statut = "ONGOING";
        }
    }
}
