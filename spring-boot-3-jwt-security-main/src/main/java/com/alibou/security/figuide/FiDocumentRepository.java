package com.alibou.security.figuide;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FiDocumentRepository extends JpaRepository<FiDocument, Integer> {
    Optional<FiDocument> findByProduitIdAndActiveTrue(Integer produitId);
    List<FiDocument> findByProduitIdOrderByUploadedAtDesc(Integer produitId);
    Optional<FiDocument> findByProduitIdAndVersion(Integer produitId, String version);
}
