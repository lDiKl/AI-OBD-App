# Анализ связи между B2C и B2B приложениями

> Документ основан на:
> - `AI OBD Diagnostic Assistant/AI_OBD_Diagnostic_MVP.md`
> - `AI OBD Diagnostic Assistant/AI_OBD_Diagnostic_Production_Strategy.md`
> - `AI Assistant for Auto Services (SaaS)/AI_Auto_Service_Assistant_MVP.md`

---

## Обзор двух продуктов

| Параметр | B2C — AI OBD Diagnostic | B2B — AI Auto Service |
|---|---|---|
| Аудитория | Водители, покупатели б/у авто | Автосервисы, механики, флоты |
| Платформа | Android (Kotlin, mobile) | **Android (Kotlin)** + Web (React/TypeScript) |
| OBD | Bluetooth ELM327 (прямая интеграция) | ELM327 через B2B Android app; Web — ручной ввод |
| AI архитектура | Rule Engine + LLM (гибрид) | LLM + structured knowledge base |
| Монетизация | Freemium, 5–10€/мес подписка | 49€ / 99€ / мес подписка |
| MVP timeline | ~2–3 месяца | Android ~2–3 мес / Web ~3 мес |

---

## Важно: что НЕ входит в MVP обоих приложений

Согласно официальным документам, эти функции **намеренно исключены** из первых версий:

**B2C MVP — не включает:**
- Service marketplace integration ← **ключевое для связи**
- Photo analysis
- Sound diagnostics
- Advanced predictive analytics

**B2B MVP (Web Dashboard) — не включает:**
- Full parts supplier integration
- Automated VIN decoding
- AI learning from aggregated industry data ← **ключевое для связи**

**B2B MVP (Android App) — включает OBD**, но не включает:
- Live Data graphs (Phase 2)
- Поддержку OEM-специфичных протоколов (J2534 и т.п.)
- Расширенное программирование ЭБУ

**Вывод:** Интеграция между приложениями — это **пост-MVP функциональность**, которую нужно закладывать в архитектуру сейчас, но реализовывать позже.

---

## Официальная последовательность запуска

Согласно `AI_OBD_Diagnostic_Production_Strategy.md`:

```
Phase 0: Validation (2–3 недели)
  → Landing page + waitlist
  → Market validation via ads
  → Опрос водителей
  → Цель: 500+ пользователей в waitlist

Phase 1: Core MVP B2C (8–10 недель)
  → Flutter app + OBD + AI объяснения
  → Закрытая бета: 100–300 пользователей
  → Сбор реальных scan logs

Phase 2: Smart Intelligence Layer (6–8 недель)
  → Платная подписка (Stripe/RevenueCat)
  → VIN профиль, улучшенные оценки стоимости

Phase 3: Scale
  → Фото / звук диагностика
  → Региональный cost API
  → iOS + Android полный релиз

После 5,000+ активных пользователей:
  → Запуск B2B версии на основе собранных данных
  → Интеграция между приложениями
```

---

## Концепция связи: как это работает

### Главная идея

B2C собирает данные и аудиторию → B2B использует эти данные и получает клиентов через B2C.

```
[Водитель] ──── B2C App ────→ диагностика + объяснение
                    │
                    │ (после 5k users, Phase 3+)
                    ↓
           [Общий Backend API]
                    │
                    ↓
           [B2B App] ──── Сервис получает лид с отчётом
```

### Точка соединения: Diagnostic Report → Service Lead

**User flow (будущий, не MVP):**
1. Пользователь сканирует авто → AI объясняет проблему
2. Если Risk Level = Medium или High → появляется кнопка **"Найти сервис рядом"**
3. Геопоиск по зарегистрированным B2B сервисам по специализации
4. Пользователь отправляет `DiagnosticReport` в сервис одним нажатием
5. Механик открывает лид в B2B dashboard — видит коды, freeze frame, AI анализ
6. Сервис отправляет смету обратно → пользователь видит в B2C app
7. Запись на ремонт

**Ценность:**
- Водитель: не надо объяснять проблему механику
- Механик: готовая диагностика вместо первичного приёма
- Ты: комиссия за лид / запись

---

## Как расширить существующую схему БД для интеграции

Текущая схема из Production Strategy покрывает B2C:

```
Users → Vehicles → ScanSessions → ErrorOccurrences → ErrorCodes
                                                    → RepairCostEstimates
```

Для интеграции нужно добавить **три новые сущности**:

```sql
-- Зарегистрированные B2B сервисы
Shops
  id (UUID)
  name
  address
  lat, lng              -- для геопоиска
  phone, email
  specializations[]     -- типы ремонта: engine, transmission, brakes...
  subscription_tier     -- basic | pro
  rating
  created_at

-- Связующая сущность (лид)
ServiceLeads
  id (UUID)
  scan_session_id (FK → ScanSessions)  -- диагностика из B2C
  shop_id (FK → Shops)                 -- сервис из B2B
  user_id (FK → Users)
  status  -- new | viewed | quoted | booked | completed | declined
  quote_amount
  booking_time
  created_at
  updated_at

-- Сообщения/смета от сервиса
ShopQuotes
  id (UUID)
  lead_id (FK → ServiceLeads)
  message_text
  parts_cost
  labor_cost
  total_cost
  estimated_days
  created_at
```

Это минимальный набор для MVP интеграции.

---

## Данные, которые B2C передаёт B2B

При отправке лида сервис получает из `ScanSession`:

```json
{
  "vehicle": {
    "make": "Toyota",
    "model": "Corolla",
    "year": 2018,
    "engine_type": "1.6 petrol",
    "mileage": 95000
  },
  "scan_date": "2025-03-01",
  "error_codes": [
    {
      "code": "P0420",
      "description": "Catalyst System Efficiency Below Threshold",
      "risk_level": "Medium",
      "can_drive": "Yes, within 2 weeks",
      "ai_explanation": "Ваш катализатор работает неэффективно...",
      "estimated_repair_cost": { "min": 250, "max": 900, "currency": "EUR" },
      "freeze_frame": { "rpm": 1200, "coolant_temp": 88, "throttle": 15 }
    }
  ]
}
```

Это именно тот формат, который B2B AI уже умеет анализировать.

---

## Data Moat: как данные усиливают оба продукта

По мере роста B2C (обезличенные, агрегированные данные):

| Данные из B2C | Как используется в B2B |
|---|---|
| Частота кодов по моделям авто | Более точные вероятностные причины |
| Реальные цены ремонта (из завершённых лидов) | Замена статичных диапазонов реальными данными |
| Статистика повторных ошибок | Предупреждение механика о рецидивах |
| Региональная стоимость запчастей | Региональные сметы |

Это конкурентный барьер: чем больше пользователей → тем умнее AI → тем привлекательнее продукт.

---

## Монетизация связи

### Рекомендуется для старта: Lead Generation Fee

- Сервис платит за каждый входящий лид с подтверждённой диагностикой
- 3–5€ за лид — реалистично, это дешевле чем реклама
- Не требует сложной интеграции, можно внедрить как первый шаг

### Дополнительно позже:

| Модель | Описание | Когда |
|---|---|---|
| Featured Listing | Приоритет в геопоиске | После 20+ сервисов в сети |
| Booking Commission | % от суммы записи | После внедрения booking |
| Data Reports | Агрегированные отчёты для сервисов | После 10k+ сканов |

---

## Поэтапный план с учётом Production Strategy

```
Phase 0 (сейчас)
  → Validation: landing page, waitlist, опросы
  → Начать регистрацию заинтересованных сервисов параллельно

Phase 1–2 (B2C MVP)
  → Полный B2C с подпиской
  → Backend уже проектировать с учётом будущих Shops/Leads таблиц
  → В B2C добавить заглушку "Найти сервис" (показывает что функция будет)

Phase 3 (после 5,000 B2C пользователей)
  → Запуск B2B MVP (веб dashboard для сервисов)
  → Активация marketplace: геопоиск + отправка отчёта
  → Pilot: 5–10 сервисов бесплатно → валидация лид-потока

Phase 4+
  → Quote/Booking система
  → Монетизация лидов
  → Aggregated data insights для B2B
```

---

## Ключевые архитектурные решения

1. **Один общий Backend** — B2C mobile app и B2B web app работают через одно API. Разные endpoints, общая БД.

2. **Shop регистрация — отдельный flow** — сервисы регистрируются через отдельный onboarding, независимо от B2C пользователей.

3. **Согласие пользователя** — перед отправкой отчёта в сервис — явное согласие. Хранить в `ServiceLeads.user_consent = true`.

4. **Анонимизация данных** — агрегированная аналитика для B2B не содержит PII (имён, email, точных адресов).

5. **Статус лида в real-time** — WebSocket или push notifications для обновления статуса: `new → viewed → quoted → booked`.

---

## Конкурентное преимущество экосистемы

Ни один из существующих конкурентов (Carly, Car Scanner, OBD Auto Doctor) не имеет:
- Прямой связи с сетью автосервисов
- Передачи диагностического отчёта в сервис автоматически
- Замкнутого цикла: проблема → сервис → ремонт

Это создаёт уникальное ценностное предложение для обеих аудиторий.
