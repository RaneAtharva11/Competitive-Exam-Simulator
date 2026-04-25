package com.examplatform.repository;

import com.examplatform.enums.BatchStatus;
import com.examplatform.model.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {
    // Find the open batch for an exam on today's date with available slots
    Optional<Batch> findFirstByExamIdAndStatusAndExamDateAndCurrentCountLessThan(
            Long examId, BatchStatus status, LocalDate date, int maxSize
    );

    // Find all closed batches for percentile jobs
    List<Batch> findByStatus(BatchStatus status);
}