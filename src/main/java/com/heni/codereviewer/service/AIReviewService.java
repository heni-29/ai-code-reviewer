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

    public AIReviewResponse review(
            ChangedFile file,
            String repositoryContext) {

        String prompt = """
                You are an expert senior software engineer reviewing a GitHub pull request.

                Analyze the provided code change using both:
                1. The changed patch
                2. Relevant repository context

                Use repository context to understand:
                - existing interfaces
                - method contracts
                - dependencies
                - data models
                - repository/service interactions
                - existing implementation behavior

                IMPORTANT:
                The CHANGED FILE and PATCH represent the only code being modified by this PR.

                The REPOSITORY CONTEXT contains existing code that is provided only
                to help understand dependencies and behavior.

                Do NOT report issues solely because of problems in repository context.
                Only report an issue when the PATCH introduces or causes the problem.

                When repository context conflicts with the patch, prioritize the actual
                changed code and use the repository context to understand its behavior.

                Do NOT report:
                - stylistic preferences
                - generic best practices
                - hypothetical issues without evidence
                - unrelated existing problems
                - documentation suggestions

                Look for:
                - BUG
                - SECURITY
                - PERFORMANCE
                - DUPLICATION
                - TEST
                - API_BREAKING_CHANGE

                Severity must be one of:
                CRITICAL, HIGH, MEDIUM, LOW

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

                === CHANGED FILE ===
                %s

                === PATCH ===
                %s

                === REPOSITORY CONTEXT ===
                %s
                """.formatted(
                file.getFilename(),
                file.getPatch(),
                repositoryContext
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
            String cleanedResponse = response
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            return objectMapper.readValue(
                    cleanedResponse,
                    AIReviewResponse.class
            );

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
    ) {
    }

    private record OllamaResponse(
            String response
    ) {
    }
}