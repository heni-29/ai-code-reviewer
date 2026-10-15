from app.database import get_connection
from app.embeddings import embed_text
from app.ingestion.chunker import CodeChunk


def store_chunks(chunks: list[CodeChunk]) -> int:
    if not chunks:
        return 0

    stored = 0

    with get_connection() as connection:
        with connection.cursor() as cursor:
            for chunk in chunks:
                embedding = embed_text(chunk.content)

                cursor.execute(
                    """
                    INSERT INTO code_chunks (
                        repository,
                        commit_sha,
                        file_path,
                        chunk_index,
                        start_line,
                        end_line,
                        content,
                        embedding
                    )
                    VALUES (
                        %s, %s, %s, %s, %s, %s, %s, %s
                    )
                    ON CONFLICT (
                        repository,
                        commit_sha,
                        file_path,
                        chunk_index
                    ) DO NOTHING
                    """,
                    (
                        chunk.repository,
                        chunk.commit_sha,
                        chunk.file_path,
                        chunk.chunk_index,
                        chunk.start_line,
                        chunk.end_line,
                        chunk.content,
                        embedding,
                    ),
                )

                stored += cursor.rowcount

        connection.commit()

    return stored
