package com.logflare.android.di

import com.example.logflare.core.network.HttpUnauthorizedAction
import com.logflare.android.data.session.UnauthorizedSignOut
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppHttpUnauthorizedModule {
    /** A 401 for the current token signs out locally; see [UnauthorizedSignOut]. */
    @Binds
    abstract fun bindHttpUnauthorizedAction(impl: UnauthorizedSignOut): HttpUnauthorizedAction
}
