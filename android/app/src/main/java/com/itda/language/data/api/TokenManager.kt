package com.itda.language.data.api

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "auth_tokens")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            prefs[REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun clearTokens() {
        context.dataStore.edit { it.clear() }
    }

    fun getAccessTokenSync(): String? = runBlocking {
        context.dataStore.data.map { it[ACCESS_TOKEN] }.first()
    }

    suspend fun getRefreshToken(): String? =
        context.dataStore.data.map { it[REFRESH_TOKEN] }.first()

    suspend fun isLoggedIn(): Boolean =
        context.dataStore.data.map { it[ACCESS_TOKEN] != null }.first()
}
