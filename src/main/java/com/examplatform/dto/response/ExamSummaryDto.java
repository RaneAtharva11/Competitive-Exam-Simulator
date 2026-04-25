package com.examplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ExamSummaryDto {
    private Long examId;
    private String name;
    private int totalQuestions;
    private int durationMinutes;
    private String markingScheme; // e.g. "+4 / -1 / 0"
    private List<PaperSummaryDto> papers;
}
