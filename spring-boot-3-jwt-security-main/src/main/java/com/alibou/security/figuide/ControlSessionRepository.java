package com.alibou.security.figuide;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ControlSessionRepository extends JpaRepository<ControlSession, Integer> {
    List<ControlSession> findByOperatorIdOrderByStartedAtDesc(Integer operatorId);
    List<ControlSession> findAllByOrderByStartedAtDesc();
    List<ControlSession> findByProduitId(Integer produitId);
}
