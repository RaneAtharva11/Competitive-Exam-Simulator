package com.examplatform.controller;

import com.examplatform.exception.CustomExceptions;
import com.examplatform.repository.ExamAttemptRepository;
import com.examplatform.repository.StudentResponseRepository;
import com.examplatform.Service.AiExplanationService;
import com.examplatform.Service.ResultService;
import com.examplatform.dto.response.ResultDto;

import lombok.RequiredArgsConstructor;
import com.examplatform.model.ExamAttempt;
import com.examplatform.model.StudentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;
    private final AiExplanationService aiExplanationService;
    private final ExamAttemptRepository attemptRepository;
    private final StudentResponseRepository responseRepository;

    // GET /api/results/{attemptId} → Result page (Total marks, percentile)
    @GetMapping("/{attemptId}")
    public ResponseEntity<ResultDto> getResult(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetails userDetails) {
        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException("Attempt not found"));
        List<StudentResponse> responses = responseRepository.findByAttemptId(attemptId);
        return ResponseEntity.ok(resultService.buildResult(attempt, responses));
    }

    // GET /api/results/{attemptId}/explanations → AI explanation page
    // Frontend calls this after result page to load AI explanations
    @GetMapping("/{attemptId}/explanations")
    public ResponseEntity<Map<Long, String>> getExplanations(@PathVariable Long attemptId) {
        return ResponseEntity.ok(aiExplanationService.getExplanationsForAttempt(attemptId));
    }
}