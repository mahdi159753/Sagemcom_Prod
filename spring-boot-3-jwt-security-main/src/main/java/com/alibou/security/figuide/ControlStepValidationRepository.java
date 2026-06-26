package com.alibou.security.figuide;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ControlStepValidationRepository extends JpaRepository<ControlStepValidation, Integer> {
    List<ControlStepValidation> findByControlSessionIdOrderByValidatedAtAsc(Integer controlSessionId);
    Optional<ControlStepValidation> findByControlSessionIdAndInstructionStepId(Integer controlSessionId, Integer instructionStepId);
}
