package com.alibou.security.poste;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PosteTravailService {

    private final PosteTravailRepository repository;

    // ── GET ALL ──────────────────────────────────────────────────────────────
    public List<PosteTravailResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(PosteTravailResponse::from)
                .collect(Collectors.toList());
    }

    // ── GET BY ID ────────────────────────────────────────────────────────────
    public PosteTravailResponse findById(Long id) {
        PosteTravail poste = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Poste not found with id: " + id));
        return PosteTravailResponse.from(poste);
    }

    // ── GET BY LIGNE ─────────────────────────────────────────────────────────
    public List<PosteTravailResponse> findByLigne(String ligne) {
        return repository.findByLigne(ligne)
                .stream()
                .map(PosteTravailResponse::from)
                .collect(Collectors.toList());
    }

    // ── GET BY STATUT ────────────────────────────────────────────────────────
    public List<PosteTravailResponse> findByStatut(PosteStatut statut) {
        return repository.findByStatut(statut)
                .stream()
                .map(PosteTravailResponse::from)
                .collect(Collectors.toList());
    }

    // ── FILTER BY LIGNE + STATUT ─────────────────────────────────────────────
    public List<PosteTravailResponse> findByLigneAndStatut(String ligne, PosteStatut statut) {
        return repository.findByLigneAndStatut(ligne, statut)
                .stream()
                .map(PosteTravailResponse::from)
                .collect(Collectors.toList());
    }

    // ── CREATE ───────────────────────────────────────────────────────────────
    public PosteTravailResponse create(PosteTravailDTO dto) {
        if (repository.existsByCode(dto.getCode())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Poste with code '" + dto.getCode() + "' already exists");
        }

        PosteTravail poste = PosteTravail.builder()
                .code(dto.getCode())
                .libelle(dto.getLibelle())
                .ligne(dto.getLigne())
                .type(dto.getType())
                .ficheInstruction(dto.getFicheInstruction())
                .ficheVersion(dto.getFicheVersion())
                .operateur(dto.getOperateur() != null ? dto.getOperateur() : "")
                .statut(dto.getStatut() != null ? dto.getStatut() : PosteStatut.ACTIF)
                .trg(dto.getTrg() != null ? dto.getTrg() : 0.0)
                .trs(dto.getTrs() != null ? dto.getTrs() : 0.0)
                .fpy(dto.getFpy() != null ? dto.getFpy() : 0.0)
                .build();

        return PosteTravailResponse.from(repository.save(poste));
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────
    public PosteTravailResponse update(Long id, PosteTravailDTO dto) {
        PosteTravail existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Poste not found with id: " + id));

        // Check code uniqueness only if it changed
        if (!existing.getCode().equals(dto.getCode()) && repository.existsByCode(dto.getCode())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Poste with code '" + dto.getCode() + "' already exists");
        }

        existing.setCode(dto.getCode());
        existing.setLibelle(dto.getLibelle());
        existing.setLigne(dto.getLigne());
        existing.setType(dto.getType());
        existing.setFicheInstruction(dto.getFicheInstruction());
        existing.setFicheVersion(dto.getFicheVersion());
        existing.setOperateur(dto.getOperateur());
        if (dto.getStatut() != null) existing.setStatut(dto.getStatut());
        if (dto.getTrg() != null)    existing.setTrg(dto.getTrg());
        if (dto.getTrs() != null)    existing.setTrs(dto.getTrs());
        if (dto.getFpy() != null)    existing.setFpy(dto.getFpy());

        return PosteTravailResponse.from(repository.save(existing));
    }

    // ── TOGGLE STATUT (Actif ↔ Inactif) ─────────────────────────────────────
    public PosteTravailResponse toggleStatut(Long id) {
        PosteTravail poste = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Poste not found with id: " + id));

        poste.setStatut(poste.getStatut() == PosteStatut.ACTIF
                ? PosteStatut.INACTIF : PosteStatut.ACTIF);

        return PosteTravailResponse.from(repository.save(poste));
    }

    // ── UPDATE KPI (called by PREPARATEUR when saisir indicateurs) ───────────
    public PosteTravailResponse updateKpi(Long id, Double trg, Double trs, Double fpy) {
        PosteTravail poste = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Poste not found with id: " + id));

        if (trg != null) poste.setTrg(trg);
        if (trs != null) poste.setTrs(trs);
        if (fpy != null) poste.setFpy(fpy);

        return PosteTravailResponse.from(repository.save(poste));
    }

    // ── DELETE ───────────────────────────────────────────────────────────────
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Poste not found with id: " + id);
        }
        repository.deleteById(id);
    }
}

