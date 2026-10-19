package com.logflare.android.fcm

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class NotificationIntentTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun eachNotificationKeepsItsOwnErrorId() {
        val a = notificationContentIntent(context, 41, 1, 41)
        val b = notificationContentIntent(context, 42, 1, 42)

        assertNotEquals(a, b)
        assertEquals(41, shadowOf(a).savedIntent.getIntExtra("errorid", 0))
        assertEquals(42, shadowOf(b).savedIntent.getIntExtra("errorid", 0))
    }

    @Test
    fun reusesTheRunningTask_withoutClearingIt() {
        val flags = shadowOf(notificationContentIntent(context, 41, 1, 41)).savedIntent.flags
        assertTrue(flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        assertTrue(flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertEquals(0, flags and Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
