from app.ingestion.chunker import CodeChunk
from app.ingestion import storage


def test_store_chunks_inserts_chunks(monkeypatch):
    chunk = CodeChunk(
        repository="owner/repo",
        commit_sha="abc123",
        file_path="src/example.py",
        chunk_index=0,
        start_line=1,
        end_line=2,
        content="def add(a, b):\n    return a + b",
    )

    executed = []
    committed = False

    class FakeCursor:
        rowcount = 1

        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc, tb):
            pass

        def execute(self, query, params):
            executed.append((query, params))

    class FakeConnection:
        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc, tb):
            pass

        def cursor(self):
            return FakeCursor()

        def commit(self):
            nonlocal committed
            committed = True

    monkeypatch.setattr(
        storage,
        "get_connection",
        lambda: FakeConnection(),
    )

    monkeypatch.setattr(
        storage,
        "embed_text",
        lambda text: [0.1, 0.2, 0.3],
    )

    result = storage.store_chunks([chunk])

    assert result == 1
    assert committed is True
    assert len(executed) == 1

    query, params = executed[0]

    assert "INSERT INTO code_chunks" in query
    assert params == (
        "owner/repo",
        "abc123",
        "src/example.py",
        0,
        1,
        2,
        "def add(a, b):\n    return a + b",
        [0.1, 0.2, 0.3],
    )
