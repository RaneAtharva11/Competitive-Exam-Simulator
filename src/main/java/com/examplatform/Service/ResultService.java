package com.examplatform.Service;

import com.examplatform.repository.BatchResultRepository;
import com.examplatform.repository.ExamAttemptRepository;
import com.examplatform.repository.StudentResponseRepository;
import com.examplatform.dto.response.QuestionResultDto;
import com.examplatform.dto.response.ResultDto;
import lombok.RequiredArgsConstructor;
import com.examplatform.model.BatchResult;
import com.examplatform.model.Exam;
import com.examplatform.model.ExamAttempt;
import com.examplatform.model.StudentResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResultService {

    private final BatchResultRepository batchResultRepository;
    private final AiExplanationService aiExplanationService;
    private final ExamAttemptRepository attemptRepository;
    private final StudentResponseRepository responseRepository;

    public ResultDto buildResult(ExamAttempt attempt, List<StudentResponse> responses) {
        Exam exam = attempt.getExam();
        int totalMarks = exam.getTotalQuestions() * exam.getMarksCorrect();

        int correct = 0, wrong = 0, unattempted = 0;
        List<QuestionResultDto> questionResults = new ArrayList<>();

        for (StudentResponse r : responses) {
            if (r.isUnattempted()) unattempted++;
            else if (Boolean.TRUE.equals(r.getIsCorrect())) correct++;
            else wrong++;

            // Reveal correct option in result view
            questionResults.add(new QuestionResultDto(
                    r.getQuestion().getId(), r.getId(),
                    r.getQuestion().getQuestionText(),
                    r.getQuestion().getSubject(),
                    r.getQuestion().getChapter(),
                    r.getQuestion().getOptionA(), r.getQuestion().getOptionB(),
                    r.getQuestion().getOptionC(), r.getQuestion().getOptionD(),
                    r.getSelectedOption(),
                    r.getQuestion().getCorrectOption(),
                    r.getIsCorrect(),
                    r.isUnattempted(),
                    null  // AI explanation fetched separately or via async
            ));
        }

        Optional<BatchResult> batchResult = batchResultRepository.findByAttemptId(attempt.getId());
        Double percentile = batchResult.map(BatchResult::getPercentile).orElse(null);

        return new ResultDto(
                attempt.getId(), exam.getName(),
                attempt.getScore(), totalMarks,
                correct, wrong, unattempted,
                percentile, batchResult.isPresent(),
                questionResults
        );
    }

    // Called asynchronously after submission — generates AI explanations in background
    @Async
    public void generateAiExplanationsAsync(Long attemptId) {
        List<StudentResponse> toExplain = responseRepository
                .findByAttemptIdAndIsCorrectFalse(attemptId);
        toExplain.addAll(responseRepository.findByAttemptIdAndIsUnattemptedTrue(attemptId));

        for (StudentResponse response : toExplain) {
            if (!aiExplanationService.explanationExists(response.getId())) {
                aiExplanationService.generateAndSave(response);
            }
        }
    }

    // Scheduled job: runs at 11:30 PM every day to close full or day-end batches
    @Scheduled(cron = "0 30 23 * * *")
    public void computeDailyPercentiles() {
        // Logic: for each OPEN batch that is either full or it's end of day,
        // mark COMPUTING, calculate percentiles, mark CLOSED
        // Implementation: fetch all OPEN batches, sort by score, compute percentile
    }
}