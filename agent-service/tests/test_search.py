from app.ingestion import search


def test_search_similar_chunks_returns_ranked_results(monkeypatch):
    monkeypatch.setattr(
        search,
        "embed_text",
        lambda text: [0.1, 0.2, 0.3],
    )

    class FakeCursor:
        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc, tb):
            pass

        def execute(self, query, params):
            self.query = query
            self.params = params

        def fetchall(self):
            return [
                (
                    "src/example.py",
                    0,
                    1,
                    2,
                    "def add(a, b):\n    return a + b",
                    0.92,
                )
            ]

    class FakeConnection:
        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc, tb):
            pass

        def cursor(self):
            return FakeCursor()

    monkeypatch.setattr(
        search,
        "get_connection",
        lambda: FakeConnection(),
    )

    results = search.search_similar_chunks(
        query="add two numbers",
        repository="owner/repo",
        commit_sha="abc123",
        limit=5,
    )

    assert len(results) == 1
    assert results[0]["file_path"] == "src/example.py"
    assert results[0]["content"] == "def add(a, b):\n    return a + b"
    assert results[0]["similarity"] == 0.92


def test_search_similar_chunks_rejects_empty_query():
    assert search.search_similar_chunks(
        query="   ",
        repository="owner/repo",
        commit_sha="abc123",
    ) == []


def test_search_similar_chunks_rejects_non_positive_limit():
    assert search.search_similar_chunks(
        query="find bug",
        repository="owner/repo",
        commit_sha="abc123",
        limit=0,
    ) == []
