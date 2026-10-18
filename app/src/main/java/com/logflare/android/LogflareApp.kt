package com.logflare.android

import android.app.Application
import com.logflare.android.push.PushRegistrar
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp(Application::class)
class LogflareApp : Hilt_LogflareApp() {

    @Inject
    lateinit var pushRegistrar: PushRegistrar

    override fun onCreate() {
        super.onCreate()
        pushRegistrar.start()
    }
}
