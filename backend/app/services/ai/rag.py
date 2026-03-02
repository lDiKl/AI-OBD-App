"""
Layer 2: RAG Retrieval — fetches context from vector DB.

Uses pgvector (PostgreSQL extension) for MVP.
Returns top-K relevant chunks for a given DTC code + vehicle context.
"""


async def retrieve_context(
    code: str,
    make: str,
    model: str,
    year: int,
    top_k: int = 3,
) -> tuple[list[str], bool]:
    """
    Query vector DB for relevant context chunks.

    Returns:
        (chunks, context_found) — list of text chunks + whether any found
    """
    # TODO: implement pgvector similarity search
    # query = f"{code} {make} {model} {year}"
    # embedding = await get_embedding(query)
    # results = await db.execute(
    #     "SELECT content FROM knowledge_base ORDER BY embedding <-> $1 LIMIT $2",
    #     embedding, top_k
    # )
    return [], False
