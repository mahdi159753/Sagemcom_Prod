package com.alibou.security.poste;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PosteTravailRepository extends JpaRepository<PosteTravail, Long> {

    List<PosteTravail> findByLigne(String ligne);

    List<PosteTravail> findByStatut(PosteStatut statut);

    List<PosteTravail> findByLigneAndStatut(String ligne, PosteStatut statut);

    List<PosteTravail> findByType(PosteType type);

    Optional<PosteTravail> findByCode(String code);

    boolean existsByCode(String code);
}

