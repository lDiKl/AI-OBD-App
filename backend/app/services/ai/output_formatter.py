"""
Layer 4: Output Formatter — validates and formats LLM response.

If LLM returns invalid JSON → fallback to default template.
"""

import json


def parse_llm_response(raw: str, prompt_type: str) -> dict:
    """
    Parse and validate LLM JSON output.
    Falls back to safe default if parsing fails.
    """
    try:
        data = json.loads(raw)
        return data
    except (json.JSONDecodeError, ValueError):
        return get_fallback_response(prompt_type)


def get_fallback_response(prompt_type: str) -> dict:
    """Return a safe fallback response when LLM output is invalid."""
    fallbacks = {
        "b2c_single": {
            "simple_explanation": "This error code indicates a potential issue with your vehicle. Professional inspection is recommended.",
            "main_causes": ["Unknown — professional diagnosis needed"],
            "causes_probability": [100],
            "what_happens_if_ignored": "The issue may worsen over time. Monitor vehicle behavior.",
            "recommended_action": "Schedule a diagnostic inspection at a qualified auto service.",
        },
        "b2c_summary": {
            "overall_message": "Multiple error codes detected. Professional inspection recommended.",
            "priority_code": "",
            "priority_reason": "Professional diagnosis required to determine priority.",
            "safe_to_drive": False,
            "drive_conditions": None,
        },
        "b2b_diagnostic": {
            "probable_causes": [],
            "diagnostic_sequence": [],
            "estimated_labor_hours": 0.0,
            "parts_likely_needed": [],
            "tsb_references": [],
            "urgency": "within_week",
            "additional_notes": "AI analysis failed. Manual diagnostic required.",
        },
        "b2b_report": {
            "report_title": "Vehicle Diagnostic Report",
            "issue_summary": "A diagnostic issue was identified during inspection.",
            "what_we_did": "Diagnostic inspection was performed.",
            "why_it_matters": "Addressing this issue helps maintain vehicle reliability.",
            "parts_replaced": [],
            "next_steps": "Please contact us to discuss the repair.",
            "disclaimer": "This report is based on diagnostic data obtained during inspection.",
        },
        "b2b_estimate": {
            "suggested_parts": [],
            "suggested_labor_hours": 1.0,
            "labor_notes": "Estimate requires manual review.",
            "typical_total_range": {"min": 0, "max": 0, "currency": "EUR"},
        },
    }
    return fallbacks.get(prompt_type, {})
