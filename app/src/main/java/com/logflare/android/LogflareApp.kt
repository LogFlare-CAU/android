package com.logflare.android

import android.app.Application
import com.logflare.android.di.ApplicationScope
import com.logflare.android.push.PushRegistrar
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp(Application::class)
class LogflareApp : Hilt_LogflareApp() {

    /**
     * Lazy, and resolved on [appScope] (IO): building it builds OkHttp, Retrofit and the session
     * stores, which must not happen on the main thread during cold start.
     */
    @Inject
    lateinit var pushRegistrar: dagger.Lazy<PushRegistrar>

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        appScope.launch { pushRegistrar.get().start() }
    }
}
