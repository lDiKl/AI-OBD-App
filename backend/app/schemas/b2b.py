from pydantic import BaseModel


class FreezeFrameB2B(BaseModel):
    rpm: float | None = None
    coolant_temp_c: float | None = None
    engine_load_pct: float | None = None
    throttle_pos_pct: float | None = None
    vehicle_speed_kmh: float | None = None
    fuel_trim_short_pct: float | None = None
    fuel_trim_long_pct: float | None = None
    map_kpa: float | None = None


class DiagnosticAnalyzeRequest(BaseModel):
    vehicle_info: dict  # {make, model, year, engine_type, mileage}
    codes: list[str]  # ["P0420", "P0171"]
    symptoms: str = ""
    freeze_frame: FreezeFrameB2B = FreezeFrameB2B()
    save_as_case: bool = True


# --- Response ---

class ProbableCause(BaseModel):
    description: str
    probability: int
    reasoning: str
    supporting_evidence: list[str]


class DiagnosticStep(BaseModel):
    step: int
    action: str
    tool: str  # visual | multimeter | oscilloscope | scanner | smoke_machine | other
    expected_result: str
    abnormal_result: str


class DiagnosticAnalyzeResponse(BaseModel):
    case_id: str | None = None
    probable_causes: list[ProbableCause]
    diagnostic_sequence: list[DiagnosticStep]
    estimated_labor_hours: float
    parts_likely_needed: list[str]
    tsb_references: list[str]
    urgency: str  # immediate | within_week | routine
    additional_notes: str
