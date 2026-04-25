package com.examplatform.dto.response;

import com.examplatform.enums.AttemptStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AttemptStatusDto {
    private Long attemptId;
    private Long examId;
    private String examName;
    private LocalDateTime startTime;
    private int durationSeconds;
    private int remainingSeconds;    // server calculates this
    private AttemptStatus status;
    private int answeredCount;
    private int totalQuestions;
}