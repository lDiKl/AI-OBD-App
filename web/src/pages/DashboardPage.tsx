import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { b2bApi } from '@/api/b2b'
import type { Case } from '@/types/api'

function StatusBadge({ status }: { status: Case['status'] }) {
  const styles = {
    open: 'bg-yellow-100 text-yellow-700',
    in_progress: 'bg-blue-100 text-blue-700',
    completed: 'bg-green-100 text-green-700',
  }
  return (
    <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${styles[status]}`}>
      {status.replace('_', ' ')}
    </span>
  )
}

export default function DashboardPage() {
  const { data: cases = [], isLoading } = useQuery({
    queryKey: ['cases'],
    queryFn: b2bApi.listCases,
  })

  const open = cases.filter(c => c.status === 'open').length
  const inProgress = cases.filter(c => c.status === 'in_progress').length
  const completed = cases.filter(c => c.status === 'completed').length
  const recent = cases.slice(0, 5)

  return (
    <div className="p-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <Link
          to="/diagnostic"
          className="bg-blue-600 text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          + New Diagnostic
        </Link>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4 mb-8">
        {[
          { label: 'Open', value: open, color: 'text-yellow-600' },
          { label: 'In Progress', value: inProgress, color: 'text-blue-600' },
          { label: 'Completed', value: completed, color: 'text-green-600' },
        ].map(({ label, value, color }) => (
          <div key={label} className="bg-white rounded-xl border border-gray-200 p-5">
            <p className="text-sm text-gray-500">{label}</p>
            <p className={`text-3xl font-bold mt-1 ${color}`}>{value}</p>
          </div>
        ))}
      </div>

      {/* Recent cases */}
      <div className="bg-white rounded-xl border border-gray-200">
        <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
          <h2 className="font-semibold text-gray-800">Recent Cases</h2>
          <Link to="/cases" className="text-sm text-blue-600 hover:underline">View all</Link>
        </div>

        {isLoading ? (
          <div className="p-6 text-center text-gray-400 text-sm">Loading…</div>
        ) : recent.length === 0 ? (
          <div className="p-6 text-center text-gray-400 text-sm">
            No cases yet.{' '}
            <Link to="/diagnostic" className="text-blue-600 hover:underline">Run a diagnostic</Link>
          </div>
        ) : (
          <ul className="divide-y divide-gray-100">
            {recent.map(c => (
              <li key={c.id}>
                <Link to={`/cases/${c.id}`} className="flex items-center gap-4 px-6 py-4 hover:bg-gray-50 transition-colors">
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-900 truncate">
                      {c.vehicle_info.year} {c.vehicle_info.make} {c.vehicle_info.model}
                    </p>
                    <p className="text-xs text-gray-500 mt-0.5">
                      {c.input_codes.join(', ') || 'No codes'} · {new Date(c.created_at).toLocaleDateString()}
                    </p>
                  </div>
                  <StatusBadge status={c.status} />
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
