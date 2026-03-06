package com.avyrox.drive.data.network

import com.avyrox.drive.data.auth.FirebaseAuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Attaches Firebase ID token to every API request as:
 *   Authorization: Bearer <token>
 * If user is not signed in, the request goes through without a header.
 */
class AuthInterceptor @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { authRepository.getIdToken() }
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
