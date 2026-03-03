import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useShop } from '@/hooks/useShop'
import { b2bApi } from '@/api/b2b'

const PLANS = [
  {
    tier: 'basic' as const,
    name: 'Basic',
    price: '49',
    features: [
      'Up to 50 cases / month',
      'AI diagnostic analysis',
      'PDF reports & estimates',
      '1 team member',
    ],
  },
  {
    tier: 'pro' as const,
    name: 'Pro',
    price: '149',
    features: [
      'Unlimited cases',
      'AI diagnostic analysis',
      'PDF reports & estimates',
      'Up to 10 team members',
      'Priority AI (Claude Sonnet)',
      'Advanced analytics',
    ],
  },
]

export default function BillingPage() {
  const { shop } = useShop()
  const navigate = useNavigate()
  const [loading, setLoading] = useState<'basic' | 'pro' | null>(null)
  const [error, setError] = useState('')

  const currentTier = shop?.subscription_tier ?? 'basic'

  const handleUpgrade = async (tier: 'basic' | 'pro') => {
    setError('')
    setLoading(tier)
    try {
      const { checkout_url } = await b2bApi.createCheckout(tier)
      window.location.href = checkout_url
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : 'Unknown error'
      setError(`Failed to start checkout: ${msg}. Check Stripe configuration.`)
      setLoading(null)
    }
  }

  return (
    <div className="p-8 max-w-4xl">
      <h1 className="text-2xl font-bold text-gray-900 mb-1">Billing</h1>
      <p className="text-gray-500 text-sm mb-8">
        Current plan: <span className="font-semibold text-blue-600 capitalize">{currentTier}</span>
      </p>

      {error && (
        <div className="mb-6 bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 text-sm">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {PLANS.map((plan) => {
          const isCurrent = plan.tier === currentTier
          const isDowngrade = plan.tier === 'basic' && currentTier === 'pro'

          return (
            <div
              key={plan.tier}
              className={`rounded-xl border-2 p-6 flex flex-col ${
                isCurrent
                  ? 'border-blue-500 bg-blue-50'
                  : 'border-gray-200 bg-white'
              }`}
            >
              {isCurrent && (
                <span className="self-start text-xs font-semibold text-blue-700 bg-blue-100 px-2 py-0.5 rounded-full mb-3">
                  Current plan
                </span>
              )}

              <h2 className="text-xl font-bold text-gray-900">{plan.name}</h2>
              <div className="mt-2 mb-4">
                <span className="text-3xl font-extrabold text-gray-900">€{plan.price}</span>
                <span className="text-gray-500 text-sm"> / month</span>
              </div>

              <ul className="space-y-2 flex-1 mb-6">
                {plan.features.map((f) => (
                  <li key={f} className="flex items-start gap-2 text-sm text-gray-700">
                    <span className="text-green-500 mt-0.5">✓</span>
                    {f}
                  </li>
                ))}
              </ul>

              <button
                onClick={() => handleUpgrade(plan.tier)}
                disabled={isCurrent || isDowngrade || loading !== null}
                className={`w-full py-2.5 rounded-lg text-sm font-semibold transition-colors ${
                  isCurrent
                    ? 'bg-blue-100 text-blue-500 cursor-default'
                    : isDowngrade
                    ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                    : 'bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-60'
                }`}
              >
                {loading === plan.tier
                  ? 'Redirecting to Stripe…'
                  : isCurrent
                  ? 'Active'
                  : isDowngrade
                  ? 'Contact support to downgrade'
                  : `Upgrade to ${plan.name}`}
              </button>
            </div>
          )
        })}
      </div>

      <p className="mt-8 text-xs text-gray-400">
        Payments are processed securely by Stripe. Cancel anytime from your Stripe customer portal.
        Upgrades take effect immediately after payment.
      </p>
    </div>
  )
}
