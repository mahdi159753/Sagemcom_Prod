package com.alibou.security.poste;

import lombok.*;

/** Used for both CREATE and UPDATE requests from Angular */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosteTravailDTO {
    private String      code;
    private String      libelle;
    private String      ligne;
    private PosteType   type;
    private String      ficheInstruction;
    private String      ficheVersion;
    private String      operateur;
    private PosteStatut statut;
    private Double      trg;
    private Double      trs;
    private Double      fpy;

    /** Convert JPA entity → DTO (used in service responses) */
    public static PosteTravailDTO from(PosteTravail p) {
        return PosteTravailDTO.builder()
                .code(p.getCode())
                .libelle(p.getLibelle())
                .ligne(p.getLigne())
                .type(p.getType())
                .ficheInstruction(p.getFicheInstruction())
                .ficheVersion(p.getFicheVersion())
                .operateur(p.getOperateur())
                .statut(p.getStatut())
                .trg(p.getTrg())
                .trs(p.getTrs())
                .fpy(p.getFpy())
                .build();
    }
}
