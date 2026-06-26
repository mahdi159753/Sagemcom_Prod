package com.alibou.security.ligneproduction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LigneProductionRepository extends JpaRepository<LigneProduction, Long> {

    List<LigneProduction> findByStatut(LigneStatut statut);

    List<LigneProduction> findByChantier(String chantier);

    Optional<LigneProduction> findByNom(String nom);

    boolean existsByCode(String code);

    List<LigneProduction> findByProduitId(Integer produitId);
}
