import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'

export default function BillingSuccessPage() {
  const navigate = useNavigate()
  const qc = useQueryClient()

  useEffect(() => {
    // Invalidate shop profile so plan tier updates on next fetch
    qc.invalidateQueries({ queryKey: ['shop-profile'] })
  }, [qc])

  return (
    <div className="flex items-center justify-center h-full">
      <div className="text-center max-w-md p-8">
        <div className="text-5xl mb-4">🎉</div>
        <h1 className="text-2xl font-bold text-gray-900 mb-2">Payment successful!</h1>
        <p className="text-gray-500 text-sm mb-6">
          Your plan is being upgraded. It may take a few seconds for the change to appear.
          If your plan has not updated within a minute, please refresh the page.
        </p>
        <button
          onClick={() => navigate('/')}
          className="bg-blue-600 text-white px-6 py-2.5 rounded-lg text-sm font-semibold hover:bg-blue-700 transition-colors"
        >
          Go to Dashboard
        </button>
      </div>
    </div>
  )
}
