package com.heni.codereviewer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.heni.codereviewer.repository.ReviewIssueRepository;
import com.heni.codereviewer.service.GitHubService;
import com.heni.codereviewer.service.RepositoryContextService;
import com.heni.codereviewer.service.ReviewProcessingService;
import com.heni.codereviewer.dto.GitHubWebhookRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class WebhookControllerTest {

    private GitHubService gitHubService;
    private ReviewIssueRepository reviewIssueRepository;
    private RepositoryContextService repositoryContextService;
    private ReviewProcessingService reviewProcessingService;

    private WebhookController controller;

    @BeforeEach
    void setUp() {

        gitHubService = mock(GitHubService.class);
        reviewIssueRepository = mock(ReviewIssueRepository.class);
        repositoryContextService = mock(RepositoryContextService.class);
        reviewProcessingService = mock(ReviewProcessingService.class);

        controller = new WebhookController(
                gitHubService,
                repositoryContextService,
                reviewIssueRepository,
                reviewProcessingService
        );
    }

    @Test
    void shouldIgnoreNonPullRequestEvent() {

        GitHubWebhookRequest request =
                new GitHubWebhookRequest();

        ResponseEntity<String> response =
                controller.handleGitHubWebhook(
                        "push",
                        request
                );

        assertEquals(200, response.getStatusCode().value());
        assertEquals(
                "Event ignored",
                response.getBody()
        );

        verifyNoInteractions(
                reviewIssueRepository,
                reviewProcessingService
        );
    }

    @Test
    void shouldSkipAlreadyReviewedCommit() {

        GitHubWebhookRequest request =
                new GitHubWebhookRequest();

        request.setNumber(2);

        GitHubWebhookRequest.Repository repository =
                new GitHubWebhookRequest.Repository();

        repository.setFull_name(
                "heni-29/ai-code-reviewer-demo"
        );

        request.setRepository(repository);

        GitHubWebhookRequest.PullRequest pullRequest =
                new GitHubWebhookRequest.PullRequest();

        GitHubWebhookRequest.Head head =
                new GitHubWebhookRequest.Head();

        head.setSha("test-commit-sha");

        pullRequest.setHead(head);

        request.setPull_request(pullRequest);

        when(
                reviewIssueRepository
                        .existsByRepositoryAndPullRequestNumberAndCommitSha(
                                "heni-29/ai-code-reviewer-demo",
                                2,
                                "test-commit-sha"
                        )
        ).thenReturn(true);

        ResponseEntity<String> response =
                controller.handleGitHubWebhook(
                        "pull_request",
                        request
                );

        assertEquals(200, response.getStatusCode().value());

        assertEquals(
                "Review already processed for this commit",
                response.getBody()
        );

        verify(
                reviewProcessingService,
                never()
        ).processReview(
                anyString(),
                anyString(),
                anyString(),
                anyInt(),
                anyString()
        );
    }

    @Test
    void shouldStartReviewForNewCommit() {

        GitHubWebhookRequest request =
                new GitHubWebhookRequest();

        request.setNumber(2);

        GitHubWebhookRequest.Repository repository =
                new GitHubWebhookRequest.Repository();

        repository.setFull_name(
                "heni-29/ai-code-reviewer-demo"
        );

        request.setRepository(repository);

        GitHubWebhookRequest.PullRequest pullRequest =
                new GitHubWebhookRequest.PullRequest();

        GitHubWebhookRequest.Head head =
                new GitHubWebhookRequest.Head();

        head.setSha("new-commit-sha");

        pullRequest.setHead(head);

        request.setPull_request(pullRequest);

        when(
                reviewIssueRepository
                        .existsByRepositoryAndPullRequestNumberAndCommitSha(
                                "heni-29/ai-code-reviewer-demo",
                                2,
                                "new-commit-sha"
                        )
        ).thenReturn(false);

        ResponseEntity<String> response =
                controller.handleGitHubWebhook(
                        "pull_request",
                        request
                );

        assertEquals(200, response.getStatusCode().value());

        assertEquals(
                "Webhook received; review processing started",
                response.getBody()
        );

        verify(reviewProcessingService)
                .processReview(
                        "heni-29",
                        "ai-code-reviewer-demo",
                        "heni-29/ai-code-reviewer-demo",
                        2,
                        "new-commit-sha"
                );
    }

}