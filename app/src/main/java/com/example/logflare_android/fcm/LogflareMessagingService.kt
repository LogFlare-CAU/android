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

@AndroidEntryPoint(FirebaseMessagingService::class)
class LogflareMessagingService : Hilt_LogflareMessagingService() {

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
        val errorid = message.data["errorid"]?.toIntOrNull() ?: 0
        val type = message.data["type"] ?: "Unknown"
        val level = message.data["level"]
        val timestamp = message.data["timestamp"]
        val messageText = message.data["message"]
        val projectid = message.data["projectid"]?.toIntOrNull()
        val isTest = message.data["test"]?.toBoolean() ?: false
        if (!isTest && filterLogs(projectid ?: 0, level ?: "INFO", messageText ?: "")) {
            Log.i(TAG, "Log filtered out: projectId=$projectid, level=$level, message=$messageText, isTest=$isTest")
            return
        }
        val contentMessage = "$level: $type\n$messageText\n at $timestamp"
        val title = "Error: $type"
        val intent = android.content.Intent(this, com.example.logflare_android.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("projectid", projectid)
            putExtra("errorid", errorid)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_action_error)
            .setContentTitle(title)
            .setContentText(contentMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
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
        val globalAlertLevel = deviceRepository.getAlertLevel()
        if (globalAlertLevel != null) {
            if (LogLevel.fromLabel(level).code < LogLevel.fromLabel(globalAlertLevel).code) {
                return@runBlocking true // 전역 레벨 미달로 필터링
            }
        }
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
