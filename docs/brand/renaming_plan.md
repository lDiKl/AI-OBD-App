# Avyrox — Plan Rebranding

**Date:** 2026-03-05

**Renaming map:**
- B2C (DriverAI) → **Avyrox Drive**
- B2B Android (ShopAI) → **Avyrox Service**
- B2B Web (ShopAI Web) → **Avyrox Cloud**
- Backend API → **Avyrox API**

---

## Legend

- `[ ]` — to do
- `[x]` — done

---

## 1. Mobile B2C — Avyrox Drive

### 1.1 App display name and resources

- [x] `mobile-b2c/app/src/main/res/values/strings.xml`
  - `app_name`: `DriverAI` → `Avyrox Drive`

- [x] `mobile-b2c/app/src/main/res/values/themes.xml`
  - `Theme.DriverAI` → `Theme.AvyroxDrive`

### 1.2 AndroidManifest

- [x] `mobile-b2c/app/src/main/AndroidManifest.xml`
  - `android:name=".DriverAIApp"` → `android:name=".AvyroxDriveApp"`
  - `android:theme="@style/Theme.DriverAI"` → `android:theme="@style/Theme.AvyroxDrive"`
  - Deep link scheme: `android:scheme="driverai"` → `android:scheme="avyroxdrive"`

### 1.3 Build config

- [x] `mobile-b2c/app/build.gradle.kts`
  - `namespace = "com.driverai.b2c"` → `namespace = "com.avyrox.drive"`
  - `applicationId = "com.driverai.b2c"` → `applicationId = "com.avyrox.drive"`
  - Release API URL: `"https://api.driverai.com"` → `"https://api.avyrox.io"`

### 1.4 Kotlin source files — package rename

- [x] All 46 `.kt` files — `package com.driverai.b2c` → `com.avyrox.drive` (batch replaced)
- [x] `DriverAIApp.kt` — class renamed `DriverAIApp` → `AvyroxDriveApp`, file renamed to `AvyroxDriveApp.kt`
- [x] `ui/auth/LoginScreen.kt` — UI text "DriverAI" → "Avyrox Drive"
- [x] Java source directory moved: `com/driverai/b2c/` → `com/avyrox/drive/`
- [x] Root folder renamed: `mobile-b2c/` → `avyrox-drive/`
- [x] `avyrox-drive/README.md` — updated with new names and correct package structure

---

## 2. Mobile B2B — Avyrox Service

### 2.1 App display name and resources

- [x] `mobile-b2b/app/src/main/res/values/strings.xml`
  - `app_name`: `ShopAI` → `Avyrox Service`

- [x] `mobile-b2b/app/src/main/res/values/themes.xml`
  - `Theme.ShopAI` → `Theme.AvyroxService`

### 2.2 AndroidManifest

- [x] `mobile-b2b/app/src/main/AndroidManifest.xml`
  - `android:name=".ShopAIApp"` → `android:name=".AvyroxServiceApp"`
  - `android:theme="@style/Theme.ShopAI"` → `android:theme="@style/Theme.AvyroxService"`
  - Deep link scheme: `android:scheme="shopai"` → `android:scheme="avyroxservice"`

### 2.3 Build config

- [x] `mobile-b2b/app/build.gradle.kts`
  - `namespace = "com.shopai.b2b"` → `namespace = "com.avyrox.service"`
  - `applicationId = "com.shopai.b2b"` → `applicationId = "com.avyrox.service"`
  - Release API URL: `"https://api.shopai.com"` → `"https://api.avyrox.io"`

### 2.4 Kotlin source files — package rename

- [x] All 41 `.kt` files — `package com.shopai.b2b` → `com.avyrox.service` (batch replaced)
- [x] `ShopAIApp.kt` — class renamed `ShopAIApp` → `AvyroxServiceApp`
- [x] `ui/theme/ShopAITheme.kt` — function renamed `ShopAITheme` → `AvyroxServiceTheme`
- [x] `MainActivity.kt` — updated to use `AvyroxServiceTheme`
- [ ] File renames: `ShopAIApp.kt` → `AvyroxServiceApp.kt`, `ShopAITheme.kt` → `AvyroxServiceTheme.kt` (cosmetic)

---

## 3. Web B2B — Avyrox Cloud

### 3.1 Package metadata

- [x] `web/package.json`
  - `"name": "shopai-web"` → `"name": "avyrox-cloud"`

### 3.2 HTML / SEO

- [x] `web/index.html`
  - `<title>ShopAI — Auto Service Assistant</title>` → `<title>Avyrox Cloud — Auto Service Platform</title>`

### 3.3 UI text in source files

- [x] `web/src/pages/LoginPage.tsx`
  - `ShopAI` → `Avyrox Cloud`, subtitle updated

- [x] `web/src/components/Layout.tsx`
  - `ShopAI` → `Avyrox Cloud`

---

## 4. Backend — Avyrox API

### 4.1 FastAPI app title

- [x] `backend/app/main.py`
  - `title="AI OBD Ecosystem API"` → `title="Avyrox API"`

### 4.2 Project metadata

- [x] `backend/pyproject.toml`
  - `name = "ai-obd-backend"` → `name = "avyrox-api"`
  - `description = "AI OBD Ecosystem — Shared Backend API"` → `description = "Avyrox — Intelligent Automotive Service Platform API"`

### 4.3 Stripe return URLs (config defaults)

- [x] `backend/app/core/config.py`
  - `B2C_SUCCESS_URL` → `"https://drive.avyrox.io/payment/success"`
  - `B2C_CANCEL_URL` → `"https://drive.avyrox.io/payment/cancel"`
  - `B2B_SUCCESS_URL` → `"https://cloud.avyrox.io/billing/success"`
  - `B2B_CANCEL_URL` → `"https://cloud.avyrox.io/billing/cancel"`

### 4.4 Environment example file

- [x] `backend/.env.example`
  - `B2C_SUCCESS_URL=avyroxdrive://payment/success`
  - `B2C_CANCEL_URL=avyroxdrive://payment/cancel`

### 4.5 Seed scripts (test data only)

- [ ] `backend/scripts/seed_shops.py`
  - Test emails `@shopai.test` → `@avyrox.test` (optional, test data only)

---

## 5. Documentation

### 5.1 Docs with old brand name references

- [x] `docs/development_plan.md`
  - `driverai://payment/success` → `avyroxdrive://payment/success`

- [x] `docs/architecture.md`
  - `"Новый лид от пользователя DriverAI"` → `"Новый лид от пользователя Avyrox Drive"`

- [ ] `docs/AI_OBD_Diagnostic_MVP.md` — review and replace "DriverAI" with "Avyrox Drive"
- [ ] `docs/AI_Auto_Service_Assistant_MVP.md` — review and replace "ShopAI" with "Avyrox Service"
- [ ] `docs/AI_OBD_Diagnostic_Production_Strategy.md` — review for old brand names
- [ ] `docs/brainstorm_analysis.md` — review for old brand names
- [ ] `docs/bainstorm.md` — review for old brand names
- [ ] `docs/api_contract.md` — review for old brand names and API URLs
- [ ] `docs/prompts.md` — review for old brand names
- [ ] `docs/development_plan.md` — full review for all old names

### 5.2 Memory file

- [x] `MEMORY.md` (Claude's project memory) — updated: `Avyrox Drive`, `Avyrox Service`, `Avyrox Cloud`

---

## 6. External Services

> For services that cannot be renamed in-place — create new entries with new names.

### 6.1 Firebase

- [ ] **B2C Firebase project**
  - Current: created for "DriverAI"
  - Action: In Firebase Console — rename the project display name to "Avyrox Drive"
    (Project ID itself cannot be changed — acceptable as it's internal only)
  - Update `google-services.json` in `mobile-b2c/app/` if project settings change

- [ ] **B2B Firebase project** (if separate) or shared project
  - Rename display name to "Avyrox Service / Avyrox Cloud"
  - Update `google-services.json` in `mobile-b2b/app/` if needed

- [ ] **Firebase Auth — authorized domains**
  - After domain acquisition: add `avyrox.io`, `cloud.avyrox.io` to authorized domains
  - Remove old `driverai.com`, `shopai.com` domains (when ready)

### 6.2 Stripe

- [ ] **B2C subscription product**
  - In Stripe Dashboard → Products — rename "DriverAI Premium" → "Avyrox Drive Premium"

- [ ] **B2B subscription products**
  - Rename all ShopAI subscription tiers → "Avyrox Service [Tier]" / "Avyrox Cloud [Tier]"

- [ ] **Stripe webhook URLs**
  - After domain acquisition: update webhook endpoint URL to new domain
  - Old: `https://api.driverai.com/api/v1/webhooks/stripe`
  - New: `https://api.avyrox.io/api/v1/webhooks/stripe`

### 6.3 Google Play Store

> Note: Changing `applicationId` (`com.driverai.b2c` → `com.avyrox.drive`) means the app is treated as a NEW app on Google Play.
> If the app is not yet published — just use the new ID. If already published — two options:
> a) publish as new app (loses reviews/installs), or b) keep old applicationId but change display name only.

- [ ] **B2C app listing**
  - App name: "DriverAI" → "Avyrox Drive"
  - Short/full description: update to reflect Avyrox branding
  - App icon: update when new brand assets are ready (see design system doc)

- [ ] **B2B app listing**
  - App name: "ShopAI" → "Avyrox Service"
  - Short/full description: update
  - App icon: update

### 6.4 Domain

- [ ] Purchase preferred domain (recommended: `avyrox.io` or `avyrox.app`)
- [ ] Configure DNS:
  - `api.avyrox.io` → backend server
  - `cloud.avyrox.io` → web B2B frontend
  - `drive.avyrox.io` → B2C landing (future)
  - `service.avyrox.io` → B2B landing (future)

### 6.5 Social media & accounts

- [ ] Secure username `avyrox` on: GitHub, LinkedIn, X (Twitter), Instagram, Facebook
- [ ] Secure `avyrox` developer accounts (Google Play Console — if not already)

---

## 7. App Icons & Visual Assets

- [ ] Design new icon for **Avyrox Drive** (B2C) — see `docs/brand/avyrox_design_system_and_roadmap.md`
  - Replace `mobile-b2c/app/src/main/res/mipmap-*/ic_launcher.*`

- [ ] Design new icon for **Avyrox Service** (B2B Android)
  - Replace `mobile-b2b/app/src/main/res/mipmap-*/ic_launcher.*`

- [ ] Design favicon for **Avyrox Cloud** (web)
  - Replace `web/favicon.svg`

---

## 8. Execution Order (Recommended)

Complete steps in this order to minimize broken states:

1. **Docs** (5.1) — no code impact, easy start
2. **Backend** (Section 4) — config, titles, URLs
3. **Web** (Section 3) — fastest to deploy
4. **Mobile B2C** (Section 1) — package rename via Android Studio
5. **Mobile B2B** (Section 2) — package rename via Android Studio
6. **Firebase** (6.1) — rename projects and update google-services.json
7. **Stripe** (6.2) — rename products and update webhooks
8. **Domain** (6.4) — purchase and configure DNS
9. **Google Play** (6.3) — update listings after new builds
10. **Social media** (6.5) — secure usernames
11. **App icons** (Section 7) — visual assets last

---

## Summary Table

| Area | Current Name | New Name | Status |
|------|-------------|----------|--------|
| B2C Android app | DriverAI | Avyrox Drive | [x] |
| B2C package ID | com.driverai.b2c | com.avyrox.drive | [x] |
| B2C deep link scheme | driverai:// | avyroxdrive:// | [x] |
| B2C API domain | api.driverai.com | api.avyrox.io | [x] |
| B2B Android app | ShopAI | Avyrox Service | [x] |
| B2B package ID | com.shopai.b2b | com.avyrox.service | [x] |
| B2B deep link scheme | shopai:// | avyroxservice:// | [x] |
| B2B Web title | ShopAI | Avyrox Cloud | [x] |
| Web npm package | shopai-web | avyrox-cloud | [x] |
| B2B API domain | api.shopai.com | api.avyrox.io | [x] |
| Backend API title | AI OBD Ecosystem API | Avyrox API | [x] |
| Root folder B2C | mobile-b2c/ | avyrox-drive/ | [x] |
| Java package dir B2C | com/driverai/b2c/ | com/avyrox/drive/ | [x] |
| Firebase B2C project | (current display name) | Avyrox Drive | [ ] |
| Firebase B2B project | (current display name) | Avyrox Service | [ ] |
| Stripe B2C product | DriverAI Premium | Avyrox Drive Premium | [ ] |
| Stripe B2B products | ShopAI tiers | Avyrox Service tiers | [ ] |
| Google Play B2C | DriverAI | Avyrox Drive | [ ] |
| Google Play B2B | ShopAI | Avyrox Service | [ ] |
| Domain | — | avyrox.io (recommended) | [ ] |
