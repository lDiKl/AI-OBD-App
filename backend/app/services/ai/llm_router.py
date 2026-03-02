"""
Layer 3: LLM Router — selects model based on request complexity.

Routing logic:
- Redis cache hit → return cached (0ms)
- Standard code + popular make + RAG context found → Local LLM (Llama 3 8B)
- Rare code / B2B analysis / report generation → Claude API
"""

from enum import Enum


class LLMTarget(str, Enum):
    LOCAL = "local"    # Ollama / vLLM — cheap, ~500ms
    CLAUDE = "claude"  # Anthropic API — quality, ~2000ms


# Codes frequent enough to be handled by local model (top-500 most common)
TOP_COMMON_CODES = {
    "P0420", "P0430", "P0171", "P0174", "P0300",
    "P0301", "P0302", "P0303", "P0304", "P0441",
    "P0442", "P0455", "P0456", "P0128", "P0131",
    # TODO: load full top-500 from DB at startup
}


def route_request(
    code: str,
    is_b2b: bool,
    rag_context_found: bool,
    is_report_generation: bool = False,
) -> LLMTarget:
    """Decide which LLM to use for this request."""

    # B2B always uses Claude for maximum quality
    if is_b2b or is_report_generation:
        return LLMTarget.CLAUDE

    # Rare code with no RAG context — use Claude for better coverage
    if not rag_context_found:
        return LLMTarget.CLAUDE

    # Common code with RAG context — local model is sufficient
    if code in TOP_COMMON_CODES and rag_context_found:
        return LLMTarget.LOCAL

    # Default: Claude for edge cases
    return LLMTarget.CLAUDE
