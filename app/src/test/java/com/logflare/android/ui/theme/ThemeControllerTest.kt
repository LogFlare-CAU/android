package com.logflare.android.ui.theme

import com.logflare.android.data.ThemePreference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeControllerTest {

    /** A DataStore stand-in: saving Dark is slow, Light fast, and each save emits the new value. */
    private val store = MutableStateFlow(ThemePreference.SYSTEM)
    private val saved = mutableListOf<ThemePreference>()
    private val save: suspend (ThemePreference) -> Unit = { p ->
        delay(if (p == ThemePreference.DARK) 100 else 10)
        saved += p
        store.value = p
    }

    @Test
    fun followsTheStore() = runTest {
        val controller = ThemeController(store, save, backgroundScope)
        store.value = ThemePreference.DARK
        runCurrent()
        assertEquals(ThemePreference.DARK, controller.preference.value)
    }

    @Test
    fun quickChoices_latestWinsAndIsSavedLast() = runTest {
        val controller = ThemeController(store, save, backgroundScope)
        runCurrent()
        controller.set(ThemePreference.DARK)
        controller.set(ThemePreference.LIGHT)
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(ThemePreference.LIGHT, controller.preference.value)
        assertEquals(ThemePreference.LIGHT, saved.last())
        assertEquals(ThemePreference.LIGHT, store.value)
    }

    @Test
    fun olderSaveLandingLate_doesNotSwitchBack() = runTest {
        val controller = ThemeController(store, save, backgroundScope)
        val seen = mutableListOf<ThemePreference>()
        backgroundScope.launch { controller.preference.collect { seen += it } }
        runCurrent()
        controller.set(ThemePreference.DARK)
        runCurrent() // the slow Dark save is under way
        controller.set(ThemePreference.LIGHT)
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(ThemePreference.LIGHT, controller.preference.value)
        assertEquals(listOf(ThemePreference.DARK, ThemePreference.LIGHT), saved)
        // Once Light was chosen the theme never went back to Dark.
        assertEquals(ThemePreference.LIGHT, seen.last())
        assertEquals(listOf(ThemePreference.LIGHT), seen.dropWhile { it != ThemePreference.LIGHT }.distinct())
    }

    @Test
    fun failedSave_keepsTheChoice() = runTest {
        val controller = ThemeController(store, { error("disk full") }, backgroundScope)
        runCurrent()
        controller.set(ThemePreference.DARK)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(ThemePreference.DARK, controller.preference.value)
    }
}
