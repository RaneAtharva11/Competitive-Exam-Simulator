package com.examplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_responses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    // NULL means unattempted (student skipped)
    @Column(name = "selected_option", length = 1)
    private String selectedOption;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    public boolean isUnattempted() {
        return isUnattempted;
    }


    @Column(name = "is_unattempted")
    private boolean isUnattempted = true;

    @OneToOne(mappedBy = "studentResponse", cascade = CascadeType.ALL)
    @JsonIgnore
    private AiExplanation aiExplanation;


    public void setIsUnattempted(boolean b) {
        isUnattempted=b;
    }
}