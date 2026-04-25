package com.examplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ResultDto {
    private Long attemptId;
    private String examName;
    private int score;
    private int totalMarks;
    private int correctCount;
    private int wrongCount;
    private int unattemptedCount;
    private Double percentile;       // null until batch closes
    private boolean percentileReady;
    private List<QuestionResultDto> questionResults;
}