from app import embeddings


def test_embed_text_returns_embedding(monkeypatch):
    expected = [0.1, 0.2, 0.3]

    def fake_embeddings(model, prompt):
        assert model == "nomic-embed-text"
        assert prompt == "calculate total price"
        return {"embedding": expected}

    monkeypatch.setattr(
        embeddings.ollama,
        "embeddings",
        fake_embeddings,
    )

    result = embeddings.embed_text("calculate total price")

    assert result == expected
