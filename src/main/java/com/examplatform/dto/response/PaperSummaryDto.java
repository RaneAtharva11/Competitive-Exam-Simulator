package com.examplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaperSummaryDto {
    private Long paperId;
    private int paperNumber;
    private String subjects;
    private int questionCount;
}