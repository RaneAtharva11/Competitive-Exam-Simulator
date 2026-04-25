package com.examplatform.Service;

import com.examplatform.repository.ExamRepository;
import com.examplatform.repository.PaperRepository;
import com.examplatform.repository.QuestionRepository;
import com.examplatform.dto.response.ExamSummaryDto;
import com.examplatform.dto.response.PaperSummaryDto;
import com.examplatform.dto.response.QuestionDto;
import lombok.RequiredArgsConstructor;
import com.examplatform.model.Paper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final PaperRepository paperRepository;
    private final QuestionRepository questionRepository;

    // Called on dashboard: returns all available exams
    public List<ExamSummaryDto> getAllExams() {
        return examRepository.findAll().stream().map(exam -> {
            List<Paper> papers = paperRepository.findByExamIdOrderByPaperNumber(exam.getId());
            List<PaperSummaryDto> paperDtos = papers.stream()
                    .map(p -> new PaperSummaryDto(p.getId(), p.getPaperNumber(),
                            p.getSubjects(), p.getQuestionCount()))
                    .collect(Collectors.toList());

            String markingScheme = String.format("+%d / %d / %d",
                    exam.getMarksCorrect(), exam.getMarksWrong(), exam.getMarksUnattempted());

            return new ExamSummaryDto(
                    exam.getId(), exam.getName(),
                    exam.getTotalQuestions(),
                    exam.getDurationSeconds() / 60,
                    markingScheme, paperDtos
            );
        }).collect(Collectors.toList());
    }

    // Returns questions for a specific paper — correctOption is excluded
    public List<QuestionDto> getQuestionsForPaper(Long paperId) {
        return questionRepository.findByPaperIdOrderByQuestionOrder(paperId)
                .stream()
                .map(q -> new QuestionDto(
                        q.getId(), q.getQuestionOrder(), q.getSubject(), q.getChapter(),
                        q.getQuestionText(), q.getOptionA(), q.getOptionB(),
                        q.getOptionC(), q.getOptionD()
                        // correctOption intentionally omitted
                ))
                .collect(Collectors.toList());
    }
}