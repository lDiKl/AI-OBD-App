# AI OBD Diagnostic Assistant (B2C)

## 1. Product Vision

Mobile application for car owners that: - Connects to OBD-II adapter
(ELM327) - Reads error codes - Explains issues in simple human
language - Assesses risk level - Estimates repair cost - Stores vehicle
history

Goal: Make car diagnostics understandable and reduce fear of unknown
repair costs.

------------------------------------------------------------------------

## 2. Target Audience

-   Car owners (non-technical)
-   Used car buyers
-   People afraid of being overcharged in service
-   Drivers who want transparency

------------------------------------------------------------------------

## 3. Core Problem

Existing OBD apps: - Show raw error codes - Provide technical
descriptions - Do not explain urgency - Do not estimate repair costs

Users need clarity and confidence.

------------------------------------------------------------------------

## 4. MVP Scope (Version 1.0)

### 4.1 OBD Connection

-   Bluetooth connection to ELM327
-   Read standard OBD-II codes
-   Display freeze frame data

### 4.2 AI Error Explanation

For each error: - Simple explanation - Possible causes - Risk level (Low
/ Medium / High) - Can you drive? (Yes / Limited / No) - Consequences if
ignored

### 4.3 Basic Repair Cost Estimation

-   Static repair cost ranges
-   Regional selection (e.g., Poland, Germany)
-   Parts + labor estimate range

### 4.4 History

-   Save scan sessions
-   Track recurring errors

------------------------------------------------------------------------

## 5. Not Included in MVP

-   Photo analysis
-   Sound diagnostics
-   Service marketplace integration
-   Advanced predictive analytics

------------------------------------------------------------------------

## 6. Monetization (MVP Phase)

Freemium model: - Free: Read codes - Paid subscription (5--10€): - AI
explanation - Risk assessment - Repair cost estimation

------------------------------------------------------------------------

## 7. Technical Architecture

Frontend: - Flutter or native Kotlin

Backend: - REST API - Error code database - AI model (LLM + structured
rules) - Basic pricing dataset

------------------------------------------------------------------------

## 8. Estimated Development Time

-   OBD integration: 2--3 weeks
-   Backend + AI logic: 3--4 weeks
-   UI/UX: 2--3 weeks
-   Testing: 2 weeks

Estimated MVP timeline: \~2--3 months (solo developer).
