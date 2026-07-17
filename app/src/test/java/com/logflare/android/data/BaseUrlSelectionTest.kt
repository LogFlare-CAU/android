package com.logflare.android.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.logflare.core.network.host.BaseUrlProvider
import com.example.logflare.core.network.host.MutableBaseUrlProvider
import com.logflare.android.di.AppNetworkBindings
import dagger.Binds
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
    fun baseUrlProviderBindingsResolveToSameImplementation() {
        val bindsMethods = AppNetworkBindings::class.java.declaredMethods.filter {
            it.isAnnotationPresent(Binds::class.java)
        }
        val boundTypes = bindsMethods.map { it.returnType.name }.toSet()
        assertTrue(
            "BaseUrlProvider and MutableBaseUrlProvider must both be bound",
            BaseUrlProvider::class.java.name in boundTypes &&
                MutableBaseUrlProvider::class.java.name in boundTypes,
        )
        val implTypes = bindsMethods.map { it.parameterTypes.single().name }.toSet()
        assertEquals(
            "Both bindings must target the same implementation class",
            setOf(DataStoreBaseUrlProvider::class.java.name),
            implTypes,
        )
    }

    @Test
    fun mutableProviderIsSameInstanceAsBaseUrlProvider() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ServerConfigRepository(context)
        val provider = DataStoreBaseUrlProvider(repository)

        assertSame(provider, provider as BaseUrlProvider)
        assertSame(provider, provider as MutableBaseUrlProvider)
    }
}
