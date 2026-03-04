// TypeScript types matching the B2B API

export interface Shop {
  id: string
  name: string
  address: string
  phone: string
  email: string
  subscription_tier: 'basic' | 'pro'
  verified: boolean
}

export interface ShopSetupRequest {
  shop_name: string
  address?: string
  phone?: string
  email?: string
}

export interface FreezeFrame {
  rpm?: number
  coolant_temp_c?: number
  engine_load_pct?: number
  throttle_pos_pct?: number
  vehicle_speed_kmh?: number
  fuel_trim_short_pct?: number
  fuel_trim_long_pct?: number
  map_kpa?: number
}

export interface VehicleInfo {
  make: string
  model: string
  year: number
  engine_type: string
  mileage: number
}

export interface DiagnosticAnalyzeRequest {
  vehicle_info: VehicleInfo
  codes: string[]
  symptoms?: string
  freeze_frame?: FreezeFrame
  save_as_case?: boolean
}

export interface ProbableCause {
  description: string
  probability: number
  reasoning: string
  supporting_evidence: string[]
}

export interface DiagnosticStep {
  step: number
  action: string
  tool: 'visual' | 'multimeter' | 'oscilloscope' | 'scanner' | 'smoke_machine' | 'other'
  expected_result: string
  abnormal_result: string
}

export interface DiagnosticAnalyzeResponse {
  case_id: string | null
  probable_causes: ProbableCause[]
  diagnostic_sequence: DiagnosticStep[]
  estimated_labor_hours: number
  parts_likely_needed: string[]
  tsb_references: string[]
  urgency: 'immediate' | 'within_week' | 'routine'
  additional_notes: string
}

export interface EstimatePart {
  name: string
  quantity: number
  unit_price: number
  note?: string
}

export interface EstimateData {
  parts: EstimatePart[]
  labor_hours: number
  labor_rate_eur: number
  markup_pct: number
  parts_total: number
  labor_total: number
  subtotal: number
  total: number
  currency: string
  notes?: string
}

export interface Case {
  id: string
  shop_id: string
  vehicle_info: VehicleInfo
  input_codes: string[]
  symptoms_text: string
  ai_result: DiagnosticAnalyzeResponse | Record<string, never>
  client_report_text: string
  estimate: EstimateData | Record<string, never>
  status: 'open' | 'in_progress' | 'completed'
  created_at: string
}

export interface ReportOut {
  report_title: string
  issue_summary: string
  what_we_did: string
  why_it_matters: string
  parts_replaced: string[]
  next_steps: string
  disclaimer: string
}

export interface CheckoutResponse {
  checkout_url: string
}

export interface B2BSubscriptionStatus {
  subscription_tier: 'basic' | 'pro'
}

export interface QuoteDto {
  cost_min: number
  cost_max: number
  estimated_days: number
  notes?: string
}

export interface QuoteRequest {
  cost_min: number
  cost_max: number
  estimated_days: number
  notes?: string
}

export interface Lead {
  lead_id: string
  user_email: string
  status: 'pending' | 'quoted' | 'closed'
  dtc_codes: string[]
  vehicle_info: Record<string, string>
  freeze_frame?: Record<string, unknown>
  created_at: string
  quote?: QuoteDto
}
