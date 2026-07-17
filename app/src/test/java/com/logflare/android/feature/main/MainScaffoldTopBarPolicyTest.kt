package com.logflare.android.feature.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainScaffoldTopBarPolicyTest {
    @Test
    fun accountNestedDestinationsHideScaffoldTopBar() {
        assertTrue(shouldHideScaffoldTopBar("mypage/add-member"))
        assertTrue(shouldHideScaffoldTopBar("mypage/edit-member/qa-member"))
        assertTrue(shouldHideScaffoldTopBar("mypage/logout"))
    }

    @Test
    fun otherDestinationsKeepScaffoldTopBar() {
        assertFalse(shouldHideScaffoldTopBar("home"))
        assertFalse(shouldHideScaffoldTopBar("logs"))
        assertFalse(shouldHideScaffoldTopBar("projects"))
        assertFalse(shouldHideScaffoldTopBar("mypage"))
        assertFalse(shouldHideScaffoldTopBar("projects/create"))
        assertFalse(shouldHideScaffoldTopBar("project/101"))
        assertFalse(shouldHideScaffoldTopBar("project/101/settings"))
        assertFalse(shouldHideScaffoldTopBar("log/detail"))
        assertFalse(shouldHideScaffoldTopBar(null))
    }

    @Test
    fun accountOwnedTopBarDropsExtraContentTopPadding() {
        assertEquals(0, mainNavHostExtraTopPaddingDp("mypage/add-member"))
        assertEquals(0, mainNavHostExtraTopPaddingDp("mypage/edit-member/qa-member"))
        assertEquals(0, mainNavHostExtraTopPaddingDp("mypage/logout"))
    }

    @Test
    fun otherDestinationsKeepSixteenDpExtraTopPadding() {
        assertEquals(16, mainNavHostExtraTopPaddingDp("home"))
        assertEquals(16, mainNavHostExtraTopPaddingDp("mypage"))
        assertEquals(16, mainNavHostExtraTopPaddingDp("project/101"))
        assertEquals(16, mainNavHostExtraTopPaddingDp(null))
    }
}
