package com.examplatform.repository;

import com.examplatform.model.AiExplanation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiExplanationRepository extends JpaRepository<AiExplanation, Long> {
    Optional<AiExplanation> findByStudentResponseId(Long responseId);
    List<AiExplanation> findByStudentResponse_Attempt_Id(Long attemptId);
    boolean existsByStudentResponseId(Long responseId);
}