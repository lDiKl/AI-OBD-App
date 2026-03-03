"""PDF generation for B2B reports and estimates using WeasyPrint."""
from __future__ import annotations

import json
from typing import TYPE_CHECKING

from weasyprint import HTML

if TYPE_CHECKING:
    from app.models.diagnostic_case import DiagnosticCase
    from app.models.shop import Shop


# ── Shared CSS ────────────────────────────────────────────────────────────────

_CSS = """
* { box-sizing: border-box; margin: 0; padding: 0; }
body {
    font-family: Arial, Helvetica, sans-serif;
    font-size: 13px;
    color: #1f2937;
    padding: 40px 48px;
    line-height: 1.5;
}
.header { border-bottom: 2px solid #2563eb; padding-bottom: 14px; margin-bottom: 24px; }
.shop-name { font-size: 22px; font-weight: bold; color: #1e40af; }
.shop-meta { font-size: 11px; color: #6b7280; margin-top: 2px; }
.doc-title { font-size: 17px; font-weight: bold; margin-bottom: 4px; }
.vehicle-line { font-size: 12px; color: #4b5563; margin-bottom: 16px; }
h3 { font-size: 13px; font-weight: bold; color: #374151;
     border-bottom: 1px solid #e5e7eb; padding-bottom: 4px; margin: 20px 0 8px; }
p { margin-bottom: 6px; }
ul { padding-left: 18px; margin-bottom: 6px; }
li { margin-bottom: 3px; }
table { width: 100%; border-collapse: collapse; margin-top: 8px; }
th { background: #eff6ff; color: #1e40af; font-size: 11px;
     text-align: left; padding: 6px 8px; border: 1px solid #dbeafe; }
td { padding: 6px 8px; border: 1px solid #e5e7eb; font-size: 12px; vertical-align: top; }
tr:nth-child(even) td { background: #f9fafb; }
.total-row td { font-weight: bold; background: #eff6ff; }
.disclaimer { margin-top: 28px; font-size: 11px; color: #9ca3af;
              border-top: 1px solid #e5e7eb; padding-top: 10px; }
.footer { margin-top: 32px; font-size: 11px; color: #6b7280; text-align: center; }
"""


def _shop_header(shop: "Shop") -> str:
    meta_parts = [p for p in [getattr(shop, "address", None), getattr(shop, "phone", None)] if p]
    meta = " &nbsp;·&nbsp; ".join(meta_parts)
    return f"""
    <div class="header">
        <div class="shop-name">{_esc(shop.name)}</div>
        {f'<div class="shop-meta">{_esc(meta)}</div>' if meta else ''}
    </div>
    """


def _vehicle_line(vehicle_info: dict) -> str:
    v = vehicle_info or {}
    parts = [
        f"{v.get('year', '')} {v.get('make', '')} {v.get('model', '')}".strip(),
        f"Engine: {v['engine']}" if v.get("engine") else "",
        f"VIN: {v['vin']}" if v.get("vin") else "",
        f"Mileage: {v['mileage']} km" if v.get("mileage") else "",
    ]
    return " &nbsp;·&nbsp; ".join(p for p in parts if p)


def _esc(text: str | None) -> str:
    """Minimal HTML escaping."""
    if not text:
        return ""
    return (
        str(text)
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
    )


# ── Report PDF ────────────────────────────────────────────────────────────────

def generate_report_pdf(case: "DiagnosticCase", shop: "Shop") -> bytes:
    """Render client-facing service report as PDF bytes."""
    raw = case.client_report_text or "{}"
    report: dict = json.loads(raw) if isinstance(raw, str) else (raw or {})

    parts_replaced = report.get("parts_replaced") or []
    if isinstance(parts_replaced, str):
        parts_replaced = [parts_replaced]

    parts_html = (
        "<ul>" + "".join(f"<li>{_esc(p)}</li>" for p in parts_replaced) + "</ul>"
        if parts_replaced
        else "<p>—</p>"
    )

    html = f"""
    <!DOCTYPE html><html><head>
    <meta charset="utf-8">
    <style>{_CSS}</style>
    </head><body>
    {_shop_header(shop)}

    <div class="doc-title">{_esc(report.get('report_title', 'Service Report'))}</div>
    <div class="vehicle-line">{_vehicle_line(case.vehicle_info)}</div>

    <h3>Issue Summary</h3>
    <p>{_esc(report.get('issue_summary', ''))}</p>

    <h3>What We Did</h3>
    <p>{_esc(report.get('what_we_did', ''))}</p>

    <h3>Why It Matters</h3>
    <p>{_esc(report.get('why_it_matters', ''))}</p>

    <h3>Parts Replaced</h3>
    {parts_html}

    <h3>Next Steps</h3>
    <p>{_esc(report.get('next_steps', ''))}</p>

    <div class="disclaimer">{_esc(report.get('disclaimer', ''))}</div>
    <div class="footer">{_esc(shop.name)}</div>
    </body></html>
    """

    return HTML(string=html).write_pdf()


# ── Estimate PDF ──────────────────────────────────────────────────────────────

def generate_estimate_pdf(case: "DiagnosticCase", shop: "Shop") -> bytes:
    """Render mechanic estimate as PDF bytes."""
    est: dict = case.estimate or {}
    parts: list[dict] = est.get("parts", [])
    currency = est.get("currency", "EUR")
    sym = "€" if currency == "EUR" else currency

    parts_rows = ""
    for p in parts:
        qty = p.get("quantity", 1)
        unit = p.get("unit_price", 0)
        total = qty * unit
        parts_rows += f"""
        <tr>
            <td>{_esc(p.get('name', ''))}</td>
            <td style="text-align:center">{qty}</td>
            <td style="text-align:right">{sym}{unit:.2f}</td>
            <td style="text-align:right">{sym}{total:.2f}</td>
            {f'<td>{_esc(p.get("note",""))}</td>' if p.get("note") else '<td></td>'}
        </tr>
        """

    labor_hours = est.get("labor_hours", 0)
    labor_rate = est.get("labor_rate_eur", 65)
    labor_total = est.get("labor_total", labor_hours * labor_rate)
    parts_total = est.get("parts_total", 0)
    subtotal = est.get("subtotal", parts_total + labor_total)
    markup_pct = est.get("markup_pct", 0)
    total = est.get("total", subtotal)
    notes = est.get("notes", "")

    html = f"""
    <!DOCTYPE html><html><head>
    <meta charset="utf-8">
    <style>{_CSS}</style>
    </head><body>
    {_shop_header(shop)}

    <div class="doc-title">Repair Estimate</div>
    <div class="vehicle-line">{_vehicle_line(case.vehicle_info)}</div>

    <h3>Parts &amp; Materials</h3>
    <table>
        <thead><tr><th>Part / Material</th><th>Qty</th><th>Unit Price</th><th>Total</th><th>Note</th></tr></thead>
        <tbody>
            {parts_rows if parts_rows else '<tr><td colspan="5">No parts listed</td></tr>'}
        </tbody>
    </table>

    <h3>Labour</h3>
    <table>
        <thead><tr><th>Description</th><th>Hours</th><th>Rate / hr</th><th>Total</th><th></th></tr></thead>
        <tbody>
            <tr>
                <td>Labour</td>
                <td style="text-align:center">{labor_hours:.1f}</td>
                <td style="text-align:right">{sym}{labor_rate:.2f}</td>
                <td style="text-align:right">{sym}{labor_total:.2f}</td>
                <td></td>
            </tr>
        </tbody>
    </table>

    <h3>Summary</h3>
    <table style="width:320px; margin-left:auto">
        <tbody>
            <tr><td>Parts total</td><td style="text-align:right">{sym}{parts_total:.2f}</td></tr>
            <tr><td>Labour total</td><td style="text-align:right">{sym}{labor_total:.2f}</td></tr>
            <tr><td>Subtotal</td><td style="text-align:right">{sym}{subtotal:.2f}</td></tr>
            {f'<tr><td>Markup ({markup_pct:.0f}%)</td><td style="text-align:right">{sym}{total - subtotal:.2f}</td></tr>' if markup_pct else ''}
            <tr class="total-row"><td>TOTAL ({currency})</td><td style="text-align:right">{sym}{total:.2f}</td></tr>
        </tbody>
    </table>

    {f'<h3>Notes</h3><p>{_esc(notes)}</p>' if notes else ''}

    <div class="footer">{_esc(shop.name)}</div>
    </body></html>
    """

    return HTML(string=html).write_pdf()
