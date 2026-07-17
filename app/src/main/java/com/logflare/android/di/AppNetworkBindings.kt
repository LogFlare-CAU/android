package com.logflare.android.di

import com.example.logflare.core.network.host.BaseUrlProvider
import com.example.logflare.core.network.host.MutableBaseUrlProvider
import com.logflare.android.data.DataStoreBaseUrlProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppNetworkBindings {
    @Binds
    @Singleton
    abstract fun bindBaseUrlProvider(impl: DataStoreBaseUrlProvider): BaseUrlProvider

    @Binds
    @Singleton
    abstract fun bindMutableBaseUrlProvider(impl: DataStoreBaseUrlProvider): MutableBaseUrlProvider
}
