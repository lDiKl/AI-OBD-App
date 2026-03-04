package com.shopai.b2b.data.subscription

import com.shopai.b2b.data.network.CheckoutRequest
import com.shopai.b2b.data.network.ShopApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepository @Inject constructor(
    private val api: ShopApiService,
) {
    suspend fun getCheckoutUrl(tier: String): String =
        api.getCheckoutUrl(CheckoutRequest(tier)).checkoutUrl

    suspend fun getSubscriptionTier(): String =
        api.getSubscriptionStatus().subscriptionTier
}
