package com.itda.language.data.api

import com.itda.language.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<TokenResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<TokenResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<TokenResponse>

    @GET("api/v1/auth/me")
    suspend fun getMe(): Response<UserResponse>
}
