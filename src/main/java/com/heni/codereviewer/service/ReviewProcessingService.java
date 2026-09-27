package com.heni.codereviewer.service;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.heni.codereviewer.dto.AIReviewResponse;
import com.heni.codereviewer.dto.ChangedFile;
import com.heni.codereviewer.entity.ReviewIssueEntity;
import com.heni.codereviewer.repository.ReviewIssueRepository;

@Service
public class ReviewProcessingService {

    private final GitHubService gitHubService;
    private final AIReviewService aiReviewService;
    private final ReviewIssueRepository reviewIssueRepository;
    private final RepositoryContextService repositoryContextService;

    public ReviewProcessingService(
            GitHubService gitHubService,
            AIReviewService aiReviewService,
            ReviewIssueRepository reviewIssueRepository,
            RepositoryContextService repositoryContextService) {

        this.gitHubService = gitHubService;
        this.aiReviewService = aiReviewService;
        this.reviewIssueRepository = reviewIssueRepository;
        this.repositoryContextService = repositoryContextService;
    }

    @Async
    public void processReview(
            String owner,
            String repository,
            String fullName,
            int pullRequestNumber,
            String commitSha) {

        System.out.println(
                "Starting async review for PR #"
                        + pullRequestNumber
                        + " at commit "
                        + commitSha
        );

        List<ChangedFile> changedFiles =
                gitHubService.getPullRequestFiles(
                        owner,
                        repository,
                        pullRequestNumber
                );

        System.out.println(
                "Changed files: " + changedFiles.size()
        );

        for (ChangedFile file : changedFiles) {

            System.out.println(
                    "Reviewing: " + file.getFilename()
            );

            try {

                if ("removed".equalsIgnoreCase(file.getStatus())) {
                    System.out.println(
                            "Skipping removed file: "
                                    + file.getFilename()
                    );
                    continue;
                }

                String changedFileContent =
                        gitHubService.getFileContent(
                                owner,
                                repository,
                                file.getFilename(),
                                commitSha
                        );

                String repositoryContext =
                        repositoryContextService.buildContext(
                                owner,
                                repository,
                                file.getFilename(),
                                changedFileContent,
                                commitSha
                        );

                System.out.println(
                        "Repository context size: "
                                + repositoryContext.length()
                                + " characters"
                );

                AIReviewResponse review =
                        aiReviewService.review(
                                file,
                                repositoryContext
                        );

                System.out.println(
                        "AI Review: " + review.issues
                );

                for (var issue : review.issues) {

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
                            pullRequestNumber
                    );
                    entity.setCommitSha(commitSha);

                    reviewIssueRepository.save(entity);

                    System.out.println(
                            "Saved review issue: "
                                    + entity
                    );

                    String comment =
                            buildGitHubComment(issue);

                    gitHubService.postPullRequestComment(
                            owner,
                            repository,
                            pullRequestNumber,
                            comment
                    );

                    System.out.println(
                            "Posted GitHub comment for "
                                    + issue.file
                                    + ":"
                                    + issue.line
                    );
                }

            } catch (Exception e) {

                System.out.println(
                        "Failed to review "
                                + file.getFilename()
                                + ": "
                                + e.getMessage()
                );
            }
        }

        System.out.println(
                "Async review completed for PR #"
                        + pullRequestNumber
        );
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