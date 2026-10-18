package com.logflare.android

import android.content.Intent
import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeepLinkIntentTest {

    @Test
    fun validErrorId_isRead() {
        assertEquals(42, Intent().putExtra(EXTRA_ERROR_ID, 42).deepLinkErrorId())
    }

    @Test
    fun missingZeroOrNegative_isNoLink() {
        assertNull(Intent().deepLinkErrorId())
        // The messaging service sends 0 when the push had no id.
        assertNull(Intent().putExtra(EXTRA_ERROR_ID, 0).deepLinkErrorId())
        assertNull(Intent().putExtra(EXTRA_ERROR_ID, -3).deepLinkErrorId())
    }

    @Test
    fun relaunchFromRecents_isNoLink() {
        val intent = Intent()
            .putExtra(EXTRA_ERROR_ID, 42)
            .addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)

        assertNull(intent.deepLinkErrorId())
    }

    @Test
    fun recreation_keepsAnUnconsumedLink_andIgnoresTheIntent() {
        val intent = Intent().putExtra(EXTRA_ERROR_ID, 42)
        val pending = Bundle().also { saveDeepLinkErrorId(it, 42) }
        val consumed = Bundle().also { saveDeepLinkErrorId(it, null) }

        assertEquals(42, initialDeepLinkErrorId(pending, Intent()))
        assertNull(initialDeepLinkErrorId(consumed, intent))
        assertEquals(42, initialDeepLinkErrorId(null, intent))
    }
}
