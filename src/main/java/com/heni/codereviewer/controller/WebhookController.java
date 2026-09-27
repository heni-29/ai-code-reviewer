package com.heni.codereviewer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.heni.codereviewer.dto.GitHubWebhookRequest;
import com.heni.codereviewer.repository.ReviewIssueRepository;
import com.heni.codereviewer.service.GitHubService;
import com.heni.codereviewer.service.RepositoryContextService;
import com.heni.codereviewer.service.ReviewProcessingService;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final GitHubService gitHubService;
    private final ReviewIssueRepository reviewIssueRepository;
    private final RepositoryContextService repositoryContextService;
    private final ReviewProcessingService reviewProcessingService;
    
    public WebhookController(
            GitHubService gitHubService,
            RepositoryContextService repositoryContextService,
            ReviewIssueRepository reviewIssueRepository,
            ReviewProcessingService reviewProcessingService) {

        this.gitHubService = gitHubService;
        this.reviewIssueRepository = reviewIssueRepository;
        this.repositoryContextService = repositoryContextService;
        this.reviewProcessingService = reviewProcessingService;
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
            return ResponseEntity.ok("Event ignored");
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

        String commitSha =
                request.getPull_request()
                        .getHead()
                        .getSha();

        boolean alreadyReviewed =
                reviewIssueRepository
                        .existsByRepositoryAndPullRequestNumberAndCommitSha(
                                fullName,
                                request.getNumber(),
                                commitSha
                        );

        if (alreadyReviewed) {

            System.out.println(
                    "Skipping duplicate review for PR #"
                            + request.getNumber()
                            + " at commit "
                            + commitSha
            );

            return ResponseEntity.ok(
                    "Review already processed for this commit"
            );
        }

        reviewProcessingService.processReview(
                owner,
                repository,
                fullName,
                request.getNumber(),
                commitSha
        );

        return ResponseEntity.ok(
                "Webhook received; review processing started"
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
    
}