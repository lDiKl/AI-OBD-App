# Backend — AI OBD Ecosystem API

FastAPI (Python 3.11+) shared backend for all three clients.

## Structure

```
app/
├── core/          config, database, firebase auth
├── api/v1/
│   ├── b2c/       scan/analyze, vehicles, sessions, subscription
│   ├── b2b/       diagnostic/analyze, cases, reports, estimates
│   └── shared/    dtc lookup
├── models/        SQLAlchemy ORM (PostgreSQL)
├── schemas/       Pydantic request/response models
└── services/ai/   Rule Engine → RAG → LLM Router → Formatter
```

## Setup

```bash
# 1. Copy env file
cp .env.example .env
# Edit .env with your values

# 2. Install dependencies
pip install -e ".[dev]"

# 3. Run (dev)
uvicorn app.main:app --reload

# 4. API docs
open http://localhost:8000/docs
```

## Requirements

- Python 3.11+
- PostgreSQL 15+ with pgvector extension
- Redis 7+
- Firebase project (for auth)

## Key endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/b2c/scan/analyze` | Main B2C scan analysis |
| POST | `/api/v1/b2b/diagnostic/analyze` | Main B2B diagnostic |
| GET | `/api/v1/shared/dtc/{code}` | DTC code lookup |
| POST | `/api/v1/b2c/subscription/checkout` | Stripe checkout |
| POST | `/api/v1/b2c/subscription/webhook` | Stripe webhook |

Full API contract: `../docs/api_contract.md`
