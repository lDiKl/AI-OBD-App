import { useState, useEffect } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '@/hooks/useAuth'
import { useShop } from '@/hooks/useShop'
import { b2bApi } from '@/api/b2b'

export default function SettingsPage() {
  const { user } = useAuth()
  const { shop } = useShop()
  const qc = useQueryClient()

  const [name, setName] = useState('')
  const [address, setAddress] = useState('')
  const [phone, setPhone] = useState('')
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    if (shop) {
      setName(shop.name)
      setAddress(shop.address ?? '')
      setPhone(shop.phone ?? '')
    }
  }, [shop])

  const updateProfile = useMutation({
    mutationFn: () => b2bApi.updateShopProfile({ shop_name: name, address, phone }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['shop-profile'] })
      setSaved(true)
      setTimeout(() => setSaved(false), 2500)
    },
  })

  return (
    <div className="p-8 max-w-2xl">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Settings</h1>

      {/* Account info */}
      <div className="bg-white rounded-xl border border-gray-200 p-6 mb-5">
        <h2 className="font-semibold text-gray-800 mb-4">Account</h2>
        <div className="space-y-1 text-sm">
          <p className="text-gray-500">Email: <span className="text-gray-900 font-medium">{user?.email}</span></p>
          <p className="text-gray-500">
            Plan:{' '}
            <span className={`font-medium ${shop?.subscription_tier === 'pro' ? 'text-blue-600' : 'text-gray-700'}`}>
              {shop?.subscription_tier === 'pro' ? 'Pro' : 'Basic (free)'}
            </span>
          </p>
        </div>
      </div>

      {/* Shop profile */}
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h2 className="font-semibold text-gray-800 mb-4">Shop Profile</h2>
        <div className="space-y-4">
          <div>
            <label className="block text-sm text-gray-600 mb-1">Shop Name *</label>
            <input value={name} onChange={e => setName(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <div>
            <label className="block text-sm text-gray-600 mb-1">Address</label>
            <input value={address} onChange={e => setAddress(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <div>
            <label className="block text-sm text-gray-600 mb-1">Phone</label>
            <input value={phone} onChange={e => setPhone(e.target.value)} type="tel"
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>

          <div className="flex items-center gap-3 pt-1">
            <button
              onClick={() => updateProfile.mutate()}
              disabled={updateProfile.isPending || !name}
              className="bg-blue-600 text-white text-sm font-medium px-5 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {updateProfile.isPending ? 'Saving…' : 'Save Changes'}
            </button>
            {saved && <span className="text-sm text-green-600">Saved!</span>}
            {updateProfile.isError && (
              <span className="text-sm text-red-500">Save failed. Try again.</span>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
