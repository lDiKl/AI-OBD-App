import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { b2bApi } from '@/api/b2b'

export default function ShopSetupPage() {
  const [name, setName] = useState('')
  const [address, setAddress] = useState('')
  const [phone, setPhone] = useState('')
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const { mutate, isPending, error } = useMutation({
    mutationFn: () => b2bApi.setupShop({ shop_name: name, address, phone }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['shop-profile'] })
      navigate('/', { replace: true })
    },
  })

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-lg p-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-1">Set up your shop</h1>
        <p className="text-sm text-gray-500 mb-6">
          You're signed in. Now tell us about your auto service.
        </p>

        <form
          onSubmit={e => { e.preventDefault(); mutate() }}
          className="space-y-4"
        >
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Shop name <span className="text-red-500">*</span>
            </label>
            <input
              required
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="Garage Pro"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Address</label>
            <input
              value={address}
              onChange={e => setAddress(e.target.value)}
              placeholder="123 Main St, City"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Phone</label>
            <input
              value={phone}
              onChange={e => setPhone(e.target.value)}
              placeholder="+48 600 000 000"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>

          {error && (
            <p className="text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2">
              {(error as Error).message}
            </p>
          )}

          <button
            type="submit"
            disabled={isPending || !name}
            className="w-full bg-brand-600 hover:bg-brand-700 text-white font-medium py-2 px-4 rounded-lg text-sm transition disabled:opacity-60"
          >
            {isPending ? 'Creating…' : 'Create Shop'}
          </button>
        </form>
      </div>
    </div>
  )
}
