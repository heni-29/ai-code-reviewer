package com.heni.codereviewer.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heni.codereviewer.dto.AIReviewResponse;
import com.heni.codereviewer.dto.ChangedFile;

@Service
public class AIReviewService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AIReviewService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }

    public AIReviewResponse review(ChangedFile file) {

        String prompt = """
                You are an expert senior software engineer reviewing a GitHub pull request.

                Analyze ONLY the code changes in the provided patch.

                Report an issue ONLY when there is a concrete problem introduced by the change.

                Look for:
                - BUG
                - SECURITY
                - PERFORMANCE
                - DUPLICATION
                - TEST
                - API_BREAKING_CHANGE

                Severity must be one of:
                CRITICAL, HIGH, MEDIUM, LOW

                Do NOT report:
                - stylistic preferences
                - generic best practices
                - hypothetical issues without evidence
                - unrelated code
                - logging suggestions
                - documentation suggestions

                Return ONLY valid JSON in exactly this format:

                {
                  "issues": [
                    {
                      "category": "BUG",
                      "severity": "HIGH",
                      "file": "Example.java",
                      "line": 10,
                      "message": "Description of the concrete problem",
                      "suggestion": "Specific suggested fix",
                      "confidence": 0.95
                    }
                  ]
                }

                If there are no concrete issues, return:

                {
                  "issues": []
                }

                File:
                %s

                Patch:
                %s
                """.formatted(
                file.getFilename(),
                file.getPatch()
        );

        String response = restClient.post()
                .uri("/api/generate")
                .body(new OllamaRequest(
                        "qwen2.5-coder:7b",
                        prompt,
                        false
                ))
                .retrieve()
                .body(OllamaResponse.class)
                .response();
                

        try {
                String cleanedResponse = response.replace("```json", "").replace("```", "").trim();
                return objectMapper.readValue(cleanedResponse, AIReviewResponse.class);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to parse Ollama response: " + response,
                    e
            );
        }
    }

    private record OllamaRequest(
            String model,
            String prompt,
            boolean stream
    ) {}

    private record OllamaResponse(
            String response
    ) {}
}