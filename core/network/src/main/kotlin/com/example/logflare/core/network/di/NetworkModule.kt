package com.example.logflare.core.network.di

import com.example.logflare.core.network.LogflareApi
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
    fun provideOkHttp(baseUrlProvider: BaseUrlProvider): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .addInterceptor(HostSelectionInterceptor(baseUrlProvider))
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit =
        Retrofit.Builder()
            // Retrofit requires a non-empty baseUrl; overridden via HostSelectionInterceptor when user selects server.
            // Use emulator host mapping (10.0.2.2) as safer default than localhost for dev.
            .baseUrl("http://10.0.2.2/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideLogflareApi(retrofit: Retrofit): LogflareApi =
        retrofit.create(LogflareApi::class.java)
}
