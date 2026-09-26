# AI Code Reviewer

An AI-powered GitHub Pull Request reviewer built with **Java 21, Spring Boot, GitHub REST APIs, Ollama, Qwen 2.5 Coder, and PostgreSQL**.

The application automatically analyzes code changes in GitHub Pull Requests, identifies potential bugs and security vulnerabilities, stores review findings in PostgreSQL, and posts actionable review comments directly to the Pull Request.

---

## Architecture

```text
GitHub Pull Request
        |
        | Webhook
        v
      ngrok
        |
        v
Spring Boot Backend
        |
        +--------------------> GitHub REST API
        |                             |
        |                             v
        |                      Changed Files
        |
        v
Ollama + Qwen 2.5 Coder
        |
        | Structured JSON Review
        v
    PostgreSQL
        |
        v
GitHub Pull Request Comment
```

---

## Features

- GitHub Pull Request webhook integration
- Automatically reacts to Pull Request updates
- Fetches changed files using the GitHub REST API
- AI-powered code analysis using Qwen 2.5 Coder
- Runs AI inference locally through Ollama
- Analyzes code patches instead of entire repositories
- Detects potential:
  - Bugs
  - Security vulnerabilities
  - Performance issues
  - Code duplication
  - Testing issues
  - API breaking changes
- Assigns severity to detected issues
- Assigns confidence scores to AI findings
- Converts AI findings into structured JSON
- Stores review findings in PostgreSQL
- Automatically posts review comments directly to GitHub Pull Requests
- Uses Spring Data JPA for database persistence
- Keeps AI inference local without requiring a paid AI API

---

## Tech Stack

| Technology | Purpose |
|---|---|
| **Java 21** | Backend development |
| **Spring Boot** | REST API and application framework |
| **GitHub REST API** | Pull Request and repository integration |
| **GitHub Webhooks** | Trigger automated reviews |
| **Ollama** | Local LLM inference |
| **Qwen 2.5 Coder 7B** | AI-powered code analysis |
| **PostgreSQL** | Persistent storage for review findings |
| **Spring Data JPA** | Database access and persistence |
| **Jackson** | JSON serialization and deserialization |
| **Maven** | Build and dependency management |
| **Docker** | PostgreSQL containerization |
| **ngrok** | Local webhook tunneling |

---

## How It Works

### 1. Pull Request Event

A developer creates or updates a Pull Request on GitHub.

GitHub sends a `pull_request` webhook event to the Spring Boot application.

### 2. Webhook Processing

The Spring Boot backend receives the webhook and extracts:

- Repository name
- Repository owner
- Pull Request number
- Pull Request action

### 3. Fetch Changed Files

The backend calls the GitHub REST API to retrieve the files changed in the Pull Request.

```text
GitHub PR
    |
    v
GitHub REST API
    |
    v
Changed Files + Patches
```

### 4. AI Code Analysis

Each changed file's patch is sent to **Qwen 2.5 Coder 7B** through Ollama.

The AI is instructed to report only concrete issues introduced by the change.

The response is returned as structured JSON:

```json
{
  "issues": [
    {
      "category": "SECURITY",
      "severity": "HIGH",
      "file": "Hello.java",
      "line": 3,
      "message": "SQL injection vulnerability caused by string concatenation.",
      "suggestion": "Use parameterized queries to prevent SQL injection.",
      "confidence": 0.95
    }
  ]
}
```

### 5. Persist Findings

Each AI finding is converted into a JPA entity and stored in PostgreSQL.

Stored information includes:

- Category
- Severity
- File
- Line number
- Message
- Suggested fix
- Confidence
- Repository
- Pull Request number

### 6. GitHub Review Comment

The backend converts the finding into a Markdown comment and posts it directly to the Pull Request through the GitHub REST API.

---

## Example Review

Given the following vulnerable code:

```java
public String getUser(String userId) {
    String query = "SELECT * FROM users WHERE id = '" + userId + "'";
    return database.execute(query);
}
```

The AI reviewer can identify the SQL injection vulnerability and generate:

```text
!ALERT! SECURITY — HIGH

Hello.java:3

SQL injection vulnerability due to string concatenation.

Suggested fix:
Use parameterized queries to prevent SQL injection.

Confidence: 95%
```

The finding is:

1. Returned by the AI as structured JSON
2. Saved to PostgreSQL
3. Posted automatically to the GitHub Pull Request

---

## Project Structure

```text
ai-code-reviewer/
│
├── .mvn/
│   └── wrapper/
│
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── heni/
│       │           └── codereviewer/
│       │               ├── config/
│       │               │   └── JacksonConfig.java
│       │               │
│       │               ├── controller/
│       │               │   ├── HealthController.java
│       │               │   └── WebhookController.java
│       │               │
│       │               ├── dto/
│       │               │   ├── AIReviewResponse.java
│       │               │   ├── ChangedFile.java
│       │               │   ├── GitHubWebhookRequest.java
│       │               │   └── ReviewIssue.java
│       │               │
│       │               ├── entity/
│       │               │   └── ReviewIssueEntity.java
│       │               │
│       │               ├── repository/
│       │               │   └── ReviewIssueRepository.java
│       │               │
│       │               └── service/
│       │                   ├── AIReviewService.java
│       │                   └── GitHubService.java
│       │
│       └── resources/
│           └── application.yml
│
├── .gitignore
├── .gitattributes
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

---

## Getting Started

### Prerequisites

Make sure you have the following installed:

- Java 21
- Docker
- Ollama
- Git
- GitHub account

---

## 1. Clone the Repository

```bash
git clone https://github.com/heni-29/ai-code-reviewer-demo.git

cd ai-code-reviewer-demo
```

---

## 2. Start PostgreSQL

The application uses PostgreSQL with:

```text
Database: code_reviewer
Username: reviewer
Password: reviewer
Port: 5433
```

Start the database using Docker:

```bash
docker run --name ai-code-reviewer-db \
  -e POSTGRES_DB=code_reviewer \
  -e POSTGRES_USER=reviewer \
  -e POSTGRES_PASSWORD=reviewer \
  -p 5433:5432 \
  -d postgres:16
```

If the container already exists:

```bash
docker start ai-code-reviewer-db
```

Verify:

```bash
docker ps
```

---

## 3. Install Qwen 2.5 Coder

Install Ollama and pull the model:

```bash
ollama pull qwen2.5-coder:7b
```

Verify:

```bash
ollama list
```

The application expects Ollama to be available at:

```text
http://localhost:11434
```

---

## 4. Configure GitHub Authentication

The application uses a GitHub Personal Access Token to access the GitHub REST API.

Set the token as an environment variable:

```bash
export GITHUB_TOKEN=your_github_token
```

> Never commit your GitHub token to the repository.

---

## 5. Run the Spring Boot Application

Start the backend:

```bash
./mvnw spring-boot:run
```

The application runs on:

```text
http://localhost:8080
```

Test the health endpoint:

```bash
curl http://localhost:8080/api/health
```

Expected response:

```text
AI Code Reviewer is running!
```

---

## 6. Configure the GitHub Webhook

Because the Spring Boot application is running locally, GitHub needs a publicly accessible URL.

Start ngrok:

```bash
ngrok http 8080
```

ngrok will provide a URL similar to:

```text
https://example.ngrok-free.dev
```

Configure the GitHub webhook:

```text
Payload URL:
https://example.ngrok-free.dev/api/webhooks/github
```

Set:

```text
Content type:
application/json
```

Enable the:

```text
Pull requests
```

event.

---

## Database

The application automatically creates the `review_issues` table using JPA/Hibernate.

Each review issue contains:

```text
id
category
severity
file
line
message
suggestion
confidence
repository
pullRequestNumber
```

Example database record:

```text
category: SECURITY
severity: HIGH
file: Hello.java
line: 3
confidence: 0.95
repository: heni-29/ai-code-reviewer-demo
pullRequestNumber: 1
```

---

## API Endpoints

### Health Check

```http
GET /api/health
```

Example:

```bash
curl http://localhost:8080/api/health
```

Response:

```text
AI Code Reviewer is running!
```

### GitHub Webhook

```http
POST /api/webhooks/github
```

Receives GitHub Pull Request events and triggers the automated review workflow.

Non-`pull_request` events are ignored.

---

## Security

Sensitive credentials such as the GitHub Personal Access Token are supplied through environment variables rather than stored in source code.

The current setup is designed for local development.

Production hardening can include:

- GitHub webhook signature verification
- Secure secret management
- Authentication and authorization
- Rate limiting
- HTTPS deployment
- Secure cloud infrastructure

---

## Current Status

The core end-to-end workflow has been implemented and tested:

```text
GitHub Pull Request
        |
        v
GitHub Webhook
        |
        v
ngrok
        |
        v
Spring Boot
        |
        v
GitHub REST API
        |
        v
Changed Code
        |
        v
Ollama + Qwen 2.5 Coder
        |
        v
Structured AI Review
        |
        +--------> PostgreSQL
        |
        v
GitHub Pull Request Comment
```

---

## Future Improvements

- [ ] Asynchronous Pull Request processing
- [ ] Duplicate review prevention
- [ ] GitHub webhook signature validation
- [ ] Improved exception handling
- [ ] Unit and integration tests
- [ ] Dockerize the Spring Boot application
- [ ] Application metrics and observability
- [ ] React-based review dashboard
- [ ] Review history and analytics
- [ ] Production deployment
- [ ] Support for large Pull Requests
- [ ] Inline GitHub review comments
- [ ] Configurable AI models
- [ ] Multi-language code analysis

---

## Project Goals

This project demonstrates practical experience building a backend system that combines:

- REST API development
- Event-driven webhook processing
- Third-party API integration
- AI/LLM integration
- Structured AI output processing
- Database persistence
- Local AI inference
- Docker-based infrastructure
- GitHub automation

---

## Author

**Heni Prajapati**

GitHub: [heni-29](https://github.com/heni-29/ai-code-reviewer)
