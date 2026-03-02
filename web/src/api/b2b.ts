import client from './client'
import type { DiagnosticAnalyzeRequest, DiagnosticAnalyzeResponse, Case } from '@/types/api'

export const b2bApi = {
  analyzeDiagnostic: (data: DiagnosticAnalyzeRequest) =>
    client.post<DiagnosticAnalyzeResponse>('/api/v1/b2b/diagnostic/analyze', data).then(r => r.data),

  listCases: () =>
    client.get<Case[]>('/api/v1/b2b/cases').then(r => r.data),

  getCase: (id: string) =>
    client.get<Case>(`/api/v1/b2b/cases/${id}`).then(r => r.data),

  generateReport: (caseId: string) =>
    client.post(`/api/v1/b2b/cases/${caseId}/report/generate`).then(r => r.data),

  downloadReportPdf: (caseId: string) =>
    client.get(`/api/v1/b2b/cases/${caseId}/report/pdf`, { responseType: 'blob' }).then(r => r.data),

  suggestEstimate: (caseId: string) =>
    client.post(`/api/v1/b2b/cases/${caseId}/estimate/suggest`).then(r => r.data),
}
