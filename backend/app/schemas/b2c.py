from pydantic import BaseModel


class FreezeFrame(BaseModel):
    rpm: float | None = None
    coolant_temp_c: float | None = None
    engine_load_pct: float | None = None
    throttle_pos_pct: float | None = None
    vehicle_speed_kmh: float | None = None
    fuel_trim_short_pct: float | None = None
    fuel_trim_long_pct: float | None = None
    map_kpa: float | None = None


class ScannedCode(BaseModel):
    code: str
    freeze_frame: FreezeFrame = FreezeFrame()


class ScanAnalyzeRequest(BaseModel):
    vehicle_id: str
    mileage: int
    codes: list[ScannedCode]


# --- Response ---

class CodeResultFree(BaseModel):
    description: str
    category: str
    severity: str  # low | medium | high | critical
    can_drive: str  # yes | yes_with_caution | yes_within_2_weeks | limited | no


class CodeResultPremium(BaseModel):
    simple_explanation: str
    main_causes: list[str]
    causes_probability: list[int]
    what_happens_if_ignored: str
    recommended_action: str
    cost_estimate_eur: dict | None = None  # {min, max}


class CodeResult(BaseModel):
    code: str
    free: CodeResultFree
    premium: CodeResultPremium | None = None  # None for free tier users


class ScanAnalyzeResponse(BaseModel):
    session_id: str
    overall_risk: str  # low | medium | high
    safe_to_drive: bool
    is_premium: bool = False  # True if user has premium subscription
    codes: list[CodeResult]
