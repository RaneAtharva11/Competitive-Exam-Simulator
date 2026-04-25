package com.examplatform.Service;



import com.examplatform.repository.AiExplanationRepository;
import com.examplatform.repository.StudentResponseRepository;
import lombok.RequiredArgsConstructor;
import com.examplatform.model.AiExplanation;
import com.examplatform.model.Question;
import com.examplatform.model.StudentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiExplanationService {

    private final AiExplanationRepository explanationRepository;
    private final RestTemplate restTemplate;
    private final StudentResponseRepository responseRepository;

    @Value("${gemini.api.key}")
    private String apiKey;

    // Gemini Flash endpoint — key is passed as query param, not header
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.5-flash:generateContent?key=";

    // ── Public methods ───────────────────────────────────────────────────────────

    public boolean explanationExists(Long responseId) {
        return explanationRepository.existsByStudentResponseId(responseId);
    }
    @Async
    public void triggerExplanationGeneration(Long attemptId) {
        System.out.println("🤖 Starting AI explanations for attemptId: " + attemptId);

        List<StudentResponse> toExplain = new ArrayList<>();
        toExplain.addAll(responseRepository.findByAttemptIdAndIsCorrectFalse(attemptId));
        toExplain.addAll(responseRepository.findByAttemptIdAndIsUnattemptedTrue(attemptId));

        System.out.println("📝 " + toExplain.size() + " responses need explanation");

        for (int i = 0; i < toExplain.size(); i++) {
            StudentResponse response = toExplain.get(i);

            if (explanationExists(response.getId())) {
                continue; // already done, skip
            }

            // ── Rate limit: wait 5 seconds between each call ─────────────────────
            // gemini-2.0-flash free tier = 15 RPM = one call every 4 seconds
            // We use 5s to stay safely under the limit
            if (i > 0) {
                sleepSeconds(5);
            }

            generateAndSave(response);
        }

        System.out.println("✅ All explanations done for attemptId: " + attemptId);
    }

    public void generateAndSave(StudentResponse response) {
        int maxRetries = 3;
        int attempt    = 0;

        while (attempt < maxRetries) {
            try {
                String prompt = buildPrompt(response.getQuestion(), response);
                String explanationText = callGeminiApi(prompt);

                AiExplanation explanation = new AiExplanation();
                explanation.setStudentResponse(response);
                explanation.setExplanationText(explanationText);
                explanation.setGenerated(true);
                explanation.setGeneratedAt(LocalDateTime.now());
                explanationRepository.save(explanation);

                System.out.println("✅ Saved explanation for responseId: "
                        + response.getId());
                return; // success — exit retry loop

            } catch (Exception e) {
                attempt++;
                String msg = e.getMessage() != null ? e.getMessage() : "";

                if (msg.contains("429")) {
                    // Rate limited — wait 65 seconds then retry
                    System.err.println("⚠ Rate limited on responseId "
                            + response.getId()
                            + " (attempt " + attempt + "/" + maxRetries
                            + ") — waiting 65s...");
                    sleepSeconds(65);

                } else if (msg.contains("503")) {
                    // Server overloaded — wait 30 seconds then retry
                    System.err.println("⚠ Gemini overloaded on responseId "
                            + response.getId()
                            + " (attempt " + attempt + "/" + maxRetries
                            + ") — waiting 30s...");
                    sleepSeconds(30);

                } else {
                    // Unknown error — don't retry
                    System.err.println("❌ Failed for responseId "
                            + response.getId() + ": " + msg);
                    return;
                }
            }
        }

        System.err.println("❌ Gave up on responseId " + response.getId()
                + " after " + maxRetries + " attempts");
    }

    public Map<Long, String> getExplanationsForAttempt(Long attemptId) {
        return explanationRepository
                .findByStudentResponse_Attempt_Id(attemptId)
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getStudentResponse().getId(),
                        e -> e.isGenerated()
                                ? e.getExplanationText()
                                : "Explanation not yet available."
                ));
    }

    private void sleepSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException ignored) {}
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private String buildPrompt(Question q, StudentResponse r) {
        if (r.isUnattempted()) {
            return String.format(
                    "A student skipped this exam question. Explain the correct answer " +
                            "clearly in 4-5 lines suitable for a competitive exam student.\n\n" +
                            "Question: %s\n" +
                            "Option A: %s\n" +
                            "Option B: %s\n" +
                            "Option C: %s\n" +
                            "Option D: %s\n" +
                            "Correct Answer: Option %s\n\n" +
                            "Give a concise, student-friendly explanation of why this is correct.",
                    q.getQuestionText(),
                    q.getOptionA(), q.getOptionB(),
                    q.getOptionC(), q.getOptionD(),
                    q.getCorrectOption()
            );
        } else {
            return String.format(
                    "A student answered this exam question incorrectly.\n\n" +
                            "Question: %s\n" +
                            "Option A: %s\n" +
                            "Option B: %s\n" +
                            "Option C: %s\n" +
                            "Option D: %s\n" +
                            "Student answered: Option %s\n" +
                            "Correct Answer: Option %s\n\n" +
                            "In 4-5 lines, explain why Option %s is correct and " +
                            "why Option %s is wrong. Keep it simple and exam-focused.",
                    q.getQuestionText(),
                    q.getOptionA(), q.getOptionB(),
                    q.getOptionC(), q.getOptionD(),
                    r.getSelectedOption(),
                    q.getCorrectOption(),
                    q.getCorrectOption(),
                    r.getSelectedOption()
            );
        }
    }

    private String callGeminiApi(String prompt) {

        // ── Build the URL with API key as query param ────────────────────────────
        String url = GEMINI_URL + apiKey;

        // ── Headers — Gemini only needs Content-Type ─────────────────────────────
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // ── Request body — Gemini's expected JSON format ─────────────────────────
        /*
          {
            "contents": [
              {
                "parts": [
                  { "text": "your prompt here" }
                ]
              }
            ],
            "generationConfig": {
              "maxOutputTokens": 500,
              "temperature": 0.3
            }
          }
        */
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(textPart));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("maxOutputTokens", 500);
        generationConfig.put("temperature", 0.3);   // low = more factual, less creative

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));
        requestBody.put("generationConfig", generationConfig);

        // ── Make the POST request ─────────────────────────────────────────────────
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        // ── Parse the response ────────────────────────────────────────────────────
        /*
          Gemini response structure:
          {
            "candidates": [
              {
                "content": {
                  "parts": [
                    { "text": "The explanation text here..." }
                  ]
                }
              }
            ]
          }
        */
        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) response.getBody().get("candidates");

        Map<String, Object> firstCandidate = candidates.get(0);

        Map<String, Object> responseContent =
                (Map<String, Object>) firstCandidate.get("content");

        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) responseContent.get("parts");

        return (String) parts.get(0).get("text");
    }
}