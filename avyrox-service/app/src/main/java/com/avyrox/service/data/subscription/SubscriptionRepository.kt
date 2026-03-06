package com.avyrox.service.data.subscription

import com.avyrox.service.data.network.CheckoutRequest
import com.avyrox.service.data.network.ShopApiService
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
