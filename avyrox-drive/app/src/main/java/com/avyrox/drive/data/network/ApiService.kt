package com.avyrox.drive.data.network

import retrofit2.http.POST

/**
 * Retrofit API interface for DriverAI backend.
 * Endpoints will be added here as features are built (scan, vehicles, history…).
 */
interface ApiService {

    /**
     * Called after Firebase sign-in to register the user in the backend DB
     * and retrieve the user profile (subscription status, region, etc.).
     * No body needed — the backend reads the Firebase UID from the Bearer token.
     */
    @POST("api/v1/auth/verify")
    suspend fun verifyAuth(): AuthVerifyResponse
}

data class AuthVerifyResponse(
    val uid: String,
    val email: String?,
    val subscriptionStatus: String,
    val region: String,
    val language: String,
)
