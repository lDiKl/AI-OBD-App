import client from './client'
import type {
  Shop, ShopSetupRequest,
  DiagnosticAnalyzeRequest, DiagnosticAnalyzeResponse,
  Case, ReportOut, CheckoutResponse, B2BSubscriptionStatus,
  Lead, QuoteRequest,
} from '@/types/api'

export const b2bApi = {
  // ── Shop ──────────────────────────────────────────────────────────────────
  setupShop: (data: ShopSetupRequest) =>
    client.post<Shop>('/api/v1/b2b/shop/setup', data).then(r => r.data),

  getShopProfile: () =>
    client.get<Shop>('/api/v1/b2b/shop/profile').then(r => r.data),

  updateShopProfile: (data: ShopSetupRequest) =>
    client.put<Shop>('/api/v1/b2b/shop/profile', data).then(r => r.data),

  // ── Diagnostic ────────────────────────────────────────────────────────────
  analyzeDiagnostic: (data: DiagnosticAnalyzeRequest) =>
    client.post<DiagnosticAnalyzeResponse>('/api/v1/b2b/diagnostic/analyze', data).then(r => r.data),

  // ── Cases ─────────────────────────────────────────────────────────────────
  listCases: () =>
    client.get<Case[]>('/api/v1/b2b/cases').then(r => r.data),

  getCase: (id: string) =>
    client.get<Case>(`/api/v1/b2b/cases/${id}`).then(r => r.data),

  updateCase: (id: string, data: { status?: string; symptoms_text?: string }) =>
    client.put<Case>(`/api/v1/b2b/cases/${id}`, data).then(r => r.data),

  deleteCase: (id: string) =>
    client.delete(`/api/v1/b2b/cases/${id}`),

  // ── Reports ───────────────────────────────────────────────────────────────
  generateReport: (caseId: string, data: {
    confirmed_diagnosis: string
    repair_description: string
    parts_list?: string[]
    language?: string
  }) =>
    client.post<ReportOut>(`/api/v1/b2b/cases/${caseId}/report/generate`, data).then(r => r.data),

  // ── Estimates ─────────────────────────────────────────────────────────────
  suggestEstimate: (caseId: string, data: {
    confirmed_diagnosis: string
    repair_description: string
    region?: string
  }) =>
    client.post(`/api/v1/b2b/cases/${caseId}/estimate/suggest`, data).then(r => r.data),

  saveEstimate: (caseId: string, data: object) =>
    client.put(`/api/v1/b2b/cases/${caseId}/estimate`, data).then(r => r.data),

  // ── PDF Downloads ──────────────────────────────────────────────────────────
  downloadReportPdf: (caseId: string) =>
    client.get(`/api/v1/b2b/cases/${caseId}/report/pdf`, { responseType: 'blob' }).then(r => r.data),

  downloadEstimatePdf: (caseId: string) =>
    client.get(`/api/v1/b2b/cases/${caseId}/estimate/pdf`, { responseType: 'blob' }).then(r => r.data),

  // ── Subscription ───────────────────────────────────────────────────────────
  createCheckout: (tier: 'basic' | 'pro') =>
    client.post<CheckoutResponse>('/api/v1/b2b/subscription/checkout', { tier }).then(r => r.data),

  getSubscriptionStatus: () =>
    client.get<B2BSubscriptionStatus>('/api/v1/b2b/subscription/status').then(r => r.data),

  // ── Leads ─────────────────────────────────────────────────────────────────
  getLeads: () =>
    client.get<Lead[]>('/api/v1/b2b/leads').then(r => r.data),

  getLead: (id: string) =>
    client.get<Lead>(`/api/v1/b2b/leads/${id}`).then(r => r.data),

  sendQuote: (id: string, body: QuoteRequest) =>
    client.put<Lead>(`/api/v1/b2b/leads/${id}/quote`, body).then(r => r.data),

  closeLead: (id: string) =>
    client.put<Lead>(`/api/v1/b2b/leads/${id}/close`).then(r => r.data),
}
