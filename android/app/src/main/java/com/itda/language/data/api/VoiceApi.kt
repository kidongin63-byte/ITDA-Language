package com.itda.language.data.api

import com.itda.language.data.model.VoiceCategoryList
import com.itda.language.data.model.VoicePersona
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface VoiceApi {

    @GET("api/v1/voices/categories")
    suspend fun getCategories(): Response<VoiceCategoryList>

    @GET("api/v1/voices/personas")
    suspend fun getPersonas(
        @Query("gender") gender: String? = null,
        @Query("age_group") ageGroup: String? = null,
        @Query("region") region: String? = null,
        @Query("tone") tone: String? = null,
    ): Response<List<VoicePersona>>

    @POST("api/v1/voices/lock")
    suspend fun lockVoice(@Body body: VoiceLockRequest): Response<Unit>

    @GET("api/v1/voices/lock-status")
    suspend fun getLockStatus(): Response<VoiceLockStatusResponse>

    @POST("api/v1/voices/change-request")
    suspend fun requestVoiceChange(@Body body: VoiceChangeRequestBody): Response<Unit>
}
