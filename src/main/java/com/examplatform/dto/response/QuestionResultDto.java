package com.examplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuestionResultDto {
    private Long questionId;
    private Long responseId;
    private String questionText;
    private String subject;
    private String chapter;
    private String optionA, optionB, optionC, optionD;
    private String selectedOption;   // what student picked
    private String correctOption;    // now revealed after exam
    private Boolean isCorrect;
    private boolean isUnattempted;
    private String aiExplanation;    // null if not yet generated
}