package com.examplatform.dto.request;

import lombok.Data;

@Data
public class SubmitResponseRequest {
    private Long questionId;
    private String selectedOption; // "A","B","C","D" or null if skipping
}