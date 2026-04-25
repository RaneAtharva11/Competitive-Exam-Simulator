package com.examplatform.repository;

import com.examplatform.model.StudentResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentResponseRepository extends JpaRepository<StudentResponse, Long> {
    List<StudentResponse> findByAttemptId(Long attemptId);

    // All wrong answers for AI explanation generation

     List<StudentResponse> findByAttemptIdAndIsCorrectFalse(Long attemptId);

    // All unattempted for AI explanation generation

     List<StudentResponse> findByAttemptIdAndIsUnattemptedTrue(Long attemptId);


    Optional<StudentResponse> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);

    int countByAttemptIdAndIsCorrectTrue(Long attemptId);
    int countByAttemptIdAndIsCorrectFalse(Long attemptId);
    int countByAttemptIdAndIsUnattemptedTrue(Long attemptId);
}