package com.example.logflare_android.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LogflareMessagingService : FirebaseMessagingService() {

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

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.i(TAG, "FCM message from: ${message.from}")
        Log.i(TAG, "FCM data: ${message.data}")
        message.notification?.let {
            Log.i(TAG, "FCM notification: title=${it.title}, body=${it.body}")
        }
    }

    companion object {
        private const val TAG = "LogflareFCM"
    }
}
