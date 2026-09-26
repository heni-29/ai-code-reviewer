package com.heni.codereviewer.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.heni.codereviewer.dto.AIReviewResponse;
import com.heni.codereviewer.dto.ChangedFile;
import com.heni.codereviewer.dto.GitHubWebhookRequest;
import com.heni.codereviewer.entity.ReviewIssueEntity;
import com.heni.codereviewer.repository.ReviewIssueRepository;
import com.heni.codereviewer.service.AIReviewService;
import com.heni.codereviewer.service.GitHubService;
import com.heni.codereviewer.service.RepositoryContextService;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final GitHubService gitHubService;
    private final AIReviewService aiReviewService;
    private final ReviewIssueRepository reviewIssueRepository;
    private final RepositoryContextService repositoryContextService;
    
    public WebhookController(
            GitHubService gitHubService,
            AIReviewService aiReviewService,
            RepositoryContextService repositoryContextService,
            ReviewIssueRepository reviewIssueRepository) {

        this.gitHubService = gitHubService;
        this.aiReviewService = aiReviewService;
        this.reviewIssueRepository = reviewIssueRepository;
        this.repositoryContextService = repositoryContextService;
    }

    @PostMapping("/github")
    public ResponseEntity<String> handleGitHubWebhook(
            @RequestHeader(
                    value = "X-GitHub-Event",
                    required = false
            ) String event,
            @RequestBody GitHubWebhookRequest request) {

        System.out.println("GitHub Event: " + event);
        if (!"pull_request".equals(event)) {
            return ResponseEntity.ok("Event ignored: ");
        }
        System.out.println("Action: " + request.getAction());
        System.out.println("PR Number: " + request.getNumber());

        if (request.getRepository() == null ||
                request.getRepository().getFull_name() == null) {

            return ResponseEntity.badRequest()
                    .body("Repository information missing");
        }

        String fullName =
                request.getRepository().getFull_name();

        String[] repositoryParts =
                fullName.split("/");

        if (repositoryParts.length != 2) {
            return ResponseEntity.badRequest()
                    .body("Invalid repository name");
        }

        String owner = repositoryParts[0];
        String repository = repositoryParts[1];

        List<ChangedFile> changedFiles =
                gitHubService.getPullRequestFiles(
                        owner,
                        repository,
                        request.getNumber()
                );

        System.out.println(
                "Changed files: " + changedFiles.size()
        );

        for (ChangedFile file : changedFiles) {

            System.out.println(
                    "Reviewing: " + file.getFilename()
            );

            String commitSha = request.getPull_request().getHead().getSha();

            String changedFileContent = gitHubService.getFileContent(owner, repository, file.getFilename(), commitSha);

            String repositoryContext = repositoryContextService.buildContext(owner, repository, file.getFilename(), changedFileContent, commitSha );
            
            System.out.println("Repository context size: " + repositoryContext.length() + " characters");
            
            System.out.println("Repository context built for: " + file.getFilename());

            AIReviewResponse review = aiReviewService.review(file, repositoryContext);

            System.out.println("AI Review: " + review.issues);

            for (var issue : review.issues) {

                // Save issue to PostgreSQL
                ReviewIssueEntity entity =
                        new ReviewIssueEntity();

                entity.setCategory(issue.category);
                entity.setSeverity(issue.severity);
                entity.setFile(issue.file);
                entity.setLine(issue.line);
                entity.setMessage(issue.message);
                entity.setSuggestion(issue.suggestion);
                entity.setConfidence(issue.confidence);

                entity.setRepository(fullName);
                entity.setPullRequestNumber(
                        request.getNumber()
                );

                reviewIssueRepository.save(entity);

                System.out.println(
                        "Saved review issue: " + entity
                );

                // Create GitHub comment
                String comment =
                        buildGitHubComment(issue);

                gitHubService.postPullRequestComment(
                        owner,
                        repository,
                        request.getNumber(),
                        comment
                );

                System.out.println(
                        "Posted GitHub comment for "
                                + issue.file
                                + ":"
                                + issue.line
                );
            }
        }

        return ResponseEntity.ok(
                "Webhook received, reviewed, and commented"
        );
    }

    @GetMapping("/test-context")
    public ResponseEntity<String> testContext() {

        String owner = "heni-29";
        String repository = "ai-code-reviewer";

        String path =
                "src/main/java/com/heni/codereviewer/service/AIReviewService.java";

        String commitSha = "main";

        String fileContent =
                gitHubService.getFileContent(
                        owner,
                        repository,
                        path,
                        commitSha
                );

        String context =
                repositoryContextService.buildContext(
                        owner,
                        repository,
                        path,
                        fileContent,
                        commitSha
                );

        return ResponseEntity.ok(context);
    }
    private String buildGitHubComment(
            com.heni.codereviewer.dto.ReviewIssue issue) {

        return """
                ## AI Code Review

                ### !!ALERT %s - %s

                **%s:%d**

                %s

                **Suggested fix:**
                %s

                **Confidence:** %.0f%%
                """.formatted(
                issue.category,
                issue.severity,
                issue.file,
                issue.line,
                issue.message,
                issue.suggestion,
                issue.confidence * 100
        );
    }
}