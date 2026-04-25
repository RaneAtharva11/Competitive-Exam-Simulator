package com.examplatform.repository;

import com.examplatform.model.BatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatchResultRepository extends JpaRepository<BatchResult, Long> {
    Optional<BatchResult> findByAttemptId(Long attemptId);
    List<BatchResult> findByBatchIdOrderByScoreDesc(Long batchId);
}