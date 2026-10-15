from app.ingestion.repository import ingest_repository, index_repository

def test_ingest_repository_creates_chunks(monkeypatch):
    monkeypatch.setattr(
        "app.ingestion.repository.get_repository_commit_sha",
        lambda repository, ref: "abc123",
    )

    monkeypatch.setattr(
        "app.ingestion.repository.select_relevant_files",
        lambda repository, issue, ref, limit: [
            "src/Calculator.py",
            "tests/test_calculator.py",
        ],
    )

    sources = {
        "src/Calculator.py": "def add(a, b):\n    return a + b",
        "tests/test_calculator.py": "def test_add():\n    assert add(1, 2) == 3",
    }

    monkeypatch.setattr(
        "app.ingestion.repository.get_repository_file",
        lambda repository, file_path, ref: sources[file_path],
    )

    chunks = ingest_repository(
        repository="owner/repo",
        issue="Fix calculator",
        ref="main",
        max_files=5,
        max_lines=1,
    )

    assert len(chunks) == 4

    assert chunks[0].repository == "owner/repo"
    assert chunks[0].commit_sha == "abc123"
    assert chunks[0].file_path == "src/Calculator.py"
    assert chunks[0].chunk_index == 0
    assert chunks[0].content == "def add(a, b):"

    assert chunks[1].file_path == "src/Calculator.py"
    assert chunks[1].chunk_index == 1

    assert chunks[2].file_path == "tests/test_calculator.py"
    assert chunks[2].chunk_index == 0

def test_index_repository_stores_chunks():
    from unittest.mock import patch

    from app.ingestion.chunker import CodeChunk
    from app.ingestion.repository import index_repository

    chunks = [
        CodeChunk(
            repository="demo/example",
            commit_sha="abc123",
            file_path="src/UserService.java",
            chunk_index=0,
            start_line=1,
            end_line=3,
            content="public class UserService {}",
        )
    ]

    with (
        patch(
            "app.ingestion.repository.get_repository_commit_sha",
            return_value="abc123",
        ),
        patch(
            "app.ingestion.repository.ingest_repository",
            return_value=chunks,
        ),
        patch(
            "app.ingestion.repository.store_chunks",
            return_value=1,
        ) as mock_store,
    ):
        commit_sha, stored = index_repository(
            repository="demo/example",
            issue="Fix user lookup",
            ref="main",
        )

    assert commit_sha == "abc123"
    assert stored == 1
    mock_store.assert_called_once_with(chunks)

def test_index_repository_skips_already_indexed_commit():
    from unittest.mock import patch

    with (
        patch(
            "app.ingestion.repository.get_repository_commit_sha",
            return_value="abc123",
        ),
        patch(
            "app.ingestion.repository.has_indexed_commit",
            return_value=True,
        ),
        patch(
            "app.ingestion.repository.ingest_repository",
        ) as mock_ingest,
        patch(
            "app.ingestion.repository.store_chunks",
        ) as mock_store,
    ):
        commit_sha, stored = index_repository(
            repository="demo/example",
            issue="Fix user lookup",
            ref="main",
        )

    assert commit_sha == "abc123"
    assert stored == 0
    mock_ingest.assert_not_called()
    mock_store.assert_not_called()
