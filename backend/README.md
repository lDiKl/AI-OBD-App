# Backend — AI OBD Ecosystem API

FastAPI (Python 3.11+) shared backend for all three clients (B2C mobile, B2B mobile, B2B web).

## Structure

```
app/
├── core/          config, database, firebase auth
├── api/v1/
│   ├── auth/      token verify, user provisioning
│   ├── b2c/       scan/analyze, vehicles, sessions, subscription
│   ├── b2b/       diagnostic/analyze, cases, reports, estimates
│   └── shared/    dtc lookup + search
├── models/        SQLAlchemy ORM (PostgreSQL)
├── schemas/       Pydantic request/response models
└── services/ai/   Rule Engine → RAG → LLM Router → Formatter
```

---

## Quick Start (Docker — recommended)

> Prerequisites: Docker Desktop running

### 1. First-time setup

```bash
# From the backend/ directory:

# Build and start all services (PostgreSQL, Redis, API)
docker compose up -d --build

# Apply database migrations (run once, or after new migrations)
docker compose exec api alembic upgrade head

# Seed DTC codes database (run once — loads 3,071 fault codes)
docker compose exec api python scripts/seed_dtc.py
```

### 2. Daily workflow

```bash
# Start everything
docker compose up -d

# Stop everything
docker compose down

# Restart only the API (after code changes that --reload doesn't catch)
docker compose restart api

# View API logs
docker compose logs api -f
```

### 3. After pulling new code

```bash
# Rebuild image if pyproject.toml changed
docker compose up -d --build

# Apply new migrations (if any were added)
docker compose exec api alembic upgrade head
```

---

## Ports

| Service    | Host port | Notes                        |
|------------|-----------|------------------------------|
| API        | 8000      | FastAPI + uvicorn --reload   |
| PostgreSQL | 5433      | Internal: 5432               |
| Redis      | 6380      | Internal: 6379               |

---

## Useful URLs (when running)

| URL | Description |
|-----|-------------|
| http://localhost:8000/docs | Swagger UI — interactive API docs |
| http://localhost:8000/redoc | ReDoc — alternative API docs |
| http://localhost:8000/health | Health check endpoint |

---

## Database migrations

```bash
# Apply all pending migrations
docker compose exec api alembic upgrade head

# Check current migration version
docker compose exec api alembic current

# Show migration history
docker compose exec api alembic history

# Rollback one migration
docker compose exec api alembic downgrade -1

# Create a new migration (after changing models)
docker compose exec api alembic revision --autogenerate -m "description"
```

---

## Key endpoints (Phase 1)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/health` | — | Health check |
| POST | `/api/v1/auth/verify` | Bearer | Verify Firebase token, provision user |
| GET | `/api/v1/b2c/vehicles/` | Bearer | List user vehicles |
| POST | `/api/v1/b2c/vehicles/` | Bearer | Add vehicle |
| PUT | `/api/v1/b2c/vehicles/{id}` | Bearer | Update vehicle |
| DELETE | `/api/v1/b2c/vehicles/{id}` | Bearer | Delete vehicle |
| POST | `/api/v1/b2c/scan/analyze` | Bearer | Analyze OBD scan (Rule Engine) |
| GET | `/api/v1/shared/dtc/{code}` | Bearer | DTC code lookup |
| GET | `/api/v1/shared/dtc/search?q=P04` | Bearer | DTC code search |

Full API contract: `../docs/api_contract.md`

---

## Environment variables

Copy `.env.example` to `.env` and fill in your values (only needed for local non-Docker runs):

```bash
cp .env.example .env
```

In Docker, environment variables are set directly in `docker-compose.yml`.
Firebase credentials: place `firebase-credentials.json` in the `backend/` folder — it will be available at `/app/firebase-credentials.json` inside the container.

---

## Requirements

- Docker Desktop
- No local Python installation required for Docker workflow
