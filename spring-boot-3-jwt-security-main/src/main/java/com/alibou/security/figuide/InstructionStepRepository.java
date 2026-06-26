package com.alibou.security.figuide;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstructionStepRepository extends JpaRepository<InstructionStep, Integer> {
    List<InstructionStep> findByFiDocumentIdOrderBySequenceOrderAsc(Integer fiDocumentId);
}
