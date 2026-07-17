package com.logflare.android.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.logflare.core.network.host.BaseUrlProvider
import com.example.logflare.core.network.host.MutableBaseUrlProvider
import com.logflare.android.di.AppNetworkBindings
import dagger.Binds
import dagger.Provides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
class BaseUrlSelectionTest {

    @Test
    fun selectedUrlIsVisibleBeforeSetReturns() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ServerConfigRepository(context)
        val provider = DataStoreBaseUrlProvider(repository)

        provider.setBaseUrl("http://10.0.2.2:8000")
        assertEquals("http://10.0.2.2:8000/", provider.getBaseUrl())
    }

    @Test
    fun baseUrlProviderIsAliasOfMutableBinding() {
        val bindsMethods = AppNetworkBindings::class.java.declaredMethods.filter {
            it.isAnnotationPresent(Binds::class.java)
        }
        assertEquals(
            "Only MutableBaseUrlProvider should be @Binds to the singleton impl",
            1,
            bindsMethods.size,
        )
        val binds = bindsMethods.single()
        assertEquals(MutableBaseUrlProvider::class.java, binds.returnType)
        assertEquals(DataStoreBaseUrlProvider::class.java, binds.parameterTypes.single())

        val provideMethod = AppNetworkBindings.Companion::class.java.getDeclaredMethod(
            "provideBaseUrlProvider",
            MutableBaseUrlProvider::class.java,
        )
        assertTrue(provideMethod.isAnnotationPresent(Provides::class.java))
        assertEquals(BaseUrlProvider::class.java, provideMethod.returnType)

        val fake = object : MutableBaseUrlProvider {
            override fun getBaseUrl(): String? = "http://alias.test/"
            override suspend fun setBaseUrl(url: String) = Unit
        }
        assertSame(
            "Alias must return the exact MutableBaseUrlProvider instance OkHttp will use",
            fake,
            AppNetworkBindings.provideBaseUrlProvider(fake),
        )
    }
}
