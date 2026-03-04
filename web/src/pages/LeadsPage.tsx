import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { b2bApi } from '@/api/b2b'
import type { Lead, QuoteRequest } from '@/types/api'

function statusBadge(status: Lead['status']) {
  const map = {
    pending:  { label: 'Pending',  cls: 'bg-yellow-100 text-yellow-800' },
    quoted:   { label: 'Quoted',   cls: 'bg-blue-100   text-blue-800'   },
    closed:   { label: 'Closed',   cls: 'bg-gray-100   text-gray-600'   },
  }
  const { label, cls } = map[status]
  return (
    <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${cls}`}>
      {label}
    </span>
  )
}

function QuoteForm({ lead, onDone }: { lead: Lead; onDone: () => void }) {
  const qc = useQueryClient()
  const [costMin, setCostMin] = useState(lead.quote?.cost_min?.toString() ?? '')
  const [costMax, setCostMax] = useState(lead.quote?.cost_max?.toString() ?? '')
  const [days, setDays]       = useState(lead.quote?.estimated_days?.toString() ?? '')
  const [notes, setNotes]     = useState(lead.quote?.notes ?? '')
  const [err, setErr]         = useState('')

  const mutation = useMutation({
    mutationFn: (body: QuoteRequest) => b2bApi.sendQuote(lead.lead_id, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['b2b-leads'] })
      onDone()
    },
    onError: () => setErr('Failed to send quote. Please try again.'),
  })

  const submit = () => {
    setErr('')
    const body: QuoteRequest = {
      cost_min: parseFloat(costMin),
      cost_max: parseFloat(costMax),
      estimated_days: parseInt(days),
      notes: notes || undefined,
    }
    if (isNaN(body.cost_min) || isNaN(body.cost_max) || isNaN(body.estimated_days)) {
      setErr('Please fill in all required fields with valid numbers.')
      return
    }
    mutation.mutate(body)
  }

  return (
    <div className="mt-3 p-4 bg-blue-50 rounded-lg border border-blue-200 space-y-3">
      <p className="text-sm font-medium text-blue-900">Send Quote</p>
      <div className="grid grid-cols-3 gap-3">
        <div>
          <label className="block text-xs text-gray-600 mb-1">Min Cost (€) *</label>
          <input
            type="number" value={costMin} onChange={e => setCostMin(e.target.value)}
            className="w-full border border-gray-300 rounded px-2 py-1 text-sm"
          />
        </div>
        <div>
          <label className="block text-xs text-gray-600 mb-1">Max Cost (€) *</label>
          <input
            type="number" value={costMax} onChange={e => setCostMax(e.target.value)}
            className="w-full border border-gray-300 rounded px-2 py-1 text-sm"
          />
        </div>
        <div>
          <label className="block text-xs text-gray-600 mb-1">Days *</label>
          <input
            type="number" value={days} onChange={e => setDays(e.target.value)}
            className="w-full border border-gray-300 rounded px-2 py-1 text-sm"
          />
        </div>
      </div>
      <div>
        <label className="block text-xs text-gray-600 mb-1">Notes</label>
        <textarea
          value={notes} onChange={e => setNotes(e.target.value)}
          rows={2}
          className="w-full border border-gray-300 rounded px-2 py-1 text-sm resize-none"
        />
      </div>
      {err && <p className="text-xs text-red-600">{err}</p>}
      <div className="flex gap-2">
        <button
          onClick={submit}
          disabled={mutation.isPending}
          className="px-4 py-1.5 bg-blue-600 text-white text-sm rounded hover:bg-blue-700 disabled:opacity-50"
        >
          {mutation.isPending ? 'Sending…' : 'Send Quote'}
        </button>
        <button
          onClick={onDone}
          className="px-4 py-1.5 border border-gray-300 text-sm rounded hover:bg-gray-50"
        >
          Cancel
        </button>
      </div>
    </div>
  )
}

function LeadRow({ lead }: { lead: Lead }) {
  const [expanded, setExpanded] = useState(false)
  const [showQuoteForm, setShowQuoteForm] = useState(false)
  const qc = useQueryClient()

  const closeMutation = useMutation({
    mutationFn: () => b2bApi.closeLead(lead.lead_id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['b2b-leads'] }),
  })

  const vehicleLabel = [lead.vehicle_info.make, lead.vehicle_info.model, lead.vehicle_info.year]
    .filter(Boolean).join(' ')

  const dtcLabel = lead.dtc_codes.length > 0
    ? lead.dtc_codes.join(', ')
    : '—'

  return (
    <div className="border border-gray-200 rounded-lg bg-white overflow-hidden">
      {/* Row header */}
      <button
        className="w-full flex items-center justify-between px-4 py-3 hover:bg-gray-50 text-left"
        onClick={() => setExpanded(v => !v)}
      >
        <div className="flex items-center gap-4 min-w-0">
          <div className="min-w-0">
            <p className="font-medium text-sm truncate">{vehicleLabel || 'Unknown vehicle'}</p>
            <p className="text-xs text-gray-500 mt-0.5">{lead.user_email}</p>
          </div>
          <p className="text-xs text-gray-400 font-mono truncate max-w-[180px]">{dtcLabel}</p>
        </div>
        <div className="flex items-center gap-3 shrink-0">
          {statusBadge(lead.status)}
          <span className="text-xs text-gray-400">
            {new Date(lead.created_at).toLocaleDateString()}
          </span>
          <span className="text-gray-400 text-sm">{expanded ? '▲' : '▼'}</span>
        </div>
      </button>

      {/* Expanded detail */}
      {expanded && (
        <div className="px-4 pb-4 border-t border-gray-100 pt-3 space-y-3">
          {/* DTC codes */}
          {lead.dtc_codes.length > 0 && (
            <div>
              <p className="text-xs font-medium text-gray-600 mb-1">DTC Codes</p>
              <div className="flex flex-wrap gap-1">
                {lead.dtc_codes.map(c => (
                  <span key={c} className="px-2 py-0.5 bg-red-50 text-red-700 text-xs font-mono rounded">{c}</span>
                ))}
              </div>
            </div>
          )}

          {/* Vehicle info */}
          <div>
            <p className="text-xs font-medium text-gray-600 mb-1">Vehicle Info</p>
            <div className="text-xs text-gray-700 space-y-0.5">
              {Object.entries(lead.vehicle_info).map(([k, v]) => (
                <div key={k}><span className="text-gray-400 capitalize">{k.replace('_', ' ')}: </span>{v}</div>
              ))}
            </div>
          </div>

          {/* Freeze frame */}
          {lead.freeze_frame && Object.keys(lead.freeze_frame).length > 0 && (
            <div>
              <p className="text-xs font-medium text-gray-600 mb-1">Freeze Frame</p>
              <div className="text-xs text-gray-700 font-mono bg-gray-50 p-2 rounded grid grid-cols-2 gap-x-4 gap-y-0.5">
                {Object.entries(lead.freeze_frame).map(([k, v]) => (
                  <div key={k}><span className="text-gray-400">{k}: </span>{String(v)}</div>
                ))}
              </div>
            </div>
          )}

          {/* Existing quote */}
          {lead.quote && (
            <div className="p-3 bg-blue-50 rounded-lg border border-blue-100">
              <p className="text-xs font-medium text-blue-900 mb-1">Sent Quote</p>
              <p className="text-sm text-blue-800">
                €{lead.quote.cost_min}–€{lead.quote.cost_max} · ~{lead.quote.estimated_days} days
              </p>
              {lead.quote.notes && (
                <p className="text-xs text-blue-700 mt-1">{lead.quote.notes}</p>
              )}
            </div>
          )}

          {/* Actions */}
          {lead.status !== 'closed' && (
            <div className="flex gap-2 pt-1">
              {!showQuoteForm && (
                <button
                  onClick={() => setShowQuoteForm(true)}
                  className="px-3 py-1.5 bg-blue-600 text-white text-sm rounded hover:bg-blue-700"
                >
                  {lead.quote ? 'Update Quote' : 'Send Quote'}
                </button>
              )}
              <button
                onClick={() => closeMutation.mutate()}
                disabled={closeMutation.isPending}
                className="px-3 py-1.5 border border-gray-300 text-sm rounded hover:bg-gray-50 disabled:opacity-50"
              >
                {closeMutation.isPending ? 'Closing…' : 'Close Lead'}
              </button>
            </div>
          )}

          {showQuoteForm && (
            <QuoteForm lead={lead} onDone={() => setShowQuoteForm(false)} />
          )}
        </div>
      )}
    </div>
  )
}

export default function LeadsPage() {
  const { data: leads, isLoading, isError } = useQuery({
    queryKey: ['b2b-leads'],
    queryFn: b2bApi.getLeads,
    refetchInterval: 30_000,
  })

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Incoming Leads</h1>
        <p className="text-sm text-gray-500 mt-1">Diagnostic reports sent by drivers who need service</p>
      </div>

      {isLoading && (
        <div className="flex justify-center py-16 text-gray-400">Loading leads…</div>
      )}
      {isError && (
        <div className="py-8 text-center text-red-500 text-sm">Failed to load leads.</div>
      )}

      {leads && leads.length === 0 && (
        <div className="py-16 text-center text-gray-400">
          <p className="text-lg">No leads yet</p>
          <p className="text-sm mt-1">When drivers send their diagnostics to your shop, they'll appear here.</p>
        </div>
      )}

      {leads && leads.length > 0 && (
        <div className="space-y-2">
          {leads.map(lead => (
            <LeadRow key={lead.lead_id} lead={lead} />
          ))}
        </div>
      )}
    </div>
  )
}
