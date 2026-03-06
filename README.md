# AI OBD Ecosystem

Two interconnected automotive AI applications on a shared backend.

## Apps

| App | Platform | Audience | Folder |
|-----|----------|----------|--------|
| Avyrox Drive (B2C) | Android (Kotlin) | Car owners | `avyrox-drive/` |
| Avyrox Service (B2B) | Android (Kotlin) | Mechanics | `avyrox-service/` |
| Avyrox Cloud (B2B) | React + TypeScript | Auto service dashboard | `web/` |
| Avyrox API | FastAPI (Python) | Shared for all apps | `backend/` |

## Structure

```
├── avyrox-drive/ Android B2C — OBD scan, AI explanation, risk assessment (Avyrox Drive)
├── avyrox-service/ Android B2B — case management, on-site diagnostics
├── web/          React B2B SaaS — full dashboard, reports, estimates
├── backend/      FastAPI — shared REST API, PostgreSQL, Redis, AI layer
├── shared/       OpenAPI spec, shared data schemas
└── docs/         Architecture, API contract, LLM prompts, DTC data
```

## Getting Started

- Backend: see `backend/README.md`
- Mobile: open `avyrox-drive/` or `avyrox-service/` in Android Studio
- Web: see `web/README.md`

## Docs

- `docs/development_plan.md` — full architecture and tech decisions
- `docs/api_contract.md` — all API endpoints
- `docs/prompts.md` — LLM prompts (6 prompts, routing logic)
- `docs/dtc_codes.csv` — 3,071 OBD-II DTC codes
