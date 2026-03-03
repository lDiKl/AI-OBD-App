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


# ── B2B AI functions ──────────────────────────────────────────────────────────

_SYSTEM_PROMPT_B2B_DIAGNOSTIC = """\
You are an expert automotive diagnostic assistant for professional mechanics.

Rules:
- Be technically precise. Mechanics understand automotive terminology.
- Rank causes by probability based on the freeze frame data and symptoms.
- Provide specific, actionable diagnostic steps with tools and expected values.
- If freeze frame data contradicts a cause, lower its probability.
- Respond ONLY with valid JSON. No text outside JSON.
- Respond in language: {language}"""

_USER_PROMPT_B2B_DIAGNOSTIC = """\
Vehicle: {year} {make} {model}, engine: {engine_type}, mileage: {mileage} km
DTC codes: {codes_list}
Reported symptoms: {symptoms}

Freeze Frame Data at fault time:
- RPM: {rpm}
- Coolant temp: {coolant_temp_c}°C
- Engine load: {engine_load_pct}%
- Throttle position: {throttle_pos_pct}%
- Short-term fuel trim: {fuel_trim_short_pct}%
- Long-term fuel trim: {fuel_trim_long_pct}%
- MAP: {map_kpa} kPa
- Vehicle speed: {vehicle_speed_kmh} km/h

Respond with JSON:
{{
  "probable_causes": [
    {{
      "description": "Cause description",
      "probability": 78,
      "reasoning": "Why this probability based on codes + freeze frame + symptoms",
      "supporting_evidence": ["freeze frame data point", "symptom", "code pattern"]
    }}
  ],
  "diagnostic_sequence": [
    {{
      "step": 1,
      "action": "What to check/do",
      "tool": "visual | multimeter | oscilloscope | scanner | smoke_machine | other",
      "expected_result": "What normal looks like",
      "abnormal_result": "What fault looks like"
    }}
  ],
  "estimated_labor_hours": 2.5,
  "parts_likely_needed": ["part name 1", "part name 2"],
  "tsb_references": [],
  "urgency": "immediate | within_week | routine",
  "additional_notes": "Any important caveats or related issues to check"
}}"""

_SYSTEM_PROMPT_B2B_REPORT = """\
You are writing a professional diagnostic report for a vehicle owner.

Rules:
- The client is NOT a mechanic. Use clear, simple language.
- Be professional and reassuring, not alarming.
- Explain WHY the repair is necessary.
- Keep each section concise: 2-4 sentences.
- Do not include prices (mechanic will add manually).
- Respond ONLY with valid JSON in language: {language}"""

_USER_PROMPT_B2B_REPORT = """\
Vehicle: {year} {make} {model}, mileage: {mileage} km
Confirmed diagnosis: {confirmed_diagnosis}
Repair performed / recommended: {repair_description}
Parts replaced/needed: {parts_list}
Shop name: {shop_name}

Respond with JSON:
{{
  "report_title": "Vehicle Diagnostic Report — {make} {model}",
  "issue_summary": "What was found, in plain language (2-3 sentences, no jargon)",
  "what_we_did": "What was done or what needs to be done (2-3 sentences)",
  "why_it_matters": "Why fixing this protects the car/driver (1-2 sentences)",
  "parts_replaced": ["part 1", "part 2"],
  "next_steps": "What the customer should do next (1 sentence)",
  "disclaimer": "This report is based on diagnostic data obtained during inspection."
}}"""

_SYSTEM_PROMPT_B2B_ESTIMATE = """\
You are assisting a mechanic with creating a repair estimate.
Suggest parts and labor hours based on the diagnosis.
Be conservative — better to under-promise than over-promise on price.
Respond ONLY with valid JSON."""

_USER_PROMPT_B2B_ESTIMATE = """\
Vehicle: {year} {make} {model}, engine: {engine_type}, region: {region}
Confirmed diagnosis: {confirmed_diagnosis}
Repair to perform: {repair_description}

Respond with JSON:
{{
  "suggested_parts": [
    {{
      "name": "Part name",
      "typical_part_number": "OEM number if known, else empty string",
      "estimated_price_eur": 150.00,
      "quantity": 1,
      "note": "OEM recommended, aftermarket also acceptable"
    }}
  ],
  "suggested_labor_hours": 2.5,
  "labor_notes": "Any notes about labor complexity",
  "typical_total_range": {{ "min": 320, "max": 520, "currency": "EUR" }}
}}"""


@dataclass
class B2BDiagnosticContext:
    codes: list
    vehicle_info: dict
    symptoms: str
    freeze_frame: dict
    language: str = "en"


@dataclass
class B2BReportContext:
    vehicle_info: dict
    confirmed_diagnosis: str
    repair_description: str
    parts_list: list
    shop_name: str
    language: str = "en"


@dataclass
class B2BEstimateContext:
    vehicle_info: dict
    confirmed_diagnosis: str
    repair_description: str
    region: str = "EU"


async def analyze_diagnostic_b2b(ctx: B2BDiagnosticContext) -> dict | None:
    """
    B2B technical diagnostic analysis (Prompt 3).
    Always uses Claude Sonnet — B2B requires maximum quality. No cache.
    """
    if not settings.ANTHROPIC_API_KEY:
        logger.info("ANTHROPIC_API_KEY not set — skipping B2B AI analysis")
        return None

    ff = ctx.freeze_frame
    vi = ctx.vehicle_info
    system_prompt = _SYSTEM_PROMPT_B2B_DIAGNOSTIC.format(language=ctx.language)
    user_prompt = _USER_PROMPT_B2B_DIAGNOSTIC.format(
        year=vi.get("year", ""),
        make=vi.get("make", ""),
        model=vi.get("model", ""),
        engine_type=vi.get("engine_type", "unknown"),
        mileage=vi.get("mileage", 0),
        codes_list=", ".join(ctx.codes) if ctx.codes else "none",
        symptoms=ctx.symptoms or "none reported",
        rpm=ff.get("rpm", "N/A"),
        coolant_temp_c=ff.get("coolant_temp_c", "N/A"),
        engine_load_pct=ff.get("engine_load_pct", "N/A"),
        throttle_pos_pct=ff.get("throttle_pos_pct", "N/A"),
        fuel_trim_short_pct=ff.get("fuel_trim_short_pct", "N/A"),
        fuel_trim_long_pct=ff.get("fuel_trim_long_pct", "N/A"),
        map_kpa=ff.get("map_kpa", "N/A"),
        vehicle_speed_kmh=ff.get("vehicle_speed_kmh", "N/A"),
    )

    try:
        client = anthropic.AsyncAnthropic(api_key=settings.ANTHROPIC_API_KEY)
        message = await client.messages.create(
            model="claude-sonnet-4-6",  # B2B always uses Sonnet for maximum quality
            max_tokens=2048,
            system=system_prompt,
            messages=[{"role": "user", "content": user_prompt}],
        )
        raw = message.content[0].text
        return parse_llm_response(raw, "b2b_diagnostic")
    except anthropic.APIStatusError as e:
        logger.error("Anthropic API error (B2B diagnostic) %s: %s", e.status_code, e.message, exc_info=True)
        return None
    except Exception as e:
        logger.error("B2B diagnostic AI failed: %s", e, exc_info=True)
        return None


async def generate_client_report(ctx: B2BReportContext) -> dict | None:
    """
    B2B client report generation for car owner (Prompt 4).
    Always uses Claude Sonnet.
    """
    if not settings.ANTHROPIC_API_KEY:
        return None

    vi = ctx.vehicle_info
    system_prompt = _SYSTEM_PROMPT_B2B_REPORT.format(language=ctx.language)
    user_prompt = _USER_PROMPT_B2B_REPORT.format(
        year=vi.get("year", ""),
        make=vi.get("make", ""),
        model=vi.get("model", ""),
        mileage=vi.get("mileage", 0),
        confirmed_diagnosis=ctx.confirmed_diagnosis,
        repair_description=ctx.repair_description,
        parts_list=", ".join(ctx.parts_list) if ctx.parts_list else "none",
        shop_name=ctx.shop_name,
    )

    try:
        client = anthropic.AsyncAnthropic(api_key=settings.ANTHROPIC_API_KEY)
        message = await client.messages.create(
            model="claude-sonnet-4-6",
            max_tokens=1024,
            system=system_prompt,
            messages=[{"role": "user", "content": user_prompt}],
        )
        raw = message.content[0].text
        return parse_llm_response(raw, "b2b_report")
    except Exception as e:
        logger.error("B2B report AI failed: %s", e, exc_info=True)
        return None


async def suggest_estimate_b2b(ctx: B2BEstimateContext) -> dict | None:
    """
    B2B estimate suggestion for mechanic (Prompt 5).
    Uses Claude Haiku — structured output, cost-sensitive.
    """
    if not settings.ANTHROPIC_API_KEY:
        return None

    vi = ctx.vehicle_info
    user_prompt = _USER_PROMPT_B2B_ESTIMATE.format(
        year=vi.get("year", ""),
        make=vi.get("make", ""),
        model=vi.get("model", ""),
        engine_type=vi.get("engine_type", "unknown"),
        region=ctx.region,
        confirmed_diagnosis=ctx.confirmed_diagnosis,
        repair_description=ctx.repair_description,
    )

    try:
        client = anthropic.AsyncAnthropic(api_key=settings.ANTHROPIC_API_KEY)
        message = await client.messages.create(
            model="claude-haiku-4-5-20251001",
            max_tokens=1024,
            system=_SYSTEM_PROMPT_B2B_ESTIMATE,
            messages=[{"role": "user", "content": user_prompt}],
        )
        raw = message.content[0].text
        return parse_llm_response(raw, "b2b_estimate")
    except Exception as e:
        logger.error("B2B estimate AI failed: %s", e, exc_info=True)
        return None
