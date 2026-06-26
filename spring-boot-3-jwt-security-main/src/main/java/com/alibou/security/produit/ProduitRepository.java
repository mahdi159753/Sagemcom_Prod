package com.alibou.security.produit;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProduitRepository extends JpaRepository<Produit, Integer> {
    Optional<Produit> findByNameAndReference(String name, String reference);
}
