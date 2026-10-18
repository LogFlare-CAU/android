package com.logflare.android.di

import com.example.logflare.core.network.auth.AuthTokenProvider
import com.logflare.android.data.session.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthTokenModule {
    @Binds
    abstract fun bindAuthTokenProvider(impl: SessionRepository): AuthTokenProvider
}
