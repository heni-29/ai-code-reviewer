import os

import ollama

EMBEDDING_MODEL = os.getenv(
    "OLLAMA_EMBEDDING_MODEL",
    "nomic-embed-text",
)


def embed_text(text: str) -> list[float]:
    response = ollama.embeddings(
        model=EMBEDDING_MODEL,
        prompt=text,
    )
    return response["embedding"]
