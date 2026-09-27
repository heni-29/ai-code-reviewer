package com.heni.codereviewer.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.heni.codereviewer.dto.AIReviewResponse;
import com.heni.codereviewer.dto.ChangedFile;
import com.heni.codereviewer.dto.ReviewIssue;
import com.heni.codereviewer.entity.ReviewIssueEntity;
import com.heni.codereviewer.repository.ReviewIssueRepository;

class ReviewProcessingServiceTest {

    private GitHubService gitHubService;
    private AIReviewService aiReviewService;
    private ReviewIssueRepository reviewIssueRepository;
    private RepositoryContextService repositoryContextService;

    private ReviewProcessingService reviewProcessingService;

    @BeforeEach
    void setUp() {

        gitHubService = mock(GitHubService.class);
        aiReviewService = mock(AIReviewService.class);
        reviewIssueRepository = mock(ReviewIssueRepository.class);
        repositoryContextService =
                mock(RepositoryContextService.class);

        reviewProcessingService =
                new ReviewProcessingService(
                        gitHubService,
                        aiReviewService,
                        reviewIssueRepository,
                        repositoryContextService
                );
    }

    @Test
    void shouldProcessChangedFileAndSaveIssue() {

        String owner = "heni-29";
        String repository = "ai-code-reviewer-demo";
        String fullName = "heni-29/ai-code-reviewer-demo";
        int pullRequestNumber = 2;
        String commitSha = "test-commit-sha";

        ChangedFile file = new ChangedFile();

        file.setFilename(
                "src/main/java/com/example/demo/UserService.java"
        );

        file.setStatus("modified");
        file.setPatch("test patch");

        when(
                gitHubService.getPullRequestFiles(
                        owner,
                        repository,
                        pullRequestNumber
                )
        ).thenReturn(List.of(file));

        when(
                gitHubService.getFileContent(
                        owner,
                        repository,
                        file.getFilename(),
                        commitSha
                )
        ).thenReturn("public class UserService {}");

        when(
                repositoryContextService.buildContext(
                        owner,
                        repository,
                        file.getFilename(),
                        "public class UserService {}",
                        commitSha
                )
        ).thenReturn("repository context");

        ReviewIssue issue = new ReviewIssue();

        issue.category = "BUG";
        issue.severity = "HIGH";
        issue.file = file.getFilename();
        issue.line = 12;
        issue.message = "Bug found";
        issue.suggestion = "Fix the bug";
        issue.confidence = 0.95;

        AIReviewResponse aiResponse =
                new AIReviewResponse();

        aiResponse.issues = List.of(issue);

        when(
                aiReviewService.review(
                        file,
                        "repository context"
                )
        ).thenReturn(aiResponse);

        reviewProcessingService.processReview(
                owner,
                repository,
                fullName,
                pullRequestNumber,
                commitSha
        );

        verify(reviewIssueRepository)
                .save(any(ReviewIssueEntity.class));

        verify(gitHubService)
                .postPullRequestComment(
                        eq(owner),
                        eq(repository),
                        eq(pullRequestNumber),
                        anyString()
                );
    }

    @Test
    void shouldSkipRemovedFiles() {

        ChangedFile file = new ChangedFile();

        file.setFilename(
                "src/main/java/com/example/demo/Deleted.java"
        );

        file.setStatus("removed");

        when(
                gitHubService.getPullRequestFiles(
                        "heni-29",
                        "ai-code-reviewer-demo",
                        2
                )
        ).thenReturn(List.of(file));

        reviewProcessingService.processReview(
                "heni-29",
                "ai-code-reviewer-demo",
                "heni-29/ai-code-reviewer-demo",
                2,
                "test-commit-sha"
        );

        verify(
                gitHubService,
                never()
        ).getFileContent(
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );

        verifyNoInteractions(
                aiReviewService,
                repositoryContextService,
                reviewIssueRepository
        );
    }

    @Test
    void shouldProcessMultipleChangedFiles() {

        ChangedFile file1 = new ChangedFile();
        file1.setFilename("src/main/java/com/example/demo/UserService.java");
        file1.setStatus("modified");

        ChangedFile file2 = new ChangedFile();
        file2.setFilename("src/main/java/com/example/demo/UserRepository.java");
        file2.setStatus("modified");

        when(
                gitHubService.getPullRequestFiles(
                        "heni-29",
                        "ai-code-reviewer-demo",
                        2
                )
        ).thenReturn(List.of(file1, file2));

        when(
                gitHubService.getFileContent(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString()
                )
        ).thenReturn("test content");

        when(
                repositoryContextService.buildContext(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString()
                )
        ).thenReturn("test context");

        AIReviewResponse emptyReview =
                new AIReviewResponse();

        emptyReview.issues = List.of();

        when(
                aiReviewService.review(
                        any(ChangedFile.class),
                        anyString()
                )
        ).thenReturn(emptyReview);

        reviewProcessingService.processReview(
                "heni-29",
                "ai-code-reviewer-demo",
                "heni-29/ai-code-reviewer-demo",
                2,
                "test-commit-sha"
        );

        verify(gitHubService, times(2))
                .getFileContent(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString()
                );

        verify(aiReviewService, times(2))
                .review(
                        any(ChangedFile.class),
                        anyString()
                );
    }
}