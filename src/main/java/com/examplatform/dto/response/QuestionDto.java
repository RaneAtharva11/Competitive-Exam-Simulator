package com.examplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuestionDto {
    private Long questionId;
    private int questionOrder;
    private String subject;
    private String chapter;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    // No correctOption field here — security!
}