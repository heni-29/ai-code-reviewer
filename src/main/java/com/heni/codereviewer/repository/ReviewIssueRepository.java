package com.heni.codereviewer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.heni.codereviewer.entity.ReviewIssueEntity;

public interface ReviewIssueRepository
        extends JpaRepository<ReviewIssueEntity, Long> {

    List<ReviewIssueEntity> findByRepositoryAndPullRequestNumber(
            String repository,
            int pullRequestNumber
    );

    boolean existsByRepositoryAndPullRequestNumberAndCommitSha(
                String repository,
                int pullRequestNumber,
                String commitSha
);
}