package com.alibou.security.figuide;

import com.alibou.security.produit.Produit;
import com.alibou.security.user.User;
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
@Table(name = "control_session")
public class ControlSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fi_document_id", nullable = false)
    private FiDocument fiDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", nullable = false)
    private User operator;

    // IN_PROGRESS, COMPLETED, FAILED
    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private Boolean conforme;

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
        status = "IN_PROGRESS";
        conforme = true; // Assume true until a non-conformance is recorded
    }
}
