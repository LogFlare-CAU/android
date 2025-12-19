package com.example.logflare_android.fcm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.logflare_android.data.DeviceRepository
import com.example.logflare_android.data.ProjectsRepository
import com.example.logflare_android.enums.LogLevel
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import com.example.logflare_android.R

@AndroidEntryPoint
class LogflareMessagingService : FirebaseMessagingService() {

    private val channelId = "logflare_channel"

    @Inject
    lateinit var deviceRepository: DeviceRepository

    @Inject
    lateinit var projectRepository: ProjectsRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM token: $token")
        // fire-and-forget register using DeviceRepository
        CoroutineScope(Dispatchers.IO).launch {
            try {
                deviceRepository.registerDevice(token)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register device token", e)
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        deviceRepository.ensureFirebaseInitializedFromCacheAsync()
//        val title = message.notification?.title ?: message.data["title"] ?: "알림"
//        val body = message.notification?.body ?: message.data["body"] ?: ""
        val errorid = message.data["errorid"]?.toIntOrNull() ?: 0
        val type = message.data["type"] ?: "Unknown"
        val level = message.data["level"]                    // 예: "ERROR"
        val timestamp = message.data["timestamp"]            // ISO8601 문자열
        val messageText = message.data["message"]
        val projectid = message.data["projectid"]?.toIntOrNull()
        val isTest = message.data["test"]?.toBoolean() ?: false
        if (!isTest && filterLogs(projectid ?: 0, level ?: "INFO", messageText ?: "")) {
            Log.i(TAG, "Log filtered out: projectId=$projectid, level=$level, message=$messageText, isTest=$isTest")
            return
        }
        val message = "$level: $type\n$messageText\n at $timestamp"
        val title = "Error: $type"
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_action_error) //TODO: 이거 실제 아이콘으로 교체
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = NotificationManagerCompat.from(this)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }


    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            channelId,
            "Logflare Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }


    fun filterLogs(projectId: Int, level: String, message: String): Boolean = runBlocking {
        val project = projectRepository.get(projectId) ?: return@runBlocking true
        val alertLevel = project.alertLevel
        val ignoreKeywords = project.excludeKeywords
        if (LogLevel.fromLabel(level).code < LogLevel.fromLabel(alertLevel).code) {
            return@runBlocking true
        }
        for (keyword in ignoreKeywords) {
            if (message.contains(keyword, ignoreCase = true)) {
                return@runBlocking true
            }
        }
        return@runBlocking false
    }


    companion object {
        private const val TAG = "LogflareFCM"
    }
}
