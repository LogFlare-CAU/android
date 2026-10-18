package com.logflare.android.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationsTest {

    @Test
    fun builders_matchTheirPatterns() {
        assertMatches(Destinations.ProjectDetailPattern, Destinations.projectDetail(7))
        assertMatches(Destinations.ProjectSettingsPattern, Destinations.projectSettings(7))
        assertMatches(Destinations.LogDetailPattern, Destinations.logDetail(42))
        assertMatches(Destinations.EditMemberPattern, Destinations.editMember(3))
    }

    @Test
    fun builders_produceTheSpecifiedRoutes() {
        assertEquals("projects/7", Destinations.projectDetail(7))
        assertEquals("projects/7/settings", Destinations.projectSettings(7))
        assertEquals("logs/42", Destinations.logDetail(42))
        assertEquals("my/members/3", Destinations.editMember(3))
    }

    @Test
    fun fixedRoutes_haveNoUnboundArguments() {
        listOf(
            Destinations.Login,
            Destinations.Home,
            Destinations.Logs,
            Destinations.Projects,
            Destinations.My,
            Destinations.ProjectCreate,
            Destinations.AddMember,
        ).forEach { assertFalse(it, it.contains("{")) }
    }

    @Test
    fun tabs_areTheFourTopLevelRoutes() {
        assertEquals(
            listOf(Destinations.Home, Destinations.Logs, Destinations.Projects, Destinations.My),
            Destinations.Tabs,
        )
    }

    private fun assertMatches(pattern: String, route: String) {
        val regex = Regex(pattern.replace(Regex("\\{[^}]+\\}"), "[^/]+"))
        assertTrue("$route !~ $pattern", regex.matches(route))
    }
}
