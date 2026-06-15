package com.itda.language.data.repository

import com.itda.language.data.api.VoiceApi
import com.itda.language.data.model.VoiceCategoryList
import com.itda.language.data.model.VoicePersona
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceRepository @Inject constructor(
    private val voiceApi: VoiceApi,
) {
    suspend fun getCategories(): Result<VoiceCategoryList> {
        return try {
            val resp = voiceApi.getCategories()
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception("카테고리 로드 실패"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPersonas(
        gender: String? = null,
        ageGroup: String? = null,
        region: String? = null,
        tone: String? = null,
    ): Result<List<VoicePersona>> {
        return try {
            val resp = voiceApi.getPersonas(gender, ageGroup, region, tone)
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception("화자 목록 로드 실패"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
