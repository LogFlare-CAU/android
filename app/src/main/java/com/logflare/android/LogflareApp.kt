package com.logflare.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp(Application::class)
class LogflareApp : Hilt_LogflareApp()
