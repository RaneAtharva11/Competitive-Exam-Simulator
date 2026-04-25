package com.examplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "exams")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public int getMarksCorrect() {
        return marksCorrect;
    }

    public void setMarksCorrect(int marksCorrect) {
        this.marksCorrect = marksCorrect;
    }

    public int getMarksWrong() {
        return marksWrong;
    }

    public void setMarksWrong(int marksWrong) {
        this.marksWrong = marksWrong;
    }

    public int getMarksUnattempted() {
        return marksUnattempted;
    }

    public void setMarksUnattempted(int marksUnattempted) {
        this.marksUnattempted = marksUnattempted;
    }

    public List<Paper> getPapers() {
        return papers;
    }

    public void setPapers(List<Paper> papers) {
        this.papers = papers;
    }

    // e.g. "JEE", "MHT-CET", "BITSAT", "VIT"
    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    // Duration in seconds. JEE = 10800 (3hrs), MHT-CET = 5400 (90min)
    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    // Marking scheme fields
    @Column(name = "marks_correct", nullable = false)
    private int marksCorrect;         // e.g. +4 for JEE

    @Column(name = "marks_wrong", nullable = false)
    private int marksWrong;           // e.g. -1 for JEE (store as negative)

    @Column(name = "marks_unattempted", nullable = false)
    private int marksUnattempted;     // always 0

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Paper> papers;
}