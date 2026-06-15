package com.itda.language.data.api

import com.itda.language.data.model.PhraseCreateRequest
import com.itda.language.data.model.PhraseResponse
import retrofit2.Response
import retrofit2.http.*

interface PhraseApi {

    @GET("api/v1/phrases/presets")
    suspend fun getPresets(): Response<Map<String, List<String>>>

    @POST("api/v1/phrases/presets/add-all")
    suspend fun addAllPresets(): Response<List<PhraseResponse>>

    @GET("api/v1/phrases")
    suspend fun listPhrases(@Query("category") category: String? = null): Response<List<PhraseResponse>>

    @POST("api/v1/phrases")
    suspend fun createPhrase(@Body body: PhraseCreateRequest): Response<PhraseResponse>

    @POST("api/v1/phrases/{phraseId}/use")
    suspend fun incrementUsage(@Path("phraseId") phraseId: String): Response<Map<String, Int>>

    @DELETE("api/v1/phrases/{phraseId}")
    suspend fun deletePhrase(@Path("phraseId") phraseId: String): Response<Unit>
}
