from app.github_client import (
    get_repository_commit_sha,
    get_repository_file,
)
from app.graph.retrieval import select_relevant_files
from app.ingestion.chunker import CodeChunk, chunk_source_file
from app.ingestion.storage import store_chunks


def ingest_repository(
    repository: str,
    issue: str,
    ref: str = "main",
    max_files: int = 5,
    max_lines: int = 80,
) -> list[CodeChunk]:
    commit_sha = get_repository_commit_sha(repository, ref)

    selected_files = select_relevant_files(
        repository=repository,
        issue=issue,
        ref=ref,
        limit=max_files,
    )

    chunks: list[CodeChunk] = []

    for file_path in selected_files:
        source = get_repository_file(
            repository=repository,
            file_path=file_path,
            ref=ref,
        )

        chunks.extend(
            chunk_source_file(
                repository=repository,
                commit_sha=commit_sha,
                file_path=file_path,
                source=source,
                max_lines=max_lines,
            )
        )

    return chunks


def index_repository(
    repository: str,
    issue: str,
    ref: str = "main",
    max_files: int = 5,
    max_lines: int = 80,
) -> tuple[str, int]:
    commit_sha = get_repository_commit_sha(repository, ref)

    chunks = ingest_repository(
        repository=repository,
        issue=issue,
        ref=ref,
        max_files=max_files,
        max_lines=max_lines,
    )

    stored = store_chunks(chunks)

    return commit_sha, stored
