package com.example.logflare.core.designsystem.v2

import com.example.logflare.core.designsystem.v2.components.LfToastState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LfToastStateTest {
    @Test
    fun show_thenHidesAfter1800ms() = runTest(StandardTestDispatcher()) {
        val state = LfToastState(backgroundScope)
        state.show("Copied")
        advanceTimeBy(1)
        assertEquals("Copied", state.message.value)
        advanceTimeBy(1700)
        assertEquals("Copied", state.message.value)
        advanceTimeBy(200)
        assertNull(state.message.value)
    }

    @Test
    fun secondShow_restartsTimer() = runTest(StandardTestDispatcher()) {
        val state = LfToastState(backgroundScope)
        state.show("one")
        advanceTimeBy(1000)
        state.show("two")
        advanceTimeBy(1500) // t = 2500
        assertEquals("two", state.message.value)
        advanceTimeBy(301) // t = 2801
        assertNull(state.message.value)
    }
}
