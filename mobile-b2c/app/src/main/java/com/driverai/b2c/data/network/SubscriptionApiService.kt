package com.driverai.b2c.data.network

import retrofit2.http.GET
import retrofit2.http.POST

data class CheckoutResponse(val checkoutUrl: String)
data class SubscriptionStatusResponse(val subscriptionStatus: String)

interface SubscriptionApiService {
    @POST("api/v1/b2c/subscription/checkout")
    suspend fun getCheckoutUrl(): CheckoutResponse

    @GET("api/v1/b2c/subscription/status")
    suspend fun getStatus(): SubscriptionStatusResponse
}
