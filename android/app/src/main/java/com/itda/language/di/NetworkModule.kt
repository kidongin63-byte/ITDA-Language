package com.itda.language.di

import com.itda.language.BuildConfig
import com.itda.language.data.api.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG)
                        HttpLoggingInterceptor.Level.BODY
                    else
                        HttpLoggingInterceptor.Level.NONE
                }
            )
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)  // TTS 합성은 시간이 걸릴 수 있음
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideTtsApi(retrofit: Retrofit): TtsApi =
        retrofit.create(TtsApi::class.java)

    @Provides
    @Singleton
    fun provideVoiceApi(retrofit: Retrofit): VoiceApi =
        retrofit.create(VoiceApi::class.java)

    @Provides
    @Singleton
    fun providePhraseApi(retrofit: Retrofit): PhraseApi =
        retrofit.create(PhraseApi::class.java)

    @Provides
    @Singleton
    fun provideAdminApi(retrofit: Retrofit): AdminApi =
        retrofit.create(AdminApi::class.java)
}
