package com.logflare.android.fcm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.logflare.android.data.DeviceRepository
import com.logflare.android.data.session.SessionRepository
import com.logflare.android.data.session.SessionState
import com.logflare.android.di.ApplicationScope
import com.logflare.android.push.AlertSettings
import com.logflare.android.push.AlertSettingsRepository
import com.logflare.android.push.NotificationHistory
import com.logflare.android.push.PushDecision
import com.logflare.android.push.PushMessage
import com.logflare.android.push.PushRegistrar
import com.logflare.android.push.composePushBody
import com.logflare.android.push.decideIncoming
import com.logflare.android.push.from
import com.logflare.android.push.toHistoryEntry
import com.logflare.android.ui.format.logTitle
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import com.logflare.android.MainActivity
import com.logflare.android.R

@AndroidEntryPoint(FirebaseMessagingService::class)
class LogflareMessagingService : Hilt_LogflareMessagingService() {

    private val channelId = "logflare_channel"

    @Inject
    lateinit var deviceRepository: DeviceRepository

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var alertSettingsRepository: AlertSettingsRepository

    @Inject
    lateinit var notificationHistory: NotificationHistory

    @Inject
    lateinit var pushRegistrar: PushRegistrar

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM token refreshed")
        appScope.launch { pushRegistrar.onNewToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        deviceRepository.ensureFirebaseInitializedFromCacheAsync()
        val data = message.data
        val push = PushMessage.from(data)
        val session = sessionRepository.state.value
        val settings = alertSettingsRepository.settingsFor(session)
        if (settings != null) {
            handle(push, data, session, settings)
        } else {
            // Cold start: the session and its alert settings are still being read from disk.
            appScope.launch {
                val (state, ready) = withTimeoutOrNull(READY_TIMEOUT_MS) { alertSettingsRepository.awaitReady() }
                    ?: sessionRepository.state.value.let { it to alertSettingsRepository.settingsFor(it) }
                handle(push, data, state, ready)
            }
        }
    }

    /** [settings] null: the session's own settings aren't loaded; see [decideIncoming]. */
    private fun handle(push: PushMessage, data: Map<String, String>, session: SessionState, settings: AlertSettings?) {
        val decision = decideIncoming(push, session, settings, LocalTime.now())
        if (decision == PushDecision.Drop || session !is SessionState.SignedIn) {
            Log.i(TAG, "Push dropped: projectId=${push.projectId}, level=${push.level}, isTest=${push.isTest}")
            return
        }
        val entry = push.toHistoryEntry(receivedAt = System.currentTimeMillis())
        appScope.launch { notificationHistory.add(entry, session.key) }
        if (decision == PushDecision.Notify) showNotification(data)
    }

    private fun showNotification(data: Map<String, String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Notification permission not granted; push recorded only")
            return
        }
        val errorid = data["errorid"]?.toIntOrNull() ?: 0
        val type = data["type"] ?: getString(R.string.push_unknown_type)
        val level = data["level"]
        val timestamp = data["timestamp"]
        val messageText = data["message"]
        val projectid = data["projectid"]?.toIntOrNull()
        // A Context string: on Android 13+ it follows the app language chosen in My.
        val contentMessage = composePushBody(
            head = level?.let { getString(R.string.push_body_head, it, type) } ?: type,
            message = messageText?.let(::logTitle),
            at = timestamp?.let { getString(R.string.push_body_at, it) },
        )
        val title = getString(R.string.push_title, type)
        val notificationId = if (errorid > 0) errorid else System.currentTimeMillis().toInt()
        val pendingIntent = notificationContentIntent(this, notificationId, projectid, errorid)
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_action_error)
            .setContentTitle(title)
            .setContentText(contentMessage)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val manager = NotificationManagerCompat.from(this)
        manager.notify(notificationId, notification)
    }

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            channelId,
            getString(R.string.push_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }


    companion object {
        private const val TAG = "LogflareFCM"

        /** How long a cold-start push waits for the session and its alert settings to load. */
        private const val READY_TIMEOUT_MS = 5_000L
    }
}

/**
 * Tap target of one notification. The request code is unique per notification so each keeps its own
 * extras (a shared code + UPDATE_CURRENT made every tap open the latest push), and the flags reuse the
 * running task so a warm tap reaches MainActivity.onNewIntent instead of restarting the app.
 */
internal fun notificationContentIntent(context: Context, requestCode: Int, projectId: Int?, errorId: Int): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("projectid", projectId)
        putExtra("errorid", errorId)
    }
    return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

