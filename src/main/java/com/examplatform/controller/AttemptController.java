package com.examplatform.controller;

import com.examplatform.Service.AttemptService;
import com.examplatform.config.CustomUserDetails;
import com.examplatform.dto.request.SubmitResponseRequest;
import com.examplatform.dto.response.AttemptStatusDto;
import com.examplatform.dto.response.ResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attempts")
@RequiredArgsConstructor
public class AttemptController {

    private final AttemptService attemptService;

    @PostMapping("/start")
    public ResponseEntity<AttemptStatusDto> startAttempt(
            @RequestParam Long examId,                      // LINE 1
            @AuthenticationPrincipal UserDetails userDetails) {  // LINE 2
        Long studentId = extractId(userDetails);           // LINE 3
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attemptService.startAttempt(studentId, examId));
    }

    @GetMapping("/{attemptId}/status")
    public ResponseEntity<AttemptStatusDto> getStatus(
            @PathVariable Long attemptId,                  // LINE 4
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                attemptService.getAttemptStatus(attemptId, extractId(userDetails))
        );
    }

    @PutMapping("/{attemptId}/response")                   // LINE 5
    public ResponseEntity<Void> saveResponse(
            @PathVariable Long attemptId,
            @RequestBody SubmitResponseRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        attemptService.saveResponse(attemptId, request, extractId(userDetails));
        return ResponseEntity.ok().build();                // LINE 6
    }

    @PostMapping("/{attemptId}/submit")
    public ResponseEntity<ResultDto> submitAttempt(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                attemptService.submitAttempt(attemptId, extractId(userDetails))
        );
    }

    private Long extractId(UserDetails userDetails) {      // LINE 7
        return ((CustomUserDetails) userDetails).getId();
    }
}
