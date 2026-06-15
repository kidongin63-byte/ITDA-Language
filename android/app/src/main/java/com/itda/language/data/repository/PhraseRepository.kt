package com.itda.language.data.repository

import com.itda.language.data.api.PhraseApi
import com.itda.language.data.model.PhraseCreateRequest
import com.itda.language.data.model.PhraseResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhraseRepository @Inject constructor(
    private val phraseApi: PhraseApi,
) {
    suspend fun getPresets(): Result<Map<String, List<String>>> {
        return try {
            val resp = phraseApi.getPresets()
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception("프리셋 로드 실패"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addAllPresets(): Result<List<PhraseResponse>> {
        return try {
            val resp = phraseApi.addAllPresets()
            if (resp.isSuccessful) Result.success(resp.body() ?: emptyList())
            else Result.failure(Exception("프리셋 추가 실패"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listPhrases(category: String? = null): Result<List<PhraseResponse>> {
        return try {
            val resp = phraseApi.listPhrases(category)
            if (resp.isSuccessful) Result.success(resp.body() ?: emptyList())
            else Result.failure(Exception("문구 로드 실패"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPhrase(text: String, category: String = "custom"): Result<PhraseResponse> {
        return try {
            val resp = phraseApi.createPhrase(PhraseCreateRequest(text, category))
            if (resp.isSuccessful && resp.body() != null) Result.success(resp.body()!!)
            else Result.failure(Exception("문구 추가 실패"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun incrementUsage(phraseId: String): Result<Unit> {
        return try {
            phraseApi.incrementUsage(phraseId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePhrase(phraseId: String): Result<Unit> {
        return try {
            phraseApi.deletePhrase(phraseId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
