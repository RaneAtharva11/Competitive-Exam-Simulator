package com.examplatform.repository;

import com.examplatform.model.Paper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaperRepository extends JpaRepository<Paper, Long> {
    List<Paper> findByExamIdOrderByPaperNumber(Long examId);


    Optional<Paper> findByExamIdAndPaperNumber(Long examId, int paperNumber);
}