# AI OBD Ecosystem

Two interconnected automotive AI applications on a shared backend.

## Apps

| App | Platform | Audience | Folder |
|-----|----------|----------|--------|
| DriverAI (B2C) | Android (Kotlin) | Car owners | `mobile-b2c/` |
| ShopAI Mobile (B2B) | Android (Kotlin) | Mechanics | `mobile-b2b/` |
| ShopAI Web (B2B) | React + TypeScript | Auto service dashboard | `web/` |
| Backend API | FastAPI (Python) | Shared for all apps | `backend/` |

## Structure

```
├── mobile-b2c/   Android B2C — OBD scan, AI explanation, risk assessment
├── mobile-b2b/   Android B2B — case management, on-site diagnostics
├── web/          React B2B SaaS — full dashboard, reports, estimates
├── backend/      FastAPI — shared REST API, PostgreSQL, Redis, AI layer
├── shared/       OpenAPI spec, shared data schemas
└── docs/         Architecture, API contract, LLM prompts, DTC data
```

## Getting Started

- Backend: see `backend/README.md`
- Mobile: open `mobile-b2c/` or `mobile-b2b/` in Android Studio
- Web: see `web/README.md`

## Docs

- `docs/development_plan.md` — full architecture and tech decisions
- `docs/api_contract.md` — all API endpoints
- `docs/prompts.md` — LLM prompts (6 prompts, routing logic)
- `docs/dtc_codes.csv` — 3,071 OBD-II DTC codes
