from app.ingestion.chunker import chunk_source_file


def test_chunk_source_file_splits_source_into_chunks():
    source = "\n".join(f"line {i}" for i in range(1, 6))

    chunks = chunk_source_file(
        repository="owner/repo",
        commit_sha="abc123",
        file_path="src/example.py",
        source=source,
        max_lines=2,
    )

    assert len(chunks) == 3

    assert chunks[0].content == "line 1\nline 2"
    assert chunks[1].content == "line 3\nline 4"
    assert chunks[2].content == "line 5"


def test_chunk_source_file_preserves_metadata():
    chunks = chunk_source_file(
        repository="owner/repo",
        commit_sha="abc123",
        file_path="src/example.py",
        source="line 1\nline 2",
        max_lines=1,
    )

    assert chunks[0].repository == "owner/repo"
    assert chunks[0].commit_sha == "abc123"
    assert chunks[0].file_path == "src/example.py"
    assert chunks[0].chunk_index == 0
    assert chunks[0].start_line == 1
    assert chunks[0].end_line == 1
    assert chunks[1].chunk_index == 1
    assert chunks[1].start_line == 2
    assert chunks[1].end_line == 2


def test_chunk_source_file_empty_source_returns_no_chunks():
    chunks = chunk_source_file(
        repository="owner/repo",
        commit_sha="abc123",
        file_path="src/example.py",
        source="",
    )

    assert chunks == []


def test_chunk_source_file_rejects_invalid_max_lines():
    try:
        chunk_source_file(
            repository="owner/repo",
            commit_sha="abc123",
            file_path="src/example.py",
            source="line 1",
            max_lines=0,
        )
    except ValueError as exc:
        assert str(exc) == "max_lines must be greater than zero"
    else:
        raise AssertionError("Expected ValueError")
