package com.logflare.android.di

import android.content.Context
import com.logflare.android.data.store.CacheStore
import com.logflare.android.data.store.FileCacheStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object StoreModule {
    @Provides
    @Singleton
    fun provideCacheStore(@ApplicationContext context: Context, json: Json): CacheStore =
        FileCacheStore(File(context.filesDir, "cache-v2"), json, Dispatchers.IO)
}
