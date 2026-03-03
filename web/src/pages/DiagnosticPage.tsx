import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { b2bApi } from '@/api/b2b'
import type { DiagnosticAnalyzeResponse, FreezeFrame } from '@/types/api'

const URGENCY_STYLES: Record<string, string> = {
  immediate: 'bg-red-100 text-red-700 border-red-200',
  within_week: 'bg-yellow-100 text-yellow-700 border-yellow-200',
  routine: 'bg-green-100 text-green-700 border-green-200',
}

export default function DiagnosticPage() {
  const navigate = useNavigate()

  const [make, setMake] = useState('')
  const [model, setModel] = useState('')
  const [year, setYear] = useState('')
  const [engine, setEngine] = useState('')
  const [mileage, setMileage] = useState('')
  const [codesInput, setCodesInput] = useState('')
  const [symptoms, setSymptoms] = useState('')
  const [rpm, setRpm] = useState('')
  const [coolant, setCoolant] = useState('')
  const [load, setLoad] = useState('')
  const [speed, setSpeed] = useState('')
  const [saveAsCase, setSaveAsCase] = useState(true)
  const [result, setResult] = useState<DiagnosticAnalyzeResponse | null>(null)

  const mutation = useMutation({
    mutationFn: b2bApi.analyzeDiagnostic,
    onSuccess: (data) => setResult(data),
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const codes = codesInput.split(/[\s,]+/).map(s => s.trim().toUpperCase()).filter(Boolean)
    if (!codes.length) return

    const freeze: FreezeFrame = {}
    if (rpm) freeze.rpm = Number(rpm)
    if (coolant) freeze.coolant_temp_c = Number(coolant)
    if (load) freeze.engine_load_pct = Number(load)
    if (speed) freeze.vehicle_speed_kmh = Number(speed)

    mutation.mutate({
      vehicle_info: { make, model, year: Number(year), engine_type: engine, mileage: Number(mileage) },
      codes,
      symptoms: symptoms || undefined,
      freeze_frame: Object.keys(freeze).length ? freeze : undefined,
      save_as_case: saveAsCase,
    })
  }

  return (
    <div className="p-8 max-w-4xl">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">AI Diagnostic Analysis</h1>

      {!result ? (
        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Vehicle info */}
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <h2 className="font-semibold text-gray-800 mb-4">Vehicle Info</h2>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm text-gray-600 mb-1">Make *</label>
                <input value={make} onChange={e => setMake(e.target.value)} required
                  placeholder="e.g. BMW" className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1">Model *</label>
                <input value={model} onChange={e => setModel(e.target.value)} required
                  placeholder="e.g. 320d" className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1">Year *</label>
                <input value={year} onChange={e => setYear(e.target.value)} required type="number"
                  placeholder="2019" className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1">Engine Type *</label>
                <input value={engine} onChange={e => setEngine(e.target.value)} required
                  placeholder="2.0L Diesel" className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1">Mileage (km) *</label>
                <input value={mileage} onChange={e => setMileage(e.target.value)} required type="number"
                  placeholder="120000" className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
              </div>
            </div>
          </div>

          {/* Codes + symptoms */}
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <h2 className="font-semibold text-gray-800 mb-4">Fault Codes & Symptoms</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm text-gray-600 mb-1">DTC Codes * (space or comma separated)</label>
                <input value={codesInput} onChange={e => setCodesInput(e.target.value)} required
                  placeholder="P0420 P0171 C0300" className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-500" />
              </div>
              <div>
                <label className="block text-sm text-gray-600 mb-1">Symptoms (optional)</label>
                <textarea value={symptoms} onChange={e => setSymptoms(e.target.value)} rows={3}
                  placeholder="Engine hesitates on acceleration, fuel smell…"
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none" />
              </div>
            </div>
          </div>

          {/* Freeze frame */}
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <h2 className="font-semibold text-gray-800 mb-1">
              Freeze Frame Data <span className="text-gray-400 font-normal text-sm">(optional)</span>
            </h2>
            <p className="text-xs text-gray-500 mb-4">Values captured at the moment the fault occurred</p>
            <div className="grid grid-cols-4 gap-4">
              {[
                { label: 'RPM', value: rpm, set: setRpm, placeholder: '1200' },
                { label: 'Coolant °C', value: coolant, set: setCoolant, placeholder: '90' },
                { label: 'Engine Load %', value: load, set: setLoad, placeholder: '45' },
                { label: 'Speed km/h', value: speed, set: setSpeed, placeholder: '80' },
              ].map(({ label, value, set, placeholder }) => (
                <div key={label}>
                  <label className="block text-sm text-gray-600 mb-1">{label}</label>
                  <input value={value} onChange={e => set(e.target.value)} type="number"
                    placeholder={placeholder} className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
                </div>
              ))}
            </div>
          </div>

          <div className="flex items-center justify-between">
            <label className="flex items-center gap-2 text-sm text-gray-600 cursor-pointer select-none">
              <input type="checkbox" checked={saveAsCase} onChange={e => setSaveAsCase(e.target.checked)}
                className="rounded" />
              Save as case
            </label>
            <button type="submit" disabled={mutation.isPending}
              className="bg-blue-600 text-white font-medium px-6 py-2.5 rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors">
              {mutation.isPending ? 'Analyzing…' : 'Analyze →'}
            </button>
          </div>

          {mutation.isError && (
            <p className="text-sm text-red-600 bg-red-50 rounded-lg px-4 py-3">
              Analysis failed. Please check your input and try again.
            </p>
          )}
        </form>
      ) : (
        <div className="space-y-6">
          {/* Header bar */}
          <div className="flex items-center justify-between">
            <div className={`inline-flex items-center gap-2 border rounded-lg px-3 py-1.5 text-sm font-medium ${URGENCY_STYLES[result.urgency] ?? ''}`}>
              Urgency: {result.urgency.replace('_', ' ')}
            </div>
            <div className="flex gap-3">
              <button onClick={() => setResult(null)}
                className="border border-gray-300 text-sm px-4 py-2 rounded-lg hover:bg-gray-50 transition-colors">
                New Analysis
              </button>
              {result.case_id && (
                <button onClick={() => navigate(`/cases/${result.case_id}`)}
                  className="bg-blue-600 text-white text-sm px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors">
                  Open Case →
                </button>
              )}
            </div>
          </div>

          {/* Probable causes */}
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <h2 className="font-semibold text-gray-800 mb-4">Probable Causes</h2>
            <div className="space-y-3">
              {result.probable_causes.map((c, i) => (
                <div key={i} className="flex items-start gap-4">
                  <div className="w-12 shrink-0 text-center">
                    <span className="text-lg font-bold text-blue-600">{c.probability}%</span>
                  </div>
                  <div>
                    <p className="text-sm font-medium text-gray-900">{c.description}</p>
                    <p className="text-xs text-gray-500 mt-0.5">{c.reasoning}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Diagnostic steps */}
          {result.diagnostic_sequence.length > 0 && (
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <h2 className="font-semibold text-gray-800 mb-4">Diagnostic Steps</h2>
              <ol className="space-y-3">
                {result.diagnostic_sequence.map(s => (
                  <li key={s.step} className="flex gap-3">
                    <span className="w-6 h-6 rounded-full bg-blue-100 text-blue-700 text-xs font-bold flex items-center justify-center shrink-0 mt-0.5">
                      {s.step}
                    </span>
                    <div>
                      <p className="text-sm font-medium text-gray-900">{s.action}</p>
                      <p className="text-xs text-gray-500">Expected: {s.expected_result}</p>
                    </div>
                  </li>
                ))}
              </ol>
            </div>
          )}

          {/* Parts + labor */}
          <div className="grid grid-cols-2 gap-4">
            {result.parts_likely_needed.length > 0 && (
              <div className="bg-white rounded-xl border border-gray-200 p-6">
                <h2 className="font-semibold text-gray-800 mb-3">Parts Likely Needed</h2>
                <ul className="space-y-1">
                  {result.parts_likely_needed.map((p, i) => (
                    <li key={i} className="text-sm text-gray-700 flex gap-2">
                      <span className="text-gray-400">•</span> {p}
                    </li>
                  ))}
                </ul>
              </div>
            )}
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <h2 className="font-semibold text-gray-800 mb-3">Estimate</h2>
              <p className="text-sm text-gray-600">
                Labor: <span className="font-medium text-gray-900">{result.estimated_labor_hours}h</span>
              </p>
              {result.additional_notes && (
                <p className="text-xs text-gray-500 mt-3">{result.additional_notes}</p>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
