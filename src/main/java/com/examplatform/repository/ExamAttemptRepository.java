package com.examplatform.repository;

import com.examplatform.enums.AttemptStatus;
import com.examplatform.model.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
    Optional<ExamAttempt> findByStudentIdAndExamIdAndStatus(
            Long studentId, Long examId, AttemptStatus status
    );
    boolean existsByStudentIdAndExamId(Long studentId, Long examId);
    List<ExamAttempt> findByBatchIdAndStatus(Long batchId, AttemptStatus status);
    List<ExamAttempt> findByStudentId(Long studentId); // for student history
}