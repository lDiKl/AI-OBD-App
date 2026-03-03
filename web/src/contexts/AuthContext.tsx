import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { getAuth, onAuthStateChanged, signOut, type User } from 'firebase/auth'
import '@/lib/firebase'

interface AuthContextValue {
  user: User | null
  loading: boolean
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // Single subscription for the entire app — no race conditions
    const unsubscribe = onAuthStateChanged(getAuth(), (u) => {
      setUser(u)
      setLoading(false)
    })
    return unsubscribe
  }, [])

  const logout = () => signOut(getAuth())

  return (
    <AuthContext.Provider value={{ user, loading, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
