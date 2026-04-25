package com.examplatform.Service;

import com.examplatform.exception.CustomExceptions;
import com.examplatform.model.*;
import com.examplatform.repository.*;
import com.examplatform.dto.request.SubmitResponseRequest;
import com.examplatform.dto.response.AttemptStatusDto;
import com.examplatform.dto.response.ResultDto;
import com.examplatform.enums.AttemptStatus;
import com.examplatform.enums.BatchStatus;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttemptService {

    private final ExamAttemptRepository attemptRepository;
    private final StudentResponseRepository responseRepository;
    private final BatchRepository batchRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final ResultService resultService;

    @Transactional
    public AttemptStatusDto startAttempt(Long studentId, Long examId) {
        // Prevent duplicate active attempt
        if (attemptRepository.existsByStudentIdAndExamId(studentId, examId)) {
            throw new CustomExceptions.AttemptAlreadyExistsException("You have already attempted this exam");
        }

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException("Exam not found"));

        // Find or create an open batch for today
        Batch batch = findOrCreateBatch(exam);

        ExamAttempt attempt = new ExamAttempt();
        attempt.setStudent(new User(studentId));  // proxy reference
        attempt.setExam(exam);
        attempt.setBatch(batch);
        attempt.setStartTime(LocalDateTime.now());
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attemptRepository.save(attempt);

        // Increment batch count
        batch.setCurrentCount(batch.getCurrentCount() + 1);
        batchRepository.save(batch);

        // Pre-create unattempted response rows for all questions
        List<Question> questions = questionRepository.findByPaper_Exam_Id(examId);
        List<StudentResponse> blankResponses = questions.stream().map(q -> {
            StudentResponse sr = new StudentResponse();
            sr.setAttempt(attempt);
            sr.setQuestion(q);
            sr.setSelectedOption(null);
            sr.setIsCorrect(null);
            sr.setIsUnattempted(true);
            return sr;
        }).collect(Collectors.toList());
        responseRepository.saveAll(blankResponses);

        int remaining = exam.getDurationSeconds();
        return new AttemptStatusDto(attempt.getId(), examId, exam.getName(),
                attempt.getStartTime(), exam.getDurationSeconds(), remaining,
                AttemptStatus.IN_PROGRESS, 0, exam.getTotalQuestions());
    }

    // Called each time student clicks an option or skips
    @Transactional
    public void saveResponse(Long attemptId, SubmitResponseRequest request, Long studentId) {
        ExamAttempt attempt = getValidAttempt(attemptId, studentId);

        StudentResponse response = responseRepository
                .findByAttemptIdAndQuestionId(attemptId, request.getQuestionId())
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException("Response row not found"));

        String selected = request.getSelectedOption();
        response.setSelectedOption(selected);
        response.setIsUnattempted(selected == null);

        if (selected != null) {
            boolean correct = selected.equalsIgnoreCase(response.getQuestion().getCorrectOption());
            response.setIsCorrect(correct);
        } else {
            response.setIsCorrect(null);
        }
        responseRepository.save(response);
    }

    // Called when student clicks final Submit or timer expires
    @Transactional
    public ResultDto submitAttempt(Long attemptId, Long studentId) {
        ExamAttempt attempt = getValidAttempt(attemptId, studentId);
        attempt.setSubmitTime(LocalDateTime.now());
        attempt.setStatus(AttemptStatus.SUBMITTED);

        // Calculate score using marking scheme
        Exam exam = attempt.getExam();
        List<StudentResponse> responses = responseRepository.findByAttemptId(attemptId);

        int score = 0;
        for (StudentResponse r : responses) {
            if (r.isUnattempted()) {
                score += exam.getMarksUnattempted();
            } else if (Boolean.TRUE.equals(r.getIsCorrect())) {
                score += exam.getMarksCorrect();
            } else {
                score += exam.getMarksWrong(); // already negative in DB
            }
        }
        attempt.setScore(score);
        attemptRepository.save(attempt);

        // Trigger async AI explanation generation
        resultService.generateAiExplanationsAsync(attemptId);

        return resultService.buildResult(attempt, responses);
    }

    // Real-time timer status for the frontend to poll
    public AttemptStatusDto getAttemptStatus(Long attemptId, Long studentId) {
        ExamAttempt attempt = getValidAttempt(attemptId, studentId);
        Exam exam = attempt.getExam();
        long elapsed = Duration.between(attempt.getStartTime(), LocalDateTime.now()).getSeconds();
        int remaining = Math.max(0, exam.getDurationSeconds() - (int) elapsed);

        // Auto-submit if timer has expired
        if (remaining == 0 && attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            attempt.setStatus(AttemptStatus.TIMED_OUT);
            attempt.setSubmitTime(LocalDateTime.now());
            attemptRepository.save(attempt);
        }

        int answered = responseRepository.countByAttemptIdAndIsCorrectTrue(attemptId)
                + responseRepository.countByAttemptIdAndIsCorrectFalse(attemptId);

        return new AttemptStatusDto(attempt.getId(), exam.getId(), exam.getName(),
                attempt.getStartTime(), exam.getDurationSeconds(), remaining,
                attempt.getStatus(), answered, exam.getTotalQuestions());
    }

    // --- Private helpers ---

    private Batch findOrCreateBatch(Exam exam) {
        return batchRepository.findFirstByExamIdAndStatusAndExamDateAndCurrentCountLessThan(
                exam.getId(), BatchStatus.OPEN, LocalDate.now(), 150
        ).orElseGet(() -> {
            Batch b = new Batch();
            b.setExam(exam);
            b.setExamDate(LocalDate.now());
            b.setMaxSize(150);
            b.setCurrentCount(0);
            b.setStatus(BatchStatus.OPEN);
            return batchRepository.save(b);
        });
    }

    private ExamAttempt getValidAttempt(Long attemptId, Long studentId) {
        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new CustomExceptions.ResourceNotFoundException("Attempt not found"));
        if (!attempt.getStudent().getId().equals(studentId)) {
            throw new CustomExceptions.UnauthorizedException("Not your attempt");
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new CustomExceptions.AttemptAlreadySubmittedException("Attempt already submitted");
        }
        return attempt;
    }
}