package com.alibou.security.nc;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NonConformityRepository extends JpaRepository<NonConformity, Integer> {
    
    List<NonConformity> findByStatut(String statut);
}
