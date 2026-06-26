package com.alibou.security.figuide;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "instruction_step")
public class InstructionStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fi_document_id", nullable = false)
    private FiDocument fiDocument;

    @Column(columnDefinition = "TEXT")
    private String stepNumber;

    private Integer sequenceOrder;

    @Column(columnDefinition = "TEXT")
    private String operation;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String warningText;

    @Column(columnDefinition = "TEXT")
    private String imageUrls;
}
