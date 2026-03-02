"""
Layer 1: Rule Engine — deterministic logic, zero hallucinations.

Handles: severity, can_drive, overall_risk.
These are safety-critical — never delegated to LLM.
"""

from dataclasses import dataclass

# Severity rules based on DTC category and system
SEVERITY_OVERRIDES: dict[str, str] = {
    # Critical — stop driving immediately
    "P0300": "critical",  # Random misfire
    "P0301": "critical",  # Cylinder 1 misfire
    "P0302": "critical",  # Cylinder 2 misfire
    "P0303": "critical",  # Cylinder 3 misfire
    "P0304": "critical",  # Cylinder 4 misfire
    "P0217": "critical",  # Engine overtemp
    "P0563": "high",      # System voltage high
    "C0035": "critical",  # Wheel speed sensor (ABS/brake)
    "C0040": "critical",  # Wheel speed sensor (ABS/brake)
}

CAN_DRIVE_RULES: dict[str, str] = {
    "critical": "no",
    "high": "limited",
    "medium": "yes_within_2_weeks",
    "low": "yes",
}

SYSTEM_SEVERITY_DEFAULTS: dict[str, str] = {
    "brakes": "high",
    "engine": "medium",
    "emissions": "medium",
    "transmission": "medium",
    "electrical": "low",
    "body": "low",
    "network": "low",
}


@dataclass
class RuleEngineResult:
    code: str
    severity: str
    can_drive: str
    system: str


def evaluate_code(code: str, system: str = "unknown") -> RuleEngineResult:
    """
    Determine severity and can_drive for a single DTC code.
    Uses static override table first, then system defaults.
    """
    if code in SEVERITY_OVERRIDES:
        severity = SEVERITY_OVERRIDES[code]
    else:
        severity = SYSTEM_SEVERITY_DEFAULTS.get(system, "medium")

    can_drive = CAN_DRIVE_RULES[severity]
    return RuleEngineResult(code=code, severity=severity, can_drive=can_drive, system=system)


def evaluate_session(codes: list[dict]) -> str:
    """
    Calculate overall risk for a scan session with multiple codes.
    Returns: low | medium | high | critical
    """
    if not codes:
        return "low"

    severity_order = {"low": 0, "medium": 1, "high": 2, "critical": 3}
    max_severity = max(codes, key=lambda c: severity_order.get(c.get("severity", "low"), 0))
    return max_severity.get("severity", "low")
