package com.example.logflare_android.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class LogflareMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM token: $token")
        // TODO: send token to backend if needed
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.i(TAG, "FCM message from: ${message.from}")
        Log.i(TAG, "FCM data: ${message.data}")
        message.notification?.let {
            Log.i(TAG, "FCM notification: title=${it.title}, body=${it.body}")
        }
        // TODO: Show a notification channel-based notification if desired
    }

    companion object {
        private const val TAG = "LogflareFCM"
    }
}
