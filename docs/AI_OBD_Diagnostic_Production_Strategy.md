# AI OBD Diagnostic Assistant

## Technical Roadmap, Architecture, Unit Economics & Production Plan

------------------------------------------------------------------------

# 1. Detailed Technical Roadmap

## Phase 0 -- Validation (2--3 weeks)

-   Landing page + waitlist
-   Market validation via ads
-   Survey car owners
-   Collect common OBD error questions
-   Define pricing sensitivity

Deliverable: Validated demand + 500+ waitlist users

------------------------------------------------------------------------

## Phase 1 -- Core MVP (8--10 weeks)

### Backend

-   REST API (FastAPI / Node.js)
-   Authentication (JWT)
-   OBD error database (standard P0xxx codes)
-   Risk classification engine (rule-based)
-   AI explanation service (LLM wrapper)
-   Basic pricing dataset per region

### Mobile App

-   Flutter app
-   Bluetooth ELM327 integration
-   Error scan UI
-   Error explanation screen
-   History storage (cloud + local cache)

Deliverable: Public beta version

------------------------------------------------------------------------

## Phase 2 -- Smart Intelligence Layer (6--8 weeks)

-   Improve probability modeling
-   Add recurrence tracking
-   Add vehicle profile (VIN, engine type)
-   Improve cost estimation using collected data
-   Add subscription paywall

Deliverable: Monetized v1.0

------------------------------------------------------------------------

## Phase 3 -- Scale & Optimization

-   Add photo diagnostics
-   Add sound analysis
-   Regional cost API
-   Performance optimization
-   Expand to iOS & Android full release

------------------------------------------------------------------------

# 2. Database Structure (Initial Schema)

## Users

-   id (UUID)
-   email
-   password_hash
-   created_at
-   subscription_status
-   region

## Vehicles

-   id (UUID)
-   user_id (FK)
-   make
-   model
-   year
-   engine_type
-   vin

## ScanSessions

-   id (UUID)
-   vehicle_id (FK)
-   scan_date
-   mileage
-   raw_data_json

## ErrorCodes

-   code (PK)
-   standard_description
-   category
-   severity_level

## ErrorOccurrences

-   id (UUID)
-   session_id (FK)
-   code (FK)
-   freeze_frame_json

## RepairCostEstimates

-   id
-   code
-   region
-   min_cost
-   max_cost
-   avg_labor_hours

------------------------------------------------------------------------

# 3. AI Architecture (LLM + Rule Engine)

## Layer 1 -- Structured Rule Engine

Handles: - Severity classification - Drive safety classification - Known
deterministic logic - Cost range mapping

This prevents hallucinations.

------------------------------------------------------------------------

## Layer 2 -- LLM Explanation Layer

Input: - OBD code - Vehicle info - Severity level - Historical
recurrence

LLM generates: - Human explanation - Possible causes (ranked) - What
happens if ignored - Suggested next steps

------------------------------------------------------------------------

## Layer 3 -- Confidence Model

Combine: - Rule-based probability - Frequency from dataset - Recurrence
patterns

Output: - Confidence score (%) - Risk badge

------------------------------------------------------------------------

## Why Hybrid Architecture?

-   Rules ensure reliability
-   LLM ensures readability
-   Structured data ensures monetizable insights

------------------------------------------------------------------------

# 4. Unit Economics

## Assumptions

-   Subscription price: 7€ / month
-   Conversion rate: 5--10%
-   Monthly active users target: 10,000

If 7% convert:

10,000 × 7% = 700 paying users\
700 × 7€ = 4,900€ / month

------------------------------------------------------------------------

## Costs

-   LLM API: \~0.02--0.05€ per diagnostic
-   1 user avg 3 diagnostics/month
-   700 paying users → \~2,100 diagnostics

Estimated AI cost: \~80--120€ / month

Other costs: - Hosting: 100--200€ - Payment processing: \~3% -
Marketing: variable

Gross margin potential: 70--85%

------------------------------------------------------------------------

# 5. Production Plan

## Step 1 -- Build Closed Beta

-   Invite 100--300 users
-   Collect real scan logs
-   Improve risk classification

## Step 2 -- Stabilize AI

-   Remove hallucinations
-   Add structured prompts
-   Implement fallback logic

## Step 3 -- Launch Paid Tier

-   Add Stripe / RevenueCat
-   Introduce subscription lock

## Step 4 -- Scale Marketing

-   YouTube car reviewers
-   SEO: OBD code search pages
-   Affiliate partnerships with ELM327 sellers

------------------------------------------------------------------------

# 6. Long-Term Competitive Advantage

-   Aggregated anonymized error frequency data
-   Regional repair cost dataset
-   Recurrence prediction engine
-   Possible B2B extension in future

------------------------------------------------------------------------

# 7. Final Strategic Recommendation

Start with B2C (AI OBD Diagnostic Assistant).

Reason: - Faster validation - Lower sales barrier - Data collection
opportunity - Foundation for future B2B SaaS

After reaching 5,000+ active users: → Launch B2B version using collected
diagnostic intelligence.
