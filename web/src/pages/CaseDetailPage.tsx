import { useState, useCallback } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { b2bApi } from '@/api/b2b'
import type { Case } from '@/types/api'

const STATUS_STYLES: Record<string, string> = {
  open: 'bg-yellow-100 text-yellow-700',
  in_progress: 'bg-blue-100 text-blue-700',
  completed: 'bg-green-100 text-green-700',
}

export default function CaseDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()

  const [activeTab, setActiveTab] = useState<'diagnosis' | 'report' | 'estimate'>('diagnosis')
  const [reportDiagnosis, setReportDiagnosis] = useState('')
  const [reportRepair, setReportRepair] = useState('')
  const [reportParts, setReportParts] = useState('')
  const [reportGenerated, setReportGenerated] = useState<Record<string, string> | null>(null)
  const [estimateDiagnosis, setEstimateDiagnosis] = useState('')
  const [estimateRepair, setEstimateRepair] = useState('')
  const [estimateSuggestion, setEstimateSuggestion] = useState<Record<string, unknown> | null>(null)
  const [pdfError, setPdfError] = useState('')

  const handleDownloadPdf = useCallback(async (type: 'report' | 'estimate') => {
    setPdfError('')
    try {
      const blob = type === 'report'
        ? await b2bApi.downloadReportPdf(id!)
        : await b2bApi.downloadEstimatePdf(id!)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${type}_${id!.slice(0, 8)}.pdf`
      a.click()
      URL.revokeObjectURL(url)
    } catch {
      setPdfError('Failed to download PDF. Try again.')
    }
  }, [id])

  const { data: c, isLoading, isError } = useQuery({
    queryKey: ['case', id],
    queryFn: () => b2bApi.getCase(id!),
    enabled: !!id,
  })

  const updateStatus = useMutation({
    mutationFn: (status: Case['status']) => b2bApi.updateCase(id!, { status }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['case', id] }),
  })

  const deleteCase = useMutation({
    mutationFn: () => b2bApi.deleteCase(id!),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['cases'] })
      navigate('/cases')
    },
  })

  const generateReport = useMutation({
    mutationFn: () => b2bApi.generateReport(id!, {
      confirmed_diagnosis: reportDiagnosis,
      repair_description: reportRepair,
      parts_list: reportParts ? reportParts.split('\n').filter(Boolean) : undefined,
    }),
    onSuccess: (data) => setReportGenerated(data as Record<string, string>),
  })

  const suggestEstimate = useMutation({
    mutationFn: () => b2bApi.suggestEstimate(id!, {
      confirmed_diagnosis: estimateDiagnosis,
      repair_description: estimateRepair,
    }),
    onSuccess: (data) => setEstimateSuggestion(data as Record<string, unknown>),
  })

  if (isLoading) return <div className="p-8 text-gray-400 text-sm">Loading…</div>
  if (isError || !c) return (
    <div className="p-8">
      <p className="text-red-500">Case not found.</p>
      <Link to="/cases" className="text-blue-600 text-sm hover:underline mt-2 block">← Back to Cases</Link>
    </div>
  )

  const aiResult = c.ai_result as {
    probable_causes?: Array<{ description: string; probability: number; reasoning: string }>
    diagnostic_sequence?: Array<{ step: number; action: string; expected_result: string }>
    parts_likely_needed?: string[]
    estimated_labor_hours?: number
    urgency?: string
    additional_notes?: string
  }

  return (
    <div className="p-8 max-w-5xl">
      {/* Breadcrumb + header */}
      <div className="mb-2">
        <Link to="/cases" className="text-sm text-gray-500 hover:text-gray-700">← Cases</Link>
      </div>
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            {c.vehicle_info.year} {c.vehicle_info.make} {c.vehicle_info.model}
          </h1>
          <p className="text-sm text-gray-500 mt-0.5">
            {c.vehicle_info.engine_type} · {c.vehicle_info.mileage?.toLocaleString()} km
          </p>
        </div>
        <div className="flex items-center gap-3">
          <select
            value={c.status}
            onChange={e => updateStatus.mutate(e.target.value as Case['status'])}
            className={`text-sm font-medium border rounded-lg px-3 py-1.5 focus:outline-none ${STATUS_STYLES[c.status] ?? ''}`}
          >
            <option value="open">open</option>
            <option value="in_progress">in progress</option>
            <option value="completed">completed</option>
          </select>
          <button
            onClick={() => {
              if (confirm('Delete this case?')) deleteCase.mutate()
            }}
            className="text-sm text-red-500 hover:text-red-700 px-3 py-1.5 rounded-lg hover:bg-red-50 transition-colors"
          >
            Delete
          </button>
        </div>
      </div>

      {/* Codes + date */}
      <div className="flex items-center gap-4 mb-6">
        <div className="flex flex-wrap gap-1">
          {c.input_codes.map(code => (
            <span key={code} className="font-mono text-sm bg-gray-100 px-2.5 py-0.5 rounded font-medium">
              {code}
            </span>
          ))}
        </div>
        <span className="text-sm text-gray-400">{new Date(c.created_at).toLocaleString()}</span>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-gray-200 mb-6">
        {(['diagnosis', 'report', 'estimate'] as const).map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-5 py-2.5 text-sm font-medium capitalize border-b-2 transition-colors -mb-px ${
              activeTab === tab
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* Tab: Diagnosis */}
      {activeTab === 'diagnosis' && (
        <div className="space-y-5">
          {c.symptoms_text && (
            <div className="bg-white rounded-xl border border-gray-200 p-5">
              <h3 className="text-sm font-semibold text-gray-700 mb-2">Symptoms</h3>
              <p className="text-sm text-gray-600">{c.symptoms_text}</p>
            </div>
          )}

          {aiResult.probable_causes && aiResult.probable_causes.length > 0 ? (
            <>
              <div className="bg-white rounded-xl border border-gray-200 p-5">
                <h3 className="text-sm font-semibold text-gray-700 mb-3">Probable Causes</h3>
                <div className="space-y-3">
                  {aiResult.probable_causes.map((cause, i) => (
                    <div key={i} className="flex gap-4 items-start">
                      <span className="text-base font-bold text-blue-600 w-10 shrink-0">{cause.probability}%</span>
                      <div>
                        <p className="text-sm font-medium text-gray-900">{cause.description}</p>
                        <p className="text-xs text-gray-500 mt-0.5">{cause.reasoning}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {aiResult.diagnostic_sequence && aiResult.diagnostic_sequence.length > 0 && (
                <div className="bg-white rounded-xl border border-gray-200 p-5">
                  <h3 className="text-sm font-semibold text-gray-700 mb-3">Diagnostic Steps</h3>
                  <ol className="space-y-2">
                    {aiResult.diagnostic_sequence.map(s => (
                      <li key={s.step} className="flex gap-3 items-start">
                        <span className="w-6 h-6 rounded-full bg-blue-100 text-blue-700 text-xs font-bold flex items-center justify-center shrink-0">
                          {s.step}
                        </span>
                        <div>
                          <p className="text-sm text-gray-900">{s.action}</p>
                          <p className="text-xs text-gray-400">Expected: {s.expected_result}</p>
                        </div>
                      </li>
                    ))}
                  </ol>
                </div>
              )}

              {aiResult.parts_likely_needed && aiResult.parts_likely_needed.length > 0 && (
                <div className="bg-white rounded-xl border border-gray-200 p-5">
                  <h3 className="text-sm font-semibold text-gray-700 mb-2">Parts Likely Needed</h3>
                  <ul className="space-y-1">
                    {aiResult.parts_likely_needed.map((p, i) => (
                      <li key={i} className="text-sm text-gray-700 flex gap-2">
                        <span className="text-gray-400">•</span>{p}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </>
          ) : (
            <div className="bg-gray-50 rounded-xl border border-dashed border-gray-200 p-8 text-center">
              <p className="text-sm text-gray-400">No AI analysis yet.</p>
              <Link to="/diagnostic" className="text-blue-600 text-sm hover:underline mt-2 block">
                Run AI Diagnostic →
              </Link>
            </div>
          )}
        </div>
      )}

      {/* Tab: Report */}
      {activeTab === 'report' && (
        <div className="space-y-5">
          {c.client_report_text ? (
            <div className="bg-white rounded-xl border border-gray-200 p-5">
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-semibold text-gray-700">Client Report (saved)</h3>
                <button
                  onClick={() => handleDownloadPdf('report')}
                  className="flex items-center gap-1.5 text-sm text-blue-600 hover:text-blue-800 font-medium px-3 py-1.5 rounded-lg hover:bg-blue-50 transition-colors border border-blue-200"
                >
                  ↓ Download PDF
                </button>
              </div>
              {pdfError && <p className="text-xs text-red-500 mb-2">{pdfError}</p>}
              <pre className="text-sm text-gray-700 whitespace-pre-wrap font-sans">{c.client_report_text}</pre>
            </div>
          ) : null}

          {reportGenerated ? (
            <div className="bg-green-50 rounded-xl border border-green-200 p-5">
              <h3 className="text-sm font-semibold text-green-800 mb-3">Generated Report</h3>
              {Object.entries(reportGenerated).map(([k, v]) => (
                <div key={k} className="mb-3">
                  <p className="text-xs font-semibold text-green-700 uppercase tracking-wide mb-0.5">
                    {k.replace(/_/g, ' ')}
                  </p>
                  <p className="text-sm text-gray-700">{String(v)}</p>
                </div>
              ))}
            </div>
          ) : (
            <div className="bg-white rounded-xl border border-gray-200 p-5">
              <h3 className="text-sm font-semibold text-gray-700 mb-4">Generate Client Report</h3>
              <div className="space-y-3">
                <div>
                  <label className="block text-sm text-gray-600 mb-1">Confirmed diagnosis *</label>
                  <input value={reportDiagnosis} onChange={e => setReportDiagnosis(e.target.value)}
                    placeholder="e.g. Faulty catalytic converter"
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
                </div>
                <div>
                  <label className="block text-sm text-gray-600 mb-1">Repair description *</label>
                  <textarea value={reportRepair} onChange={e => setReportRepair(e.target.value)} rows={3}
                    placeholder="Replaced catalytic converter, cleared codes, test drove…"
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none" />
                </div>
                <div>
                  <label className="block text-sm text-gray-600 mb-1">
                    Parts replaced <span className="text-gray-400">(one per line, optional)</span>
                  </label>
                  <textarea value={reportParts} onChange={e => setReportParts(e.target.value)} rows={3}
                    placeholder="Catalytic converter&#10;O2 sensor"
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none" />
                </div>
                <button
                  onClick={() => generateReport.mutate()}
                  disabled={generateReport.isPending || !reportDiagnosis || !reportRepair}
                  className="bg-blue-600 text-white text-sm font-medium px-5 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
                >
                  {generateReport.isPending ? 'Generating…' : 'Generate Report →'}
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Tab: Estimate */}
      {activeTab === 'estimate' && (
        <div className="space-y-5">
          {c.estimate && (
            <div className="bg-white rounded-xl border border-gray-200 p-5">
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-semibold text-gray-700">Saved Estimate</h3>
                <button
                  onClick={() => handleDownloadPdf('estimate')}
                  className="flex items-center gap-1.5 text-sm text-blue-600 hover:text-blue-800 font-medium px-3 py-1.5 rounded-lg hover:bg-blue-50 transition-colors border border-blue-200"
                >
                  ↓ Download PDF
                </button>
              </div>
              {pdfError && <p className="text-xs text-red-500 mb-2">{pdfError}</p>}
              <pre className="text-xs text-gray-600 whitespace-pre-wrap font-mono bg-gray-50 rounded-lg p-3">
                {JSON.stringify(c.estimate, null, 2)}
              </pre>
            </div>
          )}

          {estimateSuggestion ? (
            <div className="bg-blue-50 rounded-xl border border-blue-200 p-5">
              <h3 className="text-sm font-semibold text-blue-800 mb-3">AI Estimate Suggestion</h3>
              <pre className="text-sm text-gray-700 whitespace-pre-wrap font-sans">
                {JSON.stringify(estimateSuggestion, null, 2)}
              </pre>
              <button
                onClick={() => setEstimateSuggestion(null)}
                className="mt-3 text-sm text-blue-600 hover:underline"
              >
                Start over
              </button>
            </div>
          ) : (
            <div className="bg-white rounded-xl border border-gray-200 p-5">
              <h3 className="text-sm font-semibold text-gray-700 mb-4">Get AI Estimate</h3>
              <div className="space-y-3">
                <div>
                  <label className="block text-sm text-gray-600 mb-1">Confirmed diagnosis *</label>
                  <input value={estimateDiagnosis} onChange={e => setEstimateDiagnosis(e.target.value)}
                    placeholder="e.g. Faulty catalytic converter"
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
                </div>
                <div>
                  <label className="block text-sm text-gray-600 mb-1">Repair description *</label>
                  <textarea value={estimateRepair} onChange={e => setEstimateRepair(e.target.value)} rows={3}
                    placeholder="Replace catalytic converter and O2 sensor…"
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none" />
                </div>
                <button
                  onClick={() => suggestEstimate.mutate()}
                  disabled={suggestEstimate.isPending || !estimateDiagnosis || !estimateRepair}
                  className="bg-blue-600 text-white text-sm font-medium px-5 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
                >
                  {suggestEstimate.isPending ? 'Estimating…' : 'Get AI Estimate →'}
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
