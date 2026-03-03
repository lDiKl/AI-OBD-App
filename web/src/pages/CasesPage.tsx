import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { b2bApi } from '@/api/b2b'
import type { Case } from '@/types/api'

const STATUS_OPTS: Array<Case['status'] | 'all'> = ['all', 'open', 'in_progress', 'completed']

const STATUS_STYLES: Record<string, string> = {
  open: 'bg-yellow-100 text-yellow-700',
  in_progress: 'bg-blue-100 text-blue-700',
  completed: 'bg-green-100 text-green-700',
}

export default function CasesPage() {
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState<Case['status'] | 'all'>('all')

  const { data: cases = [], isLoading } = useQuery({
    queryKey: ['cases'],
    queryFn: b2bApi.listCases,
  })

  const filtered = cases.filter(c => {
    const matchesStatus = statusFilter === 'all' || c.status === statusFilter
    const q = search.toLowerCase()
    const matchesSearch = !q ||
      c.vehicle_info.make.toLowerCase().includes(q) ||
      c.vehicle_info.model.toLowerCase().includes(q) ||
      c.input_codes.some(code => code.toLowerCase().includes(q))
    return matchesStatus && matchesSearch
  })

  return (
    <div className="p-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Cases</h1>
        <Link
          to="/diagnostic"
          className="bg-blue-600 text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          + New Diagnostic
        </Link>
      </div>

      {/* Filters */}
      <div className="flex items-center gap-3 mb-5">
        <input
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="Search by make, model, or code…"
          className="flex-1 max-w-sm border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <div className="flex gap-1">
          {STATUS_OPTS.map(s => (
            <button
              key={s}
              onClick={() => setStatusFilter(s)}
              className={`text-sm px-3 py-1.5 rounded-lg transition-colors ${
                statusFilter === s
                  ? 'bg-blue-600 text-white'
                  : 'border border-gray-300 text-gray-600 hover:bg-gray-50'
              }`}
            >
              {s === 'all' ? 'All' : s.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {isLoading ? (
          <div className="p-8 text-center text-gray-400 text-sm">Loading…</div>
        ) : filtered.length === 0 ? (
          <div className="p-8 text-center text-gray-400 text-sm">No cases found</div>
        ) : (
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50">
                <th className="text-left text-xs font-medium text-gray-500 px-6 py-3">Vehicle</th>
                <th className="text-left text-xs font-medium text-gray-500 px-4 py-3">Codes</th>
                <th className="text-left text-xs font-medium text-gray-500 px-4 py-3">Status</th>
                <th className="text-left text-xs font-medium text-gray-500 px-4 py-3">Date</th>
                <th />
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filtered.map(c => (
                <tr key={c.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-6 py-4">
                    <p className="text-sm font-medium text-gray-900">
                      {c.vehicle_info.year} {c.vehicle_info.make} {c.vehicle_info.model}
                    </p>
                    <p className="text-xs text-gray-400 mt-0.5">{c.vehicle_info.engine_type}</p>
                  </td>
                  <td className="px-4 py-4">
                    <div className="flex flex-wrap gap-1">
                      {c.input_codes.map(code => (
                        <span key={code} className="font-mono text-xs bg-gray-100 px-2 py-0.5 rounded">
                          {code}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className="px-4 py-4">
                    <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${STATUS_STYLES[c.status] ?? ''}`}>
                      {c.status.replace('_', ' ')}
                    </span>
                  </td>
                  <td className="px-4 py-4 text-sm text-gray-500">
                    {new Date(c.created_at).toLocaleDateString()}
                  </td>
                  <td className="px-4 py-4 text-right">
                    <Link to={`/cases/${c.id}`} className="text-sm text-blue-600 hover:underline">
                      View
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
