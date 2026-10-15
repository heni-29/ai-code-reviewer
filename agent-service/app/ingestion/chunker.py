from dataclasses import dataclass


@dataclass(frozen=True)
class CodeChunk:
    repository: str
    commit_sha: str
    file_path: str
    chunk_index: int
    start_line: int
    end_line: int
    content: str


def chunk_source_file(
    repository: str,
    commit_sha: str,
    file_path: str,
    source: str,
    max_lines: int = 80,
) -> list[CodeChunk]:
    if max_lines <= 0:
        raise ValueError("max_lines must be greater than zero")

    lines = source.splitlines()

    if not lines:
        return []

    chunks = []

    for start in range(0, len(lines), max_lines):
        chunk_lines = lines[start:start + max_lines]

        chunks.append(
            CodeChunk(
                repository=repository,
                commit_sha=commit_sha,
                file_path=file_path,
                chunk_index=len(chunks),
                start_line=start + 1,
                end_line=start + len(chunk_lines),
                content="\n".join(chunk_lines),
            )
        )

    return chunks
