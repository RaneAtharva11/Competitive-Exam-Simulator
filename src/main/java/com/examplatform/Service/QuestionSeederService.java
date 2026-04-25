// com/examplatform/service/QuestionSeederService.java
package com.examplatform.Service;

import com.examplatform.config.ExamSeedConfig;
import com.examplatform.config.ExamSeedConfig.*;
import com.examplatform.model.*;
import com.examplatform.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class QuestionSeederService {

    private final ExamRepository       examRepository;
    private final PaperRepository      paperRepository;
    private final QuestionRepository   questionRepository;
    private final RestTemplate         restTemplate;
    private final ObjectMapper         objectMapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.0-flash:generateContent?key=";

    // ── Main seed method ─────────────────────────────────────────────────────

    public void seedAll() {
        for (ExamBlueprint examBp : ExamSeedConfig.EXAMS) {

            // 1. Find or create the Exam row
            Exam exam = examRepository.findByName(examBp.getName())
                    .orElseGet(() -> {
                        Exam e = new Exam();
                        e.setName(examBp.getName());
                        e.setTotalQuestions(examBp.getTotalQuestions());
                        e.setDurationSeconds(examBp.getDurationSeconds());
                        e.setMarksCorrect(examBp.getMarksCorrect());
                        e.setMarksWrong(examBp.getMarksWrong());
                        e.setMarksUnattempted(examBp.getMarksUnattempted());
                        return examRepository.save(e);
                    });

            for (PaperBlueprint paperBp : examBp.getPapers()) {

                // 2. Find or create the Paper row
                String subjectsCombined = paperBp.getSubjects().stream()
                        .map(SubjectBlueprint::getSubjectName)
                        .reduce((a, b) -> a + "," + b)
                        .orElse("");

                int totalQuestionsInPaper = paperBp.getSubjects().stream()
                        .mapToInt(SubjectBlueprint::getTotalQuestions)
                        .sum();

                Paper paper = paperRepository
                        .findByExamIdAndPaperNumber(exam.getId(), paperBp.getPaperNumber())
                        .orElseGet(() -> {
                            Paper p = new Paper();
                            p.setExam(exam);
                            p.setPaperNumber(paperBp.getPaperNumber());
                            p.setSubjects(subjectsCombined);
                            p.setQuestionCount(totalQuestionsInPaper);
                            return paperRepository.save(p);
                        });

                // 3. Skip if questions already exist for this paper
                if (questionRepository.countByPaperId(paper.getId()) > 0) {
                    System.out.println("⏭ Skipping " + exam.getName()
                            + " Paper " + paperBp.getPaperNumber()
                            + " — already seeded");
                    continue;
                }

                // 4. Generate questions per subject via Gemini
                int globalOrder = 1;

                for (SubjectBlueprint subjectBp : paperBp.getSubjects()) {
                    int questionsPerChapter = Math.max(1,
                            subjectBp.getTotalQuestions() / subjectBp.getChapters().size()
                    );

                    for (String chapter : subjectBp.getChapters()) {
                        System.out.println("🤖 Generating: " + exam.getName()
                                + " | " + subjectBp.getSubjectName()
                                + " | " + chapter);

                        List<GeneratedQuestion> questions = generateQuestionsFromGemini(
                                examBp.getName(),
                                subjectBp.getSubjectName(),
                                chapter,
                                questionsPerChapter
                        );

                        // 5. Save each generated question to DB
                        for (GeneratedQuestion gq : questions) {
                            Question q = new Question();
                            q.setPaper(paper);
                            q.setSubject(subjectBp.getSubjectName());
                            q.setChapter(chapter);
                            q.setQuestionText(gq.getQuestionText());
                            q.setOptionA(gq.getOptionA());
                            q.setOptionB(gq.getOptionB());
                            q.setOptionC(gq.getOptionC());
                            q.setOptionD(gq.getOptionD());
                            q.setCorrectOption(gq.getCorrectOption());
                            q.setQuestionOrder(globalOrder++);
                            questionRepository.save(q);
                        }

                        // Small delay to avoid hitting Gemini rate limits
                        // (free tier = 15 requests/min)
                        sleepSeconds(5);
                    }
                }

                System.out.println("✅ Done: " + exam.getName()
                        + " Paper " + paperBp.getPaperNumber()
                        + " | Total questions saved: " + (globalOrder - 1));
            }
        }

        System.out.println("\n🎉 ALL EXAMS SEEDED SUCCESSFULLY!");
    }

    // ── Gemini API call ──────────────────────────────────────────────────────

    private List<GeneratedQuestion> generateQuestionsFromGemini(
            String examName, String subject, String chapter, int count) {

        String prompt = buildPrompt(examName, subject, chapter, count);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(textPart));
        Map<String, Object> generationConfig = Map.of(
                "maxOutputTokens", 4096,
                "temperature", 0.7
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));
        requestBody.put("generationConfig", generationConfig);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    GEMINI_URL + apiKey, entity, Map.class
            );

            // Extract the text response
            List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) response.getBody().get("candidates");
            Map<String, Object> responseContent =
                    (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts =
                    (List<Map<String, Object>>) responseContent.get("parts");
            String rawJson = (String) parts.get(0).get("text");

            return parseQuestionsFromJson(rawJson);

        } catch (Exception e) {
            System.err.println("❌ Gemini failed for " + subject
                    + " - " + chapter + ": " + e.getMessage());
            return List.of(); // empty list — seeder continues with next chapter
        }
    }

    // ── Build the Gemini prompt ──────────────────────────────────────────────

    private String buildPrompt(String exam, String subject,
                               String chapter, int count) {
        return String.format("""
            Generate exactly %d multiple choice questions for the %s competitive exam.
            Subject: %s
            Chapter: %s

            Rules:
            - Questions must be at the difficulty level of the actual %s exam
            - Each question must have exactly 4 options (A, B, C, D)
            - Only ONE option must be correct
            - Correct option must be randomly distributed (not always A or B)
            - No question should be repeated
            - Questions should test conceptual understanding, not just memorization

            You MUST respond with ONLY a valid JSON array. No explanation before or after.
            No markdown code blocks. Just raw JSON.

            Format:
            [
              {
                "questionText": "full question text here",
                "optionA": "first option",
                "optionB": "second option",
                "optionC": "third option",
                "optionD": "fourth option",
                "correctOption": "A"
              }
            ]

            Generate exactly %d questions now:
            """,
                count, exam, subject, chapter, exam, count
        );
    }

    // ── Parse Gemini's JSON response ─────────────────────────────────────────

    private List<GeneratedQuestion> parseQuestionsFromJson(String rawJson) {
        List<GeneratedQuestion> questions = new ArrayList<>();

        try {
            // Gemini sometimes wraps JSON in markdown — strip it if present
            String cleaned = rawJson
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            // Find the JSON array start and end
            int start = cleaned.indexOf('[');
            int end   = cleaned.lastIndexOf(']');
            if (start == -1 || end == -1) {
                System.err.println("❌ No JSON array found in response");
                return questions;
            }
            cleaned = cleaned.substring(start, end + 1);

            JsonNode array = objectMapper.readTree(cleaned);

            for (JsonNode node : array) {
                try {
                    GeneratedQuestion gq = new GeneratedQuestion();
                    gq.setQuestionText(node.get("questionText").asText());
                    gq.setOptionA(node.get("optionA").asText());
                    gq.setOptionB(node.get("optionB").asText());
                    gq.setOptionC(node.get("optionC").asText());
                    gq.setOptionD(node.get("optionD").asText());

                    // Normalize correct option to uppercase A/B/C/D
                    String correct = node.get("correctOption").asText()
                            .trim().toUpperCase();
                    // Validate it's one of A/B/C/D
                    if (correct.matches("[ABCD]")) {
                        gq.setCorrectOption(correct);
                        questions.add(gq);
                    } else {
                        System.err.println("⚠ Invalid correctOption: " + correct
                                + " — skipping question");
                    }
                } catch (Exception e) {
                    System.err.println("⚠ Skipping malformed question: "
                            + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("❌ JSON parse failed: " + e.getMessage());
            System.err.println("Raw response was: " + rawJson.substring(0,
                    Math.min(300, rawJson.length())));
        }

        return questions;
    }

    // ── Inner DTO for parsed questions ───────────────────────────────────────

    @lombok.Data
    private static class GeneratedQuestion {
        private String questionText;
        private String optionA;
        private String optionB;
        private String optionC;
        private String optionD;
        private String correctOption;
    }

    private void sleepSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException ignored) {}
    }
}
