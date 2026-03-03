import { useQuery } from '@tanstack/react-query'
import { b2bApi } from '@/api/b2b'
import { useAuth } from './useAuth'

export function useShop() {
  const { user } = useAuth()

  const { data: shop, isLoading, error } = useQuery({
    queryKey: ['shop-profile'],
    queryFn: () => b2bApi.getShopProfile(),
    enabled: !!user,
    retry: false,  // Don't retry 403 (not registered)
  })

  const isNotRegistered = !!(error && (error as any)?.response?.status === 403)

  return { shop, loading: isLoading, isNotRegistered }
}
