package com.alibou.security.figuide;

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
@Table(name = "control_step_validation")
public class ControlStepValidation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "control_session_id", nullable = false)
    private ControlSession controlSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instruction_step_id", nullable = false)
    private InstructionStep instructionStep;

    // CONFORME, NON_CONFORME
    private String status;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private LocalDateTime validatedAt;

    @PrePersist
    protected void onCreate() {
        validatedAt = LocalDateTime.now();
    }
}
