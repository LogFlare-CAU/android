package com.logflare.android.di

import com.example.logflare.core.network.HttpUnauthorizedAction
import com.logflare.android.data.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

@Module
@InstallIn(SingletonComponent::class)
object AppHttpUnauthorizedModule {
    @Provides
    @Singleton
    fun provideHttpUnauthorizedAction(auth: AuthRepository): HttpUnauthorizedAction =
        HttpUnauthorizedAction {
            runBlocking(Dispatchers.IO) {
                auth.clearToken()
            }
        }
}
