package com.driverai.b2c.data.subscription

import com.driverai.b2c.data.network.SubscriptionApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepository @Inject constructor(
    private val api: SubscriptionApiService,
) {
    suspend fun getCheckoutUrl(): String = api.getCheckoutUrl().checkoutUrl

    suspend fun getSubscriptionStatus(): String = api.getStatus().subscriptionStatus
}
