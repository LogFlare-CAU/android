package com.logflare.android.di

import com.example.logflare.core.network.auth.AccessServiceTokenProvider
import com.logflare.android.data.session.AccessCredentialStore
import com.logflare.android.data.session.EncryptedAccessCredentialStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AccessCredentialModule {
    @Binds
    abstract fun bindStore(impl: EncryptedAccessCredentialStore): AccessCredentialStore

    @Binds
    abstract fun bindProvider(impl: EncryptedAccessCredentialStore): AccessServiceTokenProvider
}
