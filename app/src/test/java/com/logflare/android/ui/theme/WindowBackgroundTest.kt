package com.logflare.android.ui.theme

import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WindowBackgroundTest {
    @Test
    fun setsTheWindowBackgroundToTheAppColour_andFollowsChanges() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

        activity.window.setAppBackground(0xFF0A0B0E.toInt())
        assertEquals(0xFF0A0B0E.toInt(), (activity.window.decorView.background as ColorDrawable).color)

        activity.window.setAppBackground(0xFFF2F4F6.toInt())
        assertEquals(0xFFF2F4F6.toInt(), (activity.window.decorView.background as ColorDrawable).color)
    }
}
