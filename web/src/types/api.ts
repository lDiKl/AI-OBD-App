// TypeScript types matching docs/api_contract.md

export interface DiagnosticAnalyzeRequest {
  vehicle_info: {
    make: string
    model: string
    year: number
    engine_type: string
    mileage: number
  }
  codes: string[]
  symptoms?: string
  freeze_frame?: FreezeFrame
  save_as_case?: boolean
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

export interface Case {
  id: string
  vehicle_info: DiagnosticAnalyzeRequest['vehicle_info']
  input_codes: string[]
  symptoms_text: string
  ai_result: DiagnosticAnalyzeResponse | null
  client_report_text: string
  estimate: EstimateData | null
  status: 'open' | 'in_progress' | 'completed'
  created_at: string
}

export interface EstimateData {
  parts: EstimatePart[]
  labor_hours: number
  labor_rate_eur: number
  total_eur: number
}

export interface EstimatePart {
  name: string
  part_number?: string
  price_eur: number
  quantity: number
}
