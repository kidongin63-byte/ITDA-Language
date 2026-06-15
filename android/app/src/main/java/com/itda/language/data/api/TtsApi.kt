package com.itda.language.data.api

import com.itda.language.data.model.TtsSynthesizeRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

interface TtsApi {

    @Streaming
    @POST("api/v1/tts/synthesize")
    suspend fun synthesize(@Body body: TtsSynthesizeRequest): Response<ResponseBody>
}
