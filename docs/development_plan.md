# AI OBD App — Implementation Plan

> **Как пользоваться:**
> - `[ ]` — не начато | `[x]` — готово | `[-]` — в процессе
> - Каждый этап начинаем только после завершения предыдущего milestone
> - Детали архитектуры, AI стратегии, DB схемы — в [architecture.md](architecture.md)
> - API контракт (все endpoints) — в [api_contract.md](api_contract.md)
> - LLM промпты — в [prompts.md](prompts.md)

---

## Текущий статус

| Компонент | Статус |
|-----------|--------|
| Monorepo scaffold | ✅ Готово (commit a4f4c62) |
| mobile-b2c — OBD Bluetooth слой | ✅ Готово |
| mobile-b2c — Firebase Auth + Navigation | ✅ Готово, вход/выход работает |
| mobile-b2b — scaffold | ✅ Scaffold создан, компилируется |
| backend/ Phase 1 | ✅ Rule Engine, Vehicles CRUD, работает в Docker |
| backend/ Phase 2.1 AI Layer | ✅ Claude Haiku + Redis cache + fallback |
| web/ | ⬜ Не начато |

---

## Фаза 1 — Backend Foundation

> **Milestone:** `curl -X POST /api/v1/b2c/scan/analyze -d '{"dtc_codes":["P0420"]}'`
> возвращает `severity + can_drive + cost_range` без AI, только Rule Engine

### 1.1 Проект и инфраструктура

- [x] Структура папок `backend/` (routers/, services/, models/, db/, core/, tests/)
- [x] `pyproject.toml` — зависимости: fastapi, uvicorn, sqlalchemy[asyncio], alembic, asyncpg, pydantic, redis, python-dotenv, firebase-admin, anthropic
- [x] `docker-compose.yml` — PostgreSQL 16 + Redis локально
- [x] `.env.example` — шаблон переменных (DATABASE_URL, REDIS_URL, FIREBASE_*, ANTHROPIC_API_KEY)
- [x] `Makefile` — команды: `make dev`, `make migrate`, `make seed`, `make test`
- [x] FastAPI app factory `core/app.py` + `main.py`

### 1.2 База данных

- [x] Alembic init + `alembic.ini`
- [x] Миграция 0001: все основные таблицы (error_codes, repair_cost_estimates, users, vehicles, scan_sessions, error_occurrences)
- [ ] Миграция 0002: таблицы `shops`, `shop_users`, `diagnostic_cases` (Phase 3, B2B)
- [ ] Миграция 0003: таблицы `service_leads`, `shop_quotes` (Phase 4, Integration)
- [x] Seeding: скрипт `scripts/seed_dtc.py` — загрузить `docs/dtc_codes.csv` в `error_codes`
- [ ] Seeding: базовые `repair_cost_estimates` для топ-100 кодов по регионам (EU, UA)

### 1.3 Firebase Auth middleware

- [x] `core/security.py` — инициализация Firebase Admin SDK
- [x] Dependency `get_current_user(token)` — верификация Bearer токена → возвращает User из БД (auto-provision при первом входе)
- [x] `POST /api/v1/auth/verify` — создать/найти запись в `users` при первом входе
- [ ] Тест: верный токен → 200, неверный → 401

### 1.4 Rule Engine

- [x] `services/ai/rule_engine.py` — `evaluate_code()`, `evaluate_session()`
- [x] Severity overrides для критических кодов (P0300 misfire, C0035 ABS, etc.)
- [x] System-based severity defaults (brakes=high, engine=medium, body=low)
- [ ] `get_cost_range(code, region)` — из таблицы `repair_cost_estimates` (после seeding данных)
- [ ] Тесты: P0420 → severity=medium, P0300 → severity=critical

### 1.5 B2C Scan endpoint (без AI)

- [x] Pydantic схемы: `ScanAnalyzeRequest`, `ScanAnalyzeResponse`, `CodeResult`, `CodeResultFree`
- [x] `POST /api/v1/b2c/scan/analyze` — Rule Engine + DB lookup, создаёт `scan_session` + `error_occurrences`
- [ ] `GET /api/v1/b2c/scan/sessions` — список с пагинацией
- [ ] `GET /api/v1/b2c/scan/sessions/{id}` — детали сессии
- [x] `GET /api/v1/shared/dtc/{code}` — справочник кода
- [x] `GET /api/v1/shared/dtc/search?q=...` — поиск кодов

### 1.6 Vehicles CRUD

- [x] `POST /api/v1/b2c/vehicles` — добавить авто
- [x] `GET /api/v1/b2c/vehicles` — список авто пользователя
- [x] `PUT /api/v1/b2c/vehicles/{id}` — обновить
- [x] `DELETE /api/v1/b2c/vehicles/{id}` — удалить

**✅ Milestone 1 достигнут, когда:** POST /analyze с кодами возвращает Rule Engine результат

---

## Фаза 2 — B2C Mobile + AI Backend

> **Milestone:** Телефон → OBD сканирование → API → AI объяснение на экране

### 2.1 AI Layer на бэкенде

- [x] `services/ai/ai_service.py` — `explain_code_b2c()` + `B2CCodeContext`
- [x] Промпт из `docs/prompts.md` (B2C prompt #1 — plain language explanation)
- [x] Redis кэш: md5(`b2c:{code}:{make}:{year}:{region}:{lang}`), TTL = 7 дней
- [x] `premium` поле заполняется только если `subscription_status == "premium"`
- [x] Graceful fallback: AI недоступен → Rule Engine результат без ошибки для пользователя
- [x] Concurrent AI: все коды сессии обрабатываются параллельно через `asyncio.gather`
- [ ] Тест: P0420 + Toyota Camry 2018 → AI объяснение (нужен ANTHROPIC_API_KEY в .env)

### 2.2 Firebase Auth в mobile-b2c

- [x] Добавить `google-services.json` (получить из Firebase Console)
- [x] Раскомментировать Firebase в `app/build.gradle.kts`
- [x] `FirebaseAuthRepository` — login, logout, getIdToken
- [x] Экран Login: email/password форма + переключение Login↔Register
- [x] `AuthViewModel` — signIn, register, AuthState sealed class
- [x] `AuthInterceptor` — автоматическое добавление `Authorization: Bearer {token}` в Retrofit
- [x] `NetworkModule` — Hilt: OkHttpClient + Retrofit + ApiService
- [x] `AppNavHost` — навигация Login → Scanner (автологин если сессия активна)
- [x] Кнопка Sign Out в TopAppBar ScannerScreen

### 2.3 Vehicle Management в mobile-b2c

- [x] Room entity `VehicleEntity` + DAO + `AppDatabase` + `DatabaseModule`
- [x] Retrofit `VehicleApiService` → `POST/GET/DELETE /api/v1/b2c/vehicles`
- [x] `VehicleRepository` — Room как source of truth, sync с backend
- [x] `VehicleViewModel` — список авто, добавление, удаление, sync
- [x] `VehicleScreen` — LazyColumn + FAB + диалог добавления (make/model/year/engine/mileage/VIN)
- [x] `AppNavHost` — BottomNavigationBar: Scanner | My Cars

### 2.4 OBD Scan → Backend

- [x] Retrofit `ScanApiService` + DTOs (FreezeFrameRequest, ScannedCodeRequest, ScanAnalyzeResponse)
- [x] `ScanRepository` — маппер `ObdScanResult` → `ScanAnalyzeRequest`
- [x] `NetworkModule` — snake_case Gson naming policy (camelCase ↔ snake_case автоматически)
- [x] `ScannerViewModel` — новые состояния: `Analyzing`, `AnalysisReady`; inject VehicleRepository
- [x] `ScannerScreen` — "Analyze with AI" кнопка после OBD скана; `Analyzing` loading state
- [x] `AnalysisResultContent` — overall risk card + per-code severity badge + can_drive
- [x] Premium: AI объяснение + main_causes + recommended_action
- [x] Free tier: upsell блок "Upgrade to Premium"

### 2.5 История сканов

- [ ] Room таблица `ScanSessionEntity` + `ErrorOccurrenceEntity`
- [ ] Синхронизация: при получении результата от API → сохранить в Room
- [ ] Экран "История" — `LazyColumn` сессий по датам
- [ ] Экран деталей сессии — все коды + AI объяснения

**✅ Milestone 2 достигнут, когда:** Реальное OBD сканирование → AI объяснение на экране телефона

---

## Фаза 3 — B2B Web MVP

> **Milestone:** Механик вводит P0420 + Toyota Camry 2018 → получает анализ → генерирует PDF отчёт

### 3.1 B2B Backend endpoints

- [ ] B2B Auth middleware — `ShopUser` вместо `User`, привязка к `shop_id`
- [ ] `POST /api/v1/b2b/auth/register-shop` — регистрация сервиса
- [ ] `services/ai_service_b2b.py` — B2B промпт: ranked causes + diagnostic checklist
- [ ] `POST /api/v1/b2b/diagnostic/analyze` — полный B2B анализ (probable_causes, checklist, labor_hours)
- [ ] `POST /api/v1/b2b/cases/{id}/report/generate` — AI генерация текста отчёта для клиента
- [ ] `GET/POST /api/v1/b2b/cases` — создание и список кейсов
- [ ] `GET/PUT /api/v1/b2b/cases/{id}` — деталb и обновление статуса

### 3.2 PDF генерация

- [ ] Python библиотека `weasyprint` или `reportlab`
- [ ] HTML шаблон клиентского отчёта (Jinja2)
- [ ] HTML шаблон сметы
- [ ] `GET /api/v1/b2b/cases/{id}/report/pdf` — скачать PDF отчёт
- [ ] `GET /api/v1/b2b/cases/{id}/estimate/pdf` — скачать PDF смету

### 3.3 React проект setup

- [ ] `web/` — Vite + React + TypeScript init
- [ ] Tailwind CSS + shadcn/ui компоненты
- [ ] TanStack Query — глобальный QueryClient
- [ ] Firebase Auth JS SDK — login + token refresh
- [ ] React Router — layout + protected routes
- [ ] Axios instance с `Authorization: Bearer {token}` interceptor
- [ ] TypeScript типы из `docs/api_contract.md`

### 3.4 Web Auth + Shop Setup

- [ ] Страница Login (email + Google)
- [ ] Страница Register Shop (после первого входа если нет shop)
- [ ] Auth context + `useCurrentUser` hook
- [ ] Shop Profile страница (`GET/PUT /api/v1/b2b/shop/profile`)

### 3.5 Web Dashboard — главные страницы

- [ ] Layout: sidebar + header + main content
- [ ] Dashboard (главная): активные кейсы + кнопка "Новая диагностика"
- [ ] Страница "Новая диагностика":
  - [ ] Форма: make/model/year/engine/mileage/vin
  - [ ] Поле ввода DTC кодов (multiple, comma-separated или список)
  - [ ] Поле симптомов (textarea)
  - [ ] Кнопка "Analyze" → вызов `/b2b/diagnostic/analyze`
  - [ ] Loading state + результат
- [ ] Страница результатов AI анализа:
  - [ ] Probable causes (ranked, с % уверенности + reasoning)
  - [ ] Diagnostic checklist (интерактивный, отметить выполненное)
  - [ ] Кнопка "Сохранить как кейс"
  - [ ] Кнопка "Сгенерировать отчёт"
- [ ] Список кейсов: поиск + фильтр по статусу/коду/модели
- [ ] Страница кейса: полный анализ + статус + заметки

### 3.6 Report + Estimate в вебе

- [ ] Страница генерации отчёта: AI текст + редактор (editable)
- [ ] Кнопка "Скачать PDF отчёт"
- [ ] Estimate Builder:
  - [ ] Добавление запчастей (название, цена, кол-во)
  - [ ] Labor hours (AI предложение, редактируемое)
  - [ ] Markup % + итог
  - [ ] Кнопка "Скачать PDF смету"
- [ ] `POST /api/v1/b2b/cases/{id}/estimate` — сохранить смету

**✅ Milestone 3 достигнут, когда:** Веб-дашборд → ввод кодов → AI анализ → PDF отчёт

---

## Фаза 4 — Монетизация

> **Milestone:** Тестовая оплата через Stripe → subscription_status = "premium" → AI разблокирован

### 4.1 Stripe Backend

- [ ] Stripe SDK + `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` в .env
- [ ] Создать Price IDs в Stripe Dashboard (B2C monthly, B2B basic, B2B pro)
- [ ] `POST /api/v1/b2c/subscription/checkout` → Stripe Checkout session URL
- [ ] `POST /api/v1/b2b/subscription/checkout` → Stripe Checkout session URL
- [ ] `POST /api/v1/webhooks/stripe/b2c` → обновить `users.subscription_status`
- [ ] `POST /api/v1/webhooks/stripe/b2b` → обновить `shops.subscription_tier`
- [ ] `GET /api/v1/b2c/subscription/status` — статус подписки
- [ ] `DELETE /api/v1/b2c/subscription` — отмена

### 4.2 B2C Paywall в приложении

- [ ] Проверка `subscription_status` перед показом premium контента
- [ ] Экран "Upgrade": описание планов + кнопка
- [ ] Нажатие → открыть Stripe Checkout в браузере (`CustomTabsIntent`)
- [ ] При возврате в приложение → refresh subscription status
- [ ] Free tier: только код + severity (без AI объяснения)
- [ ] Premium tier: полный AI анализ + cost estimation + history

### 4.3 B2B Subscription в вебе

- [ ] Страница Billing: текущий план, следующий платёж, история
- [ ] Upgrade flow → Stripe Checkout
- [ ] Basic vs Pro ограничения (количество кейсов/месяц, team members)
- [ ] Team Management страница (Pro): invite механика по email

**✅ Milestone 4 достигнут, когда:** Реальная оплата → функции разблокированы

---

## Фаза 5 — B2B Android

> **Milestone:** Механик со смартфоном сканирует OBD → создаёт кейс → видит professional AI анализ

- [ ] Firebase Auth B2B (ShopUser тип — отдельный flow от B2C)
- [ ] OBD слой — переиспользовать `data/obd/` из mobile-b2c (копия или shared module)
- [ ] `DiagnosticCaseRepository` — создание кейса из OBD скана
- [ ] Экран "Scan + Create Case": поле клиента/авто + кнопка создать кейс
- [ ] Показать AI анализ (B2B формат): ranked causes + checklist
- [ ] Список кейсов с синхронизацией (Room + backend)
- [ ] Offline mode: кэшировать кейсы в Room, sync при восстановлении сети
- [ ] Push уведомления (FCM) — для будущих лидов

**✅ Milestone 5 достигнут, когда:** B2B Android создаёт кейс из OBD скана

---

## Фаза 6 — Integration Bridge *(после 5,000+ B2C MAU)*

> **Milestone:** B2C пользователь отправляет диагностику → B2B сервис получает лид в dashboard

### 6.1 B2C → найти сервис

- [ ] `GET /api/v1/b2c/shops/nearby?lat=&lng=&radius_km=` — список сервисов рядом
- [ ] Экран "Найти сервис" в B2C приложении (список или карта)
- [ ] Кнопка "Отправить диагностику в сервис"

### 6.2 Lead System

- [ ] `POST /api/v1/b2c/leads` — отправить `scan_session` в сервис (с согласия пользователя)
- [ ] `GET /api/v1/b2c/leads` — мои отправленные лиды (статус, ответы)
- [ ] `GET /api/v1/b2b/leads` — входящие лиды в B2B dashboard
- [ ] B2B Web: страница лидов — принять/отклонить/отправить смету
- [ ] `PUT /api/v1/b2b/leads/{id}/quote` — ответить сметой
- [ ] B2C: уведомление о полученной смете

### 6.3 Геосервис

- [ ] PostGIS или простые `lat/lng` запросы в PostgreSQL
- [ ] Верификация сервисов (ручная на старте)
- [ ] Профили сервисов видны B2C пользователям

**✅ Milestone 6 достигнут, когда:** Полный цикл лида: B2C → лид → смета → запись

---

## Справочные документы

| Документ | Содержание |
|----------|-----------|
| [architecture.md](architecture.md) | Полная архитектура, AI стратегия, LLM Router, схема БД |
| [api_contract.md](api_contract.md) | Все API endpoints с request/response схемами |
| [prompts.md](prompts.md) | 6 LLM промптов для B2C и B2B |
| [AI_OBD_Diagnostic_MVP.md](AI_OBD_Diagnostic_MVP.md) | B2C MVP спецификация |
| [AI_Auto_Service_Assistant_MVP.md](AI_Auto_Service_Assistant_MVP.md) | B2B MVP спецификация |
| [AI_OBD_Diagnostic_Production_Strategy.md](AI_OBD_Diagnostic_Production_Strategy.md) | B2C роадмап, unit economics |

---

*Последнее обновление: 2026-03-02*
*Текущий фокус: Фаза 2.4 — OBD Scan → Backend*
