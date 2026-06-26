package com.alibou.security.poste;

import lombok.*;

/** Full response sent to Angular — includes the database id */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosteTravailResponse {
    private Long        id;
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

    public static PosteTravailResponse from(PosteTravail p) {
        return PosteTravailResponse.builder()
                .id(p.getId())
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

