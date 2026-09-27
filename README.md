# AI Code Reviewer

AI-powered GitHub Pull Request reviewer built with Java, Spring Boot,
GitHub Webhooks, Ollama/Qwen, and PostgreSQL.

The application automatically analyzes changed files in a pull request,
uses repository context to improve the analysis, stores detected issues,
and posts review findings back to the GitHub PR.

## Architecture

GitHub Pull Request
        |
        v
GitHub Webhook
        |
        v
Spring Boot Backend
        |
        +------------------+
        |                  |
        v                  v
   GitHub API       Repository Context
        |                  |
        +--------+---------+
                 |
                 v
          Qwen 2.5 Coder
             via Ollama
                 |
                 v
          Structured Review
                 |
          +------+------+
          |             |
          v             v
     PostgreSQL     GitHub PR
                    Comment

## Features

- GitHub Pull Request webhook integration
- Automatic changed-file retrieval
- Repository-aware code analysis
- Local AI inference using Qwen 2.5 Coder through Ollama
- Structured findings with:
  - Category
  - Severity
  - File
  - Line
  - Message
  - Suggested fix
  - Confidence
- PostgreSQL persistence
- Commit-based review idempotency
- Asynchronous review processing
- Automatic GitHub PR comments
- Unit and integration testing
- Testcontainers PostgreSQL integration
- Docker and Docker Compose support

## Tech Stack

- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL
- GitHub REST API
- GitHub Webhooks
- Ollama
- Qwen 2.5 Coder 7B
- Docker
- Docker Compose
- Testcontainers
- Maven

## How It Works

1. A GitHub Pull Request event is sent to the webhook endpoint.
2. The backend identifies the repository, PR number, and commit SHA.
3. Changed files are retrieved through the GitHub API.
4. Related repository files are identified from imports and referenced types.
5. The changed code and repository context are sent to Qwen 2.5 Coder.
6. The model returns structured review findings.
7. Findings are persisted in PostgreSQL.
8. Review findings are posted as comments on the GitHub PR.
9. Reviews are skipped if the same PR commit has already been processed.

## Running with Docker Compose

### Prerequisites

- Docker
- GitHub Personal Access Token
- Ollama running locally

Set your GitHub token:

```bash
export GITHUB_TOKEN="your_github_token"