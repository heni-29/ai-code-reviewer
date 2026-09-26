package com.heni.codereviewer.service;

import com.heni.codereviewer.dto.ChangedFile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
public class GitHubService {

    private final RestClient restClient;

    private record GitHubFileResponse(
            String content
    ) {
    }

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

    private record GitHubTreeResponse(
                List<GitHubTreeItem> tree
        ) {
        }

        private record GitHubTreeItem(
                String path,
                String type
        ) {
        }

    public List<String> getRepositoryFilePaths(
                String owner,
                String repository,
                String commitSha) {

        String endpoint = String.format(
                "/repos/%s/%s/git/trees/%s?recursive=1",
                owner,
                repository,
                commitSha
        );

        GitHubTreeResponse response =
                restClient.get()
                        .uri(endpoint)
                        .retrieve()
                        .body(GitHubTreeResponse.class);

        if (response == null || response.tree() == null) {
                return List.of();
        }

        return response.tree().stream()
                .filter(item -> "blob".equals(item.type()))
                .map(GitHubTreeItem::path)
                .toList();
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

    public String getFileContent(
            String owner,
            String repository,
            String path,
            String ref) {

        String endpoint = String.format(
                "/repos/%s/%s/contents/%s?ref=%s",
                owner,
                repository,
                path,
                ref
        );

        GitHubFileResponse response =
                restClient.get()
                        .uri(endpoint)
                        .retrieve()
                        .body(GitHubFileResponse.class);

        if (response == null ||
                response.content() == null) {

            return "";
        }

        String encodedContent =
                response.content().replace("\n", "");

        return new String(
                Base64.getDecoder().decode(encodedContent),
                StandardCharsets.UTF_8
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