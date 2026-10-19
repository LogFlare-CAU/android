package com.logflare.android.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.example.logflare.core.network.host.BaseUrlProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Plain prefs stand in for the encrypted file (the keystore is not available under Robolectric). */
@RunWith(RobolectricTestRunner::class)
class EncryptedAccessCredentialStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var active: String? = "https://logs.example.com/"
    private var opened = 0
    private var failOpen = false

    @Before fun wipe() {
        context.getSharedPreferences("test_flag", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("test_secret", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun store() = EncryptedAccessCredentialStore(
        baseUrlProvider = object : BaseUrlProvider { override fun getBaseUrl() = active },
        flagPrefs = { context.getSharedPreferences("test_flag", Context.MODE_PRIVATE) },
        openSecretPrefs = {
            opened++
            if (failOpen) throw IllegalStateException("keystore unavailable")
            context.getSharedPreferences("test_secret", Context.MODE_PRIVATE) as SharedPreferences
        },
    )

    @Test fun passwordUser_neverOpensTheEncryptedFile() {
        val store = store()

        repeat(5) { assertNull(store.current()) }
        store.clear()

        assertEquals(0, opened)
    }

    @Test fun savedToken_isReturnedOnlyForItsServer() {
        val store = store()
        store.save("https://logs.example.com", "id-1", "secret-1")

        assertEquals("id-1", store.current()?.clientId)
        assertEquals("secret-1", store.current()?.secret)
        active = "https://other.example.com/"
        assertNull(store.current())
        active = null
        assertNull(store.current())
    }

    @Test fun savedToken_survivesANewInstance_readFromDisk() {
        store().save("https://logs.example.com", "id-1", "secret-1")

        assertEquals("id-1", store().current()?.clientId)
    }

    @Test fun cached_afterFirstRead() {
        store().save("https://logs.example.com", "id-1", "secret-1")
        opened = 0
        val fresh = store()

        repeat(5) { fresh.current() }

        assertEquals("the secret file is read once, then served from memory", 1, opened)
    }

    @Test fun clear_dropsTheToken_andTheFlag() {
        val store = store()
        store.save("https://logs.example.com", "id-1", "secret-1")

        store.clear()

        assertNull(store.current())
        assertNull(store().current())
    }

    @Test fun keystoreFailure_neverThrows_andIsNotRetriedPerRequest() {
        store().save("https://logs.example.com", "id-1", "secret-1")
        failOpen = true
        opened = 0
        val broken = store()

        repeat(5) { assertNull(broken.current()) }

        assertTrue("one attempt, not one per request: $opened", opened == 1)
    }
}
