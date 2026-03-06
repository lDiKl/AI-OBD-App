import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'
import { useShop } from '@/hooks/useShop'

const navItems = [
  { to: '/', label: 'Dashboard', icon: '📊', exact: true },
  { to: '/diagnostic', label: 'AI Diagnostic', icon: '🔬', exact: false },
  { to: '/cases', label: 'Cases', icon: '📋', exact: false },
  { to: '/leads', label: 'Leads', icon: '📥', exact: false },
  { to: '/billing', label: 'Billing', icon: '💳', exact: false },
  { to: '/settings', label: 'Settings', icon: '⚙️', exact: false },
]

export default function Layout() {
  const { user, logout } = useAuth()
  const { shop } = useShop()

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Sidebar */}
      <aside className="w-60 bg-white border-r border-gray-200 flex flex-col">
        {/* Logo */}
        <div className="px-6 py-5 border-b border-gray-100">
          <h1 className="text-xl font-bold text-blue-600">Avyrox Cloud</h1>
          {shop && (
            <p className="text-xs text-gray-500 mt-1 truncate">{shop.name}</p>
          )}
        </div>

        {/* Nav links */}
        <nav className="flex-1 py-4 px-3 space-y-1">
          {navItems.map(({ to, label, icon, exact }) => (
            <NavLink
              key={to}
              to={to}
              end={exact}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-blue-50 text-blue-700'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                }`
              }
            >
              <span>{icon}</span>
              {label}
            </NavLink>
          ))}
        </nav>

        {/* User info + logout */}
        <div className="border-t border-gray-100 p-4">
          <p className="text-xs text-gray-500 truncate mb-2">{user?.email}</p>
          <button
            onClick={logout}
            className="w-full text-left text-sm text-red-500 hover:text-red-700 px-2 py-1 rounded hover:bg-red-50 transition-colors"
          >
            Sign out
          </button>
        </div>
      </aside>

      {/* Main area */}
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  )
}
