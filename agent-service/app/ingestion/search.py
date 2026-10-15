from app.database import get_connection
from app.embeddings import embed_text


def search_similar_chunks(
    query: str,
    repository: str,
    commit_sha: str,
    limit: int = 5,
) -> list[dict]:
    if not query.strip():
        return []

    if limit <= 0:
        return []

    query_embedding = embed_text(query)

    with get_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                """
                SELECT
                    file_path,
                    chunk_index,
                    start_line,
                    end_line,
                    content,
                    1 - (embedding <=> %s::vector) AS similarity
                FROM code_chunks
                WHERE repository = %s
                  AND commit_sha = %s
                  AND embedding IS NOT NULL
                ORDER BY embedding <=> %s::vector
                LIMIT %s
                """,
                (
                    query_embedding,
                    repository,
                    commit_sha,
                    query_embedding,
                    limit,
                ),
            )

            rows = cursor.fetchall()

    return [
        {
            "file_path": row[0],
            "chunk_index": row[1],
            "start_line": row[2],
            "end_line": row[3],
            "content": row[4],
            "similarity": float(row[5]),
        }
        for row in rows
    ]
