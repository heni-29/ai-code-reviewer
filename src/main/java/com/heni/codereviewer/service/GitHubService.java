package com.heni.codereviewer.service;

import com.heni.codereviewer.dto.ChangedFile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class GitHubService {

    private final RestClient restClient;

    public GitHubService() {

        String token = System.getenv("GITHUB_TOKEN");

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "GITHUB_TOKEN environment variable is not set"
            );
        }

        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader(
                        "Authorization",
                        "Bearer " + token
                )
                .defaultHeader(
                        "Accept",
                        "application/vnd.github+json"
                )
                .defaultHeader(
                        "X-GitHub-Api-Version",
                        "2022-11-28"
                )
                .build();
    }

    public List<ChangedFile> getPullRequestFiles(
            String owner,
            String repository,
            int pullRequestNumber) {

        String endpoint = String.format(
                "/repos/%s/%s/pulls/%d/files",
                owner,
                repository,
                pullRequestNumber
        );

        return restClient.get()
                .uri(endpoint)
                .retrieve()
                .body(
                        new ParameterizedTypeReference<
                                List<ChangedFile>
                                >() {}
                );
    }

    public void postPullRequestComment(
            String owner,
            String repository,
            int pullRequestNumber,
            String body) {

        String endpoint = String.format(
                "/repos/%s/%s/issues/%d/comments",
                owner,
                repository,
                pullRequestNumber
        );

        restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new GitHubCommentRequest(body))
                .retrieve()
                .toBodilessEntity();
    }

    private record GitHubCommentRequest(
            String body
    ) {
    }
}