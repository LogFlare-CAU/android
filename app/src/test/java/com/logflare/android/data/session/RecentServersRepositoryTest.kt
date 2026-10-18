package com.logflare.android.data.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RecentServersRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun recentServers_dedupesAndCapsAtFive() = runTest {
        val repo = RecentServersRepository(
            PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { File(tmp.root, "recent.preferences_pb") },
            ),
        )
        assertEquals(emptyList<String>(), repo.servers.first())

        listOf(
            "https://a.example.com",
            "https://b.example.com",
            "https://a.example.com/", // same as the first once normalized
            "https://c.example.com",
            "https://d.example.com",
            "https://e.example.com",
            "https://f.example.com",
            "  https://c.example.com  ",
            "",
        ).forEach { repo.remember(it) }

        assertEquals(
            listOf(
                "https://c.example.com/",
                "https://f.example.com/",
                "https://e.example.com/",
                "https://d.example.com/",
                "https://a.example.com/",
            ),
            repo.servers.first(),
        )
    }
}
