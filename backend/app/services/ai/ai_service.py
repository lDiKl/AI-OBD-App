"""
AI Service — Layer 3+4: LLM call + Redis cache + fallback.

Phase 1: Claude API only (local LLM added in Phase 2 when Ollama is set up).
Phase 2: route_request() selects local vs Claude based on code frequency.
"""

import hashlib
import json
import logging
from dataclasses import dataclass

import anthropic
from redis.asyncio import Redis

from app.core.config import settings
from app.services.ai.output_formatter import parse_llm_response

logger = logging.getLogger(__name__)

# Cache TTL: 7 days — DTC explanations don't change often
CACHE_TTL_SECONDS = 7 * 24 * 60 * 60

# Prompts from docs/prompts.md
_SYSTEM_PROMPT_B2C = """\
You are an automotive assistant helping non-technical car owners understand their vehicle's diagnostic codes.

Rules:
- Use simple, everyday language. No technical jargon.
- Be honest but not alarming. Base risk only on provided data.
- Keep explanations short: 1-3 sentences per field.
- Use ONLY the data provided. Do not add information not in the input.
- Respond ONLY with valid JSON. No extra text outside JSON.
- Respond in the user's language: {language}"""

_USER_PROMPT_B2C = """\
Vehicle: {year} {make} {model}, engine: {engine_type}
Region: {region}
Error code: {code}
Standard description: {standard_description}
System affected: {system}
Severity: {severity_level}
Can drive: {can_drive_flag}

Respond with JSON:
{{
  "simple_explanation": "What this means in plain language (max 2 sentences)",
  "main_causes": ["most likely cause", "second cause", "third cause"],
  "causes_probability": [65, 25, 10],
  "what_happens_if_ignored": "One sentence about consequences",
  "recommended_action": "Exactly what the driver should do next (1 sentence)"
}}"""


@dataclass
class B2CCodeContext:
    code: str
    standard_description: str
    system: str
    severity_level: str
    can_drive_flag: str
    make: str
    model: str
    year: int
    engine_type: str
    region: str
    language: str


def _cache_key(ctx: B2CCodeContext) -> str:
    """Deterministic cache key for a B2C explanation request."""
    raw = f"b2c:{ctx.code}:{ctx.make}:{ctx.year}:{ctx.region}:{ctx.language}"
    return hashlib.md5(raw.encode()).hexdigest()


async def _get_redis() -> Redis | None:
    """Return async Redis client, or None if Redis is unavailable."""
    try:
        client = Redis.from_url(settings.REDIS_URL, decode_responses=True)
        await client.ping()
        return client
    except Exception:
        logger.warning("Redis unavailable — caching disabled")
        return None


async def explain_code_b2c(ctx: B2CCodeContext) -> dict | None:
    """
    Get AI explanation for a single DTC code for a B2C (premium) user.

    Returns:
        dict with keys: simple_explanation, main_causes, causes_probability,
                        what_happens_if_ignored, recommended_action
        None if AI is not configured or all retries fail (caller shows Rule Engine result only)
    """
    if not settings.ANTHROPIC_API_KEY:
        logger.info("ANTHROPIC_API_KEY not set — skipping AI explanation")
        return None

    cache_key = _cache_key(ctx)
    redis = await _get_redis()

    # Cache read
    if redis:
        try:
            cached = await redis.get(cache_key)
            if cached:
                logger.debug("AI cache hit: %s", cache_key)
                return json.loads(cached)
        except Exception:
            pass  # cache miss is fine

    system_prompt = _SYSTEM_PROMPT_B2C.format(language=ctx.language)
    user_prompt = _USER_PROMPT_B2C.format(
        year=ctx.year,
        make=ctx.make,
        model=ctx.model,
        engine_type=ctx.engine_type or "unknown",
        region=ctx.region,
        code=ctx.code,
        standard_description=ctx.standard_description,
        system=ctx.system,
        severity_level=ctx.severity_level,
        can_drive_flag=ctx.can_drive_flag,
    )

    try:
        client = anthropic.AsyncAnthropic(api_key=settings.ANTHROPIC_API_KEY)
        message = await client.messages.create(
            model="claude-haiku-4-5-20251001",  # cheapest + fast, sufficient for B2C
            max_tokens=512,
            system=system_prompt,
            messages=[{"role": "user", "content": user_prompt}],
        )
        raw = message.content[0].text
        result = parse_llm_response(raw, "b2c_single")

        # Cache write
        if redis and result:
            try:
                await redis.setex(cache_key, CACHE_TTL_SECONDS, json.dumps(result))
            except Exception:
                pass

        return result

    except anthropic.APIStatusError as e:
        logger.error("Anthropic API error %s: %s", e.status_code, e.message)
        return None
    except Exception as e:
        logger.error("AI explanation failed: %s", e)
        return None
    finally:
        if redis:
            await redis.aclose()
