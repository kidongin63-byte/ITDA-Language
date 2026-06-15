package com.itda.language.data.repository

import com.itda.language.data.api.AuthApi
import com.itda.language.data.api.TokenManager
import com.itda.language.data.model.LoginRequest
import com.itda.language.data.model.RegisterRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
) {
    suspend fun register(
        email: String, nickname: String, password: String,
        phone: String = "", gender: String = "", birthId: String = "",
    ): Result<Unit> {
        return try {
            val resp = authApi.register(RegisterRequest(email, nickname, password, phone, gender, birthId))
            if (resp.isSuccessful && resp.body() != null) {
                val tokens = resp.body()!!
                tokenManager.saveTokens(tokens.access_token, tokens.refresh_token)
                Result.success(Unit)
            } else {
                Result.failure(Exception("회원가입 실패: ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val resp = authApi.login(LoginRequest(email, password))
            if (resp.isSuccessful && resp.body() != null) {
                val tokens = resp.body()!!
                tokenManager.saveTokens(tokens.access_token, tokens.refresh_token)
                Result.success(Unit)
            } else {
                Result.failure(Exception("로그인 실패: 이메일 또는 비밀번호를 확인해주세요"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        tokenManager.clearTokens()
    }

    suspend fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()
}
