package com.examplatform.controller;

import com.examplatform.Service.ExamService;
import com.examplatform.dto.response.ExamSummaryDto;
import com.examplatform.dto.response.QuestionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    // GET /api/exams → Dashboard: shows JEE, BITSAT, MHT-CET, VIT cards
    @GetMapping
    public ResponseEntity<List<ExamSummaryDto>> getAllExams() {
        return ResponseEntity.ok(examService.getAllExams());
    }

    // GET /api/exams/{examId}/papers/{paperId}/questions → Loads questions for Paper 1 or Paper 2
    @GetMapping("/{examId}/papers/{paperId}/questions")
    public ResponseEntity<List<QuestionDto>> getQuestions(
            @PathVariable Long examId,
            @PathVariable Long paperId) {
        return ResponseEntity.ok(examService.getQuestionsForPaper(paperId));
    }
}