package com.logflare.android.di

import com.example.logflare.core.network.host.BaseUrlProvider
import com.example.logflare.core.network.host.MutableBaseUrlProvider
import com.logflare.android.data.DataStoreBaseUrlProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the singleton [DataStoreBaseUrlProvider] as [MutableBaseUrlProvider],
 * then aliases [BaseUrlProvider] to that same instance so OkHttp and Auth share one object.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppNetworkBindings {
    @Binds
    @Singleton
    abstract fun bindMutableBaseUrlProvider(impl: DataStoreBaseUrlProvider): MutableBaseUrlProvider

    companion object {
        @Provides
        @Singleton
        fun provideBaseUrlProvider(mutable: MutableBaseUrlProvider): BaseUrlProvider = mutable
    }
}
