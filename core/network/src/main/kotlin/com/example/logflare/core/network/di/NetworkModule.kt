package com.example.logflare.core.network.di

import com.example.logflare.core.network.BuildConfig
import com.example.logflare.core.network.HttpUnauthorizedAction
import com.example.logflare.core.network.LogflareApi
import com.example.logflare.core.network.UnauthorizedResponseInterceptor
import com.example.logflare.core.network.auth.AccessHeaderInterceptor
import com.example.logflare.core.network.auth.AccessServiceTokenProvider
import com.example.logflare.core.network.auth.AuthHeaderInterceptor
import com.example.logflare.core.network.auth.AuthTokenProvider
import com.example.logflare.core.network.host.BaseUrlProvider
import com.example.logflare.core.network.host.HostSelectionInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttp(
        baseUrlProvider: BaseUrlProvider,
        authTokenProvider: AuthTokenProvider,
        accessTokens: AccessServiceTokenProvider,
        httpUnauthorizedAction: HttpUnauthorizedAction,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    // Defense in case a later change logs the request after the Access headers are added.
                    redactHeader(AccessHeaderInterceptor.CLIENT_ID)
                    redactHeader(AccessHeaderInterceptor.CLIENT_SECRET)
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .addInterceptor(AuthHeaderInterceptor(authTokenProvider))
            .addInterceptor(HostSelectionInterceptor(baseUrlProvider))
            // After logging, so the service-token secret is not written to logcat.
            .addInterceptor(AccessHeaderInterceptor(accessTokens))
            .addInterceptor(UnauthorizedResponseInterceptor(httpUnauthorizedAction))
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit =
        Retrofit.Builder()
            // Retrofit requires a non-empty baseUrl; overridden via HostSelectionInterceptor when user selects server.
            // Include port 8000 to match docker-compose exposed backend.
            .baseUrl("http://10.0.2.2:8000/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideLogflareApi(retrofit: Retrofit): LogflareApi =
        retrofit.create(LogflareApi::class.java)
}
