package com.wavehouse.core.network

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject

/**
 * OkHttp interceptor that attaches Firebase ID Token to every request
 * as a Bearer token in the Authorization header.
 */
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.getToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}

/**
 * OkHttp interceptor for logging requests & responses in debug builds.
 */
class LoggingInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Timber.d("→ ${request.method} ${request.url}")
        val response = chain.proceed(request)
        Timber.d("← ${response.code} ${request.url}")
        return response
    }
}

/**
 * Provides the current Firebase ID Token synchronously.
 * Implemented in the auth module to avoid circular dependencies.
 */
interface TokenProvider {
    fun getToken(): String?
}
