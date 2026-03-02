# API Contract — AI OBD Ecosystem
## Backend: FastAPI (Python) | Base URL: /api/v1

> Все защищённые endpoints требуют заголовок: `Authorization: Bearer {firebase_id_token}`
> B2C токены и B2B токены верифицируются одним Firebase Admin SDK, но разными user pools.

---

## Аутентификация

Firebase Auth на клиентах (Android + Web). Backend верифицирует Firebase ID Token через Firebase Admin SDK Python.

```
POST /api/v1/auth/verify
  Headers: Authorization: Bearer {firebase_id_token}
  Response 200: {
    "user_type": "b2c_user" | "b2b_shop_user",
    "user_id": "uuid",
    "subscription_status": "free" | "premium" | "trial",
    "region": "PL" | "DE" | "EU"
  }

  Логика: создаёт User/ShopUser в БД если не существует (first login)
```

```
POST /api/v1/b2b/auth/register-shop
  Headers: Authorization: Bearer {firebase_id_token}
  Body: {
    "shop_name": "string",
    "address": "string",
    "lat": 52.2297,
    "lng": 21.0122,
    "phone": "string",
    "specializations": ["engine", "transmission", "brakes", "electrical", "body"]
  }
  Response 201: { "shop_id": "uuid", "shop_name": "string" }
```

```
DELETE /api/v1/auth/account
  Headers: Authorization: Bearer {firebase_id_token}
  Response 200: { "message": "Account and all data deleted" }
  Логика: GDPR erasure — удаляет все данные пользователя
```

---

## B2C Endpoints

### Автомобили

```
GET /api/v1/b2c/vehicles
  Headers: Authorization: Bearer {token}
  Response 200: [
    {
      "id": "uuid",
      "make": "Toyota",
      "model": "Corolla",
      "year": 2018,
      "engine_type": "1.6 petrol",
      "vin": "string | null",
      "created_at": "ISO8601"
    }
  ]

POST /api/v1/b2c/vehicles
  Body: {
    "make": "Toyota",
    "model": "Corolla",
    "year": 2018,
    "engine_type": "1.6 petrol",
    "vin": "string | null"
  }
  Response 201: { "id": "uuid", ...vehicle }

PUT /api/v1/b2c/vehicles/{vehicle_id}
  Body: { same as POST, all fields optional }
  Response 200: { ...updated_vehicle }

DELETE /api/v1/b2c/vehicles/{vehicle_id}
  Response 204: (no content)
```

---

### Сканирование и диагностика (главный endpoint)

```
POST /api/v1/b2c/scan/analyze
  Headers: Authorization: Bearer {token}
  Body: {
    "vehicle_id": "uuid",
    "mileage": 95000,
    "dtc_codes": ["P0420", "P0171"],
    "freeze_frame": {
      "rpm": 1200,
      "coolant_temp_c": 88,
      "throttle_pos_pct": 15,
      "engine_load_pct": 42,
      "fuel_trim_short_pct": -12.5,
      "fuel_trim_long_pct": -8.3,
      "map_kpa": 45,
      "vehicle_speed_kmh": 0
    }
  }
  Response 200: {
    "session_id": "uuid",
    "scan_date": "ISO8601",
    "overall_risk": "low" | "medium" | "high",
    "codes": [
      {
        "code": "P0420",
        "system": "emissions",
        "severity": "medium",
        "can_drive": "yes_within_2_weeks",
        "free_tier": {
          "description": "Catalyst System Efficiency Below Threshold Bank 1",
          "category": "emissions"
        },
        "premium": {               // null если free tier
          "simple_explanation": "Ваш катализатор работает ниже нормы...",
          "main_causes": ["worn catalyst", "O2 sensor fault", "exhaust leak"],
          "causes_probability": [65, 25, 10],
          "what_happens_if_ignored": "Повышенный расход топлива, возможный отказ O2 сенсора",
          "recommended_action": "Проверьте в сервисе в течение 1-2 недель",
          "cost_estimate": { "min": 250, "max": 900, "currency": "EUR", "region": "PL" },
          "avg_labor_hours": 2.5
        }
      }
    ]
  }

  Логика: Rule Engine всегда → RAG + LLM только для premium users
```

```
GET /api/v1/b2c/scan/sessions
  Query: ?vehicle_id={uuid}&limit=20&offset=0
  Response 200: [
    {
      "session_id": "uuid",
      "scan_date": "ISO8601",
      "mileage": 95000,
      "overall_risk": "medium",
      "codes_count": 2,
      "codes": ["P0420", "P0171"]
    }
  ]

GET /api/v1/b2c/scan/sessions/{session_id}
  Response 200: { полный объект как в POST /analyze response }

DELETE /api/v1/b2c/scan/sessions/{session_id}
  Response 204
```

---

### DTC справочник (free для всех)

```
GET /api/v1/shared/dtc/{code}
  Response 200: {
    "code": "P0420",
    "standard_description": "Catalyst System Efficiency Below Threshold Bank 1",
    "category": "P - Powertrain",
    "system": "emissions",
    "severity_level": "medium",
    "can_drive_flag": "yes_with_caution"
  }
  Response 404: { "error": "DTC code not found" }

GET /api/v1/shared/dtc/search
  Query: ?q=catalyst&limit=10
  Response 200: [ { ...dtc_objects } ]
```

---

### Справочник автомобилей

```
GET /api/v1/shared/vehicles/makes
  Response 200: ["Audi", "BMW", "Ford", "Honda", "Hyundai", "Kia", "Mercedes", "Nissan", "Opel", "Peugeot", "Renault", "Skoda", "Toyota", "Volkswagen", "Volvo", ...]

GET /api/v1/shared/vehicles/models
  Query: ?make=Toyota&year=2018
  Response 200: ["Auris", "Avensis", "Camry", "Corolla", "Hilux", "Land Cruiser", "Prius", "RAV4", "Yaris", ...]
```

---

### Подписка B2C

> **Модель:** Web-based подписка через Stripe. Пользователь платит на сайте, а не через Google Play.
> Причина: Избегаем 15-30% комиссии Google. Юридически разрешено (так делают Spotify, Netflix).

```
GET /api/v1/b2c/subscription/status
  Response 200: {
    "status": "free" | "premium" | "trial" | "cancelled",
    "plan": "monthly" | "yearly" | null,
    "expires_at": "ISO8601" | null,
    "trial_days_left": 7 | null
  }

POST /api/v1/b2c/subscription/checkout
  Body: { "plan": "monthly" | "yearly", "success_url": "string", "cancel_url": "string" }
  Response 200: {
    "checkout_url": "https://checkout.stripe.com/..."
  }
  Логика: создаёт Stripe Checkout Session, возвращает URL

DELETE /api/v1/b2c/subscription
  Response 200: { "cancelled_at": "ISO8601", "active_until": "ISO8601" }

POST /api/v1/webhooks/stripe/b2c
  Headers: Stripe-Signature: {stripe_sig}  // без Authorization
  Логика: обновляет subscription_status в Users таблице
```

---

### Профиль пользователя

```
GET /api/v1/b2c/user/profile
  Response 200: {
    "id": "uuid",
    "email": "string",
    "region": "PL",
    "subscription_status": "premium",
    "created_at": "ISO8601"
  }

PUT /api/v1/b2c/user/profile
  Body: { "region": "DE" }
  Response 200: { ...updated_profile }
```

---

## B2B Endpoints

### Профиль сервиса

```
GET /api/v1/b2b/shop/profile
  Response 200: {
    "id": "uuid",
    "name": "AutoPro Service",
    "address": "ul. Przykładowa 1, Warszawa",
    "lat": 52.2297,
    "lng": 21.0122,
    "phone": "+48 123 456 789",
    "email": "contact@autopro.pl",
    "specializations": ["engine", "transmission"],
    "working_hours": { "mon-fri": "8:00-18:00", "sat": "9:00-14:00" },
    "subscription_tier": "basic" | "pro",
    "rating": 4.7,
    "verified": true
  }

PUT /api/v1/b2b/shop/profile
  Body: { любые поля профиля }
  Response 200: { ...updated_profile }
```

---

### Команда сервиса (Pro план)

```
GET /api/v1/b2b/shop/team
  Response 200: [{ "id": "uuid", "email": "string", "role": "owner" | "mechanic", "created_at": "ISO8601" }]

POST /api/v1/b2b/shop/team/invite
  Body: { "email": "mechanic@autopro.pl", "role": "mechanic" }
  Response 201: { "invite_sent": true }

DELETE /api/v1/b2b/shop/team/{user_id}
  Response 204
```

---

### Диагностические кейсы (главный endpoint B2B)

```
POST /api/v1/b2b/diagnostic/analyze
  Body: {
    "vehicle": {
      "make": "Volkswagen",
      "model": "Golf",
      "year": 2019,
      "engine_type": "2.0 TDI",
      "mileage": 145000,
      "vin": "string | null"
    },
    "dtc_codes": ["P0401", "P042E"],
    "freeze_frame": { ...same structure as B2C },
    "symptoms": "Потеря мощности на холодном двигателе, дымит при старте",
    "save_as_case": true
  }
  Response 200: {
    "case_id": "uuid | null",
    "probable_causes": [
      {
        "description": "Забитый клапан EGR",
        "probability": 78,
        "reasoning": "P0401 + симптомы потери мощности + высокий пробег TDI"
      },
      {
        "description": "Дефект датчика давления EGR",
        "probability": 15,
        "reasoning": "P042E указывает на проблему с датчиком потока"
      }
    ],
    "diagnostic_sequence": [
      {
        "step": 1,
        "action": "Визуально проверить клапан EGR и патрубки на загрязнение",
        "tool": "visual",
        "expected_result": "Нагар на клапане, чёрные отложения"
      },
      {
        "step": 2,
        "action": "Измерить напряжение на разъёме датчика EGR при запуске",
        "tool": "multimeter",
        "expected_result": "5V reference, 0.5-4.5V signal"
      }
    ],
    "checklist": [
      { "id": 1, "item": "EGR клапан — визуальный осмотр", "completed": false },
      { "id": 2, "item": "Датчик давления EGR — измерение", "completed": false },
      { "id": 3, "item": "Патрубки EGR — проверка на трещины", "completed": false }
    ],
    "estimated_labor_hours": 3.0,
    "parts_likely_needed": ["EGR valve", "EGR pressure sensor"],
    "tsb_references": ["VW TSB 2033719 — EGR system 2.0 TDI 2018-2020"],
    "urgency": "within_week"
  }
```

```
GET /api/v1/b2b/cases
  Query: ?limit=20&offset=0&search=P0401&make=Volkswagen
  Response 200: [
    {
      "case_id": "uuid",
      "created_at": "ISO8601",
      "vehicle": { "make": "VW", "model": "Golf", "year": 2019 },
      "codes": ["P0401", "P042E"],
      "status": "open" | "in_progress" | "resolved",
      "has_report": false,
      "has_estimate": false
    }
  ]

GET /api/v1/b2b/cases/{case_id}
  Response 200: { полный кейс включая ai_result, report, estimate }

PUT /api/v1/b2b/cases/{case_id}
  Body: {
    "status": "in_progress",
    "checklist_updates": [{ "id": 1, "completed": true }],
    "mechanic_notes": "string"
  }
  Response 200: { ...updated_case }

DELETE /api/v1/b2b/cases/{case_id}
  Response 204
```

---

### Отчёт для клиента

```
POST /api/v1/b2b/cases/{case_id}/report/generate
  Body: {
    "confirmed_diagnosis": "Неисправный клапан EGR",
    "repair_description": "Замена клапана EGR и очистка системы",
    "additional_notes": "string | null"
  }
  Response 200: {
    "report_id": "uuid",
    "report_text": {
      "issue_summary": "В вашем автомобиле обнаружена неисправность системы рециркуляции выхлопных газов (EGR)...",
      "what_we_did": "Мы заменили клапан EGR и провели очистку трубопроводов системы...",
      "why_it_matters": "Исправная система EGR снижает расход топлива и вредные выбросы...",
      "next_steps": "Рекомендуем плановое ТО через 15,000 км"
    }
  }

GET /api/v1/b2b/cases/{case_id}/report/pdf
  Response 200: (binary PDF file)
  Headers: Content-Type: application/pdf
           Content-Disposition: attachment; filename="report_{case_id}.pdf"

PUT /api/v1/b2b/cases/{case_id}/report
  Body: { "report_text": { ...edited text } }
  Response 200: { ...updated_report }
```

---

### Смета

```
POST /api/v1/b2b/cases/{case_id}/estimate
  Body: {
    "parts": [
      { "name": "EGR Valve VW 2.0 TDI", "part_number": "03L131501E", "price": 180.00, "quantity": 1 },
      { "name": "EGR Gasket Set", "part_number": "03L131521B", "price": 12.50, "quantity": 1 }
    ],
    "labor_hours": 2.5,
    "labor_rate_per_hour": 80.00,
    "markup_pct": 15,
    "notes": "string | null"
  }
  Response 200: {
    "estimate_id": "uuid",
    "parts_total": 192.50,
    "labor_total": 200.00,
    "subtotal": 392.50,
    "markup": 58.88,
    "total": 451.38,
    "currency": "EUR"
  }

GET /api/v1/b2b/cases/{case_id}/estimate/pdf
  Response 200: (binary PDF file)
```

---

### Подписка B2B

```
GET /api/v1/b2b/subscription/status
  Response 200: {
    "status": "trial" | "active" | "cancelled" | "past_due",
    "plan": "basic" | "pro",
    "price_per_month": 49 | 99,
    "billing_period": "monthly" | "yearly",
    "next_billing_date": "ISO8601",
    "seats_used": 2,
    "seats_limit": 1 | 5
  }

POST /api/v1/b2b/subscription/checkout
  Body: { "plan": "basic" | "pro", "billing": "monthly" | "yearly" }
  Response 200: { "checkout_url": "https://checkout.stripe.com/..." }

PUT /api/v1/b2b/subscription/upgrade
  Body: { "plan": "pro" }
  Response 200: { "upgraded": true, "new_plan": "pro" }

DELETE /api/v1/b2b/subscription
  Response 200: { "cancelled_at": "ISO8601", "active_until": "ISO8601" }

POST /api/v1/webhooks/stripe/b2b
  Headers: Stripe-Signature: {stripe_sig}
  Логика: обновляет subscription_tier в Shops таблице
```

---

## Integration Endpoints (Phase 3+)

> Не реализуются в MVP. Архитектурно заложены в схеме БД.

```
// B2C: найти партнёрские сервисы
GET /api/v1/b2c/shops/nearby
  Query: ?lat=52.2297&lng=21.0122&radius_km=15&specialization=engine
  Response 200: [
    {
      "shop_id": "uuid",
      "name": "AutoPro Service",
      "distance_km": 3.2,
      "rating": 4.7,
      "specializations": ["engine", "transmission"],
      "address": "ul. Przykładowa 1"
    }
  ]

// B2C: отправить диагностический отчёт в сервис
POST /api/v1/b2c/leads
  Body: {
    "session_id": "uuid",
    "shop_id": "uuid",
    "user_consent": true,
    "message": "string | null"
  }
  Response 201: {
    "lead_id": "uuid",
    "status": "new",
    "sent_to_shop": "AutoPro Service"
  }

GET /api/v1/b2c/leads
  Response 200: [ { lead_id, status, shop_name, created_at, quote_amount } ]

// B2B: входящие лиды
GET /api/v1/b2b/leads
  Query: ?status=new&limit=20
  Response 200: [
    {
      "lead_id": "uuid",
      "created_at": "ISO8601",
      "status": "new" | "viewed" | "quoted" | "booked" | "completed" | "declined",
      "vehicle": { "make": "Toyota", "model": "Corolla", "year": 2018 },
      "codes": ["P0420"],
      "ai_summary": "Проблема с катализатором, риск: Medium",
      "user_contact": { "phone": "string | null" }
    }
  ]

PUT /api/v1/b2b/leads/{lead_id}/quote
  Body: {
    "parts_cost": 250.00,
    "labor_cost": 160.00,
    "total_cost": 410.00,
    "estimated_days": 1,
    "message": "Можем принять в четверг или пятницу"
  }
  Response 200: { "lead_id": "uuid", "status": "quoted" }

PUT /api/v1/b2c/leads/{lead_id}/book
  Body: { "preferred_date": "2025-03-10", "preferred_time": "10:00" }
  Response 200: { "lead_id": "uuid", "status": "booked" }
```

---

## Коды ошибок API

| HTTP | Код | Описание |
|---|---|---|
| 400 | VALIDATION_ERROR | Неверные данные в запросе |
| 401 | UNAUTHORIZED | Нет/невалидный токен |
| 403 | SUBSCRIPTION_REQUIRED | Функция требует платную подписку |
| 403 | FORBIDDEN | Нет доступа к ресурсу |
| 404 | NOT_FOUND | Ресурс не найден |
| 429 | RATE_LIMIT | Превышен лимит запросов |
| 503 | AI_UNAVAILABLE | AI сервис временно недоступен |

```json
// Стандартный формат ошибки
{
  "error": "SUBSCRIPTION_REQUIRED",
  "message": "AI explanation requires premium subscription",
  "detail": null
}
```

---

## Rate Limits

| Endpoint | Free | Premium / B2B |
|---|---|---|
| POST /b2c/scan/analyze | 3/день | 50/день |
| POST /b2b/diagnostic/analyze | — | 100/день |
| GET /shared/dtc/* | 30/мин | 100/мин |
| POST */report/generate | — | 20/день |
