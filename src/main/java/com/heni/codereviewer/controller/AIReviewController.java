package com.heni.codereviewer.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.heni.codereviewer.dto.AIReviewResponse;
import com.heni.codereviewer.dto.ChangedFile;
import com.heni.codereviewer.service.AIReviewService;

@RestController
@RequestMapping("/api/review")
public class AIReviewController {

    private final AIReviewService aiReviewService;

    public AIReviewController(AIReviewService aiReviewService) {
        this.aiReviewService = aiReviewService;
    }

    @PostMapping("/test")
    public AIReviewResponse testReview(@RequestBody ChangedFile file, String repositoryContext) {
        return aiReviewService.review(file, repositoryContext);
    }
}