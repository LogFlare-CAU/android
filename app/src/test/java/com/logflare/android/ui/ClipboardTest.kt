package com.logflare.android.ui

import android.app.Application
import android.content.ClipDescription
import android.content.ClipboardManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClipboardTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val clipboard get() = context.getSystemService(ClipboardManager::class.java)

    @Test
    fun sensitiveCopy_marksTheClip() {
        context.copyText("tok-123", sensitive = true)

        val clip = clipboard.primaryClip!!
        assertEquals("tok-123", clip.getItemAt(0).text.toString())
        assertTrue(clip.description.extras!!.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE))
    }

    @Test
    fun plainCopy_isNotMarked() {
        context.copyText("https://logs.example.com")

        assertFalse(clipboard.primaryClip!!.description.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE) == true)
    }
}
