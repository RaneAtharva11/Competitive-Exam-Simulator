package com.examplatform.repository;

import com.examplatform.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByPaperIdOrderByQuestionOrder(Long paperId);
    List<Question> findByPaperIdAndSubject(Long paperId, String subject);
    List<Question> findByPaper_Exam_Id(Long examId); // all questions for an exam


    int countByPaperId(Long paperId);
}