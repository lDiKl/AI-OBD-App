.PHONY: up down dev migrate migrate-create seed test lint fmt

# --- Docker ---
up:
	docker compose up -d postgres redis

down:
	docker compose down

dev:
	docker compose up -d postgres redis
	uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# --- Database ---
migrate:
	alembic upgrade head

migrate-create:
	@read -p "Migration name: " name; alembic revision --autogenerate -m "$$name"

migrate-down:
	alembic downgrade -1

migrate-status:
	alembic current

# --- Data ---
seed:
	python scripts/seed_dtc.py

# --- Tests ---
test:
	pytest tests/ -v

test-cov:
	pytest tests/ -v --cov=app --cov-report=html

# --- Code quality ---
lint:
	ruff check app/ scripts/ tests/

fmt:
	ruff format app/ scripts/ tests/

# --- One-shot setup for new dev ---
setup: up
	sleep 3
	$(MAKE) migrate
	$(MAKE) seed
	@echo "Backend ready at http://localhost:8000"
	@echo "API docs at http://localhost:8000/docs"
