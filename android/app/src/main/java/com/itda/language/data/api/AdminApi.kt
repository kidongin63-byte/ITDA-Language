package com.itda.language.data.api

import retrofit2.Response
import retrofit2.http.*

data class PinVerifyRequest(val pin: String)
data class PinVerifyResponse(val valid: Boolean, val is_admin: Boolean)
data class AdminActionRequest(val notes: String = "")
data class VoiceChangeRequestDto(
    val id: String,
    val user_id: String,
    val user_email: String,
    val user_nickname: String,
    val current_voice: String?,
    val requested_speaker: String,
    val requested_voice_name: String,
    val reason: String,
    val status: String,
    val admin_notes: String?,
    val created_at: String,
)
data class VoiceLockRequest(val speaker: String, val voice_name: String)
data class VoiceChangeRequestBody(
    val requested_speaker: String,
    val requested_voice_name: String,
    val reason: String = "",
)
data class VoiceLockStatusResponse(val locked: Boolean, val speaker: String?, val voice_name: String?)

interface AdminApi {
    @POST("api/v1/admin/verify-pin")
    suspend fun verifyPin(@Body body: PinVerifyRequest): Response<PinVerifyResponse>

    @GET("api/v1/admin/requests")
    suspend fun getChangeRequests(@Query("status") status: String = "pending"): Response<List<VoiceChangeRequestDto>>

    @POST("api/v1/admin/requests/{requestId}/approve")
    suspend fun approveRequest(@Path("requestId") id: String, @Body body: AdminActionRequest = AdminActionRequest()): Response<Unit>

    @POST("api/v1/admin/requests/{requestId}/reject")
    suspend fun rejectRequest(@Path("requestId") id: String, @Body body: AdminActionRequest = AdminActionRequest()): Response<Unit>
}
