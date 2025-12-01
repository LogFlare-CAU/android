package com.example.logflare_android.fcm

import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LogflareMessagingService : FirebaseMessagingService() {

    private val channelId = "logflare_channel"
    @Inject
    lateinit var deviceRepository: com.example.logflare_android.data.DeviceRepository

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
        val title = message.notification?.title ?: message.data["title"] ?: "알림"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        Log.i(TAG, "Received FCM message: title=$title, body=$body")
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_dialog_info) //TODO: 이거 실제 아이콘으로 교체
            .setContentTitle(title)
            .setContentText(body)
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


    companion object {
        private const val TAG = "LogflareFCM"
    }
}
