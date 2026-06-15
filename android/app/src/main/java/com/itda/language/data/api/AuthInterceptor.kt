package com.itda.language.data.api

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * JWT access_token을 자동으로 Authorization 헤더에 추가하는 인터셉터.
 * 토큰이 없으면 헤더를 추가하지 않는다.
 */
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenManager.getAccessTokenSync()

        val request = if (token != null) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        return chain.proceed(request)
    }
}
