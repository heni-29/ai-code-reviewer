package com.heni.codereviewer.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.heni.codereviewer.entity.ReviewIssueEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class ReviewIssueRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("code_reviewer_test")
                    .withUsername("reviewer")
                    .withPassword("reviewer");

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry) {

        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
    }

    @Autowired
    private ReviewIssueRepository reviewIssueRepository;

    @Test
    void shouldDetectExistingReviewForCommit() {

        ReviewIssueEntity entity =
                new ReviewIssueEntity();

        entity.setCategory("BUG");
        entity.setSeverity("HIGH");
        entity.setFile("UserService.java");
        entity.setLine(12);
        entity.setMessage("Test bug");
        entity.setSuggestion("Fix bug");
        entity.setConfidence(0.95);
        entity.setRepository(
                "heni-29/ai-code-reviewer-demo"
        );
        entity.setPullRequestNumber(2);
        entity.setCommitSha("abc123");

        reviewIssueRepository.save(entity);

        boolean exists =
                reviewIssueRepository
                        .existsByRepositoryAndPullRequestNumberAndCommitSha(
                                "heni-29/ai-code-reviewer-demo",
                                2,
                                "abc123"
                        );

        assertTrue(exists);
    }

    @Test
    void shouldReturnFalseForDifferentCommit() {

        boolean exists =
                reviewIssueRepository
                        .existsByRepositoryAndPullRequestNumberAndCommitSha(
                                "heni-29/ai-code-reviewer-demo",
                                2,
                                "does-not-exist"
                        );

        assertFalse(exists);
    }
}