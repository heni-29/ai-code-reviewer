# AI Code Reviewer

An AI powered GitHub Pull Request reviewer built with **Java, Spring Boot, GitHub REST APIs, Ollama, Qwen 2.5 Coder, and PostgreSQL**.

The application automatically analyzes code changes in GitHub Pull Requests, identifies potential bugs and security issues, stores findings in PostgreSQL, and posts actionable review comments directly on the Pull Request.

---

## Architecture

```text
GitHub Pull Request
        │
        │ Webhook
        ▼
     ngrok
        │
        ▼
Spring Boot Backend
        │
        ├──────────────► GitHub REST API
        │                  │
        │                  └── Fetch changed files
        │
        ▼
   Ollama / Qwen
   2.5 Coder 7B
        │
        │ Structured JSON review
        ▼
   PostgreSQL
        │
        └──────────────► GitHub PR Comment
