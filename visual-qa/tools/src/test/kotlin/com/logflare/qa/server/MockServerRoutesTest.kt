package com.logflare.qa.server

import com.example.logflare.core.model.FcmTokenResponse
import com.example.logflare.core.model.LogEventDetailResponse
import com.example.logflare.core.model.LogEventPageResponse
import com.example.logflare.core.model.LogSummaryResponse
import com.example.logflare.core.model.ProjectResponseWithToken
import com.example.logflare.core.model.RemovedResponse
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * The rework routes, decoded with the app's own response models (what Plan 2 Task 7 decodes),
 * against the prototype-shaped fixture. Timestamps are ages before the server clock, so the
 * app's relative labels ("1 min ago") and the 24h summary window stay stable from run to run.
 */
class MockServerRoutesTest {
    private val now = Instant.parse("2026-10-19T12:00:00Z")
    private lateinit var server: MockServer
    private val client = HttpClient.newHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        server = MockServer(host = "127.0.0.1", port = 0, clock = Clock.fixed(now, ZoneOffset.UTC))
        server.start()
    }

    @After
    fun tearDown() {
        server.stop()
    }

    private fun call(method: String, path: String, body: String? = null, bearer: Boolean = true): HttpResponse<String> {
        val builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:${server.boundPort}$path"))
            .header("Accept", "application/json")
        if (bearer) builder.header("Authorization", "Bearer qa-token-fixed")
        val publisher = body?.let {
            builder.header("Content-Type", "application/json; charset=utf-8")
            HttpRequest.BodyPublishers.ofString(it, StandardCharsets.UTF_8)
        } ?: HttpRequest.BodyPublishers.noBody()
        return client.send(builder.method(method, publisher).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
    }

    private fun ok(method: String, path: String, body: String? = null): String {
        val response = call(method, path, body)
        assertEquals("HTTP ${response.statusCode()}: ${response.body()}", 200, response.statusCode())
        return response.body()
    }

    private fun ago(minutes: Long): String =
        SERVER_TIME.format(now.minusSeconds(minutes * 60).atOffset(ZoneOffset.UTC))

    private companion object {
        /** The backend's naive-UTC form, e.g. `2026-10-19T11:59:00`. */
        val SERVER_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    }

    @Test
    fun summaryCountsTheLast24HoursPerLevelAndProject() {
        val res = json.decodeFromString(LogSummaryResponse.serializer(), ok("GET", "/log/summary"))
        assertTrue(res.success)
        val data = res.data!!
        assertEquals(24, data.windowHours)
        assertEquals(ago(24 * 60), data.since)
        assertEquals(
            mapOf("DEBUG" to 6, "INFO" to 2, "WARNING" to 2, "ERROR" to 11, "CRITICAL" to 1),
            data.counts,
        )
        assertEquals(listOf(101, 102, 103, 104), data.projects.map { it.projectId })
        val api = data.projects[0]
        assertEquals(4, api.errorCount)
        assertEquals(2, api.warningCount)
        assertEquals(ago(1), api.lastErrorAt)
        val batch = data.projects[3]
        assertEquals(0, batch.errorCount)
        assertEquals(0, batch.warningCount)
        assertNull(batch.lastErrorAt)

        val one = json.decodeFromString(LogSummaryResponse.serializer(), ok("GET", "/log/summary?project_id=102&hours=24"))
        assertEquals(listOf(102), one.data!!.projects.map { it.projectId })
        assertEquals(1, one.data!!.counts["CRITICAL"])
        assertEquals(0, one.data!!.counts["WARNING"])
    }

    @Test
    fun eventsAreGroupedNewestFirstWithOccurrences() {
        val page = json.decodeFromString(LogEventPageResponse.serializer(), ok("GET", "/log/events")).data!!
        assertEquals(11, page.items.size)
        assertNull(page.nextCursor)
        val first = page.items.first()
        assertEquals("NullPointerException in PaymentService", first.message)
        assertEquals(101, first.projectId)
        assertEquals("payment.log", first.logfileName)
        assertEquals("ERROR", first.level)
        assertEquals(3, first.occurrences)
        assertEquals(ago(1), first.lastSeen)
        assertEquals(first.lastSeen, first.timestamp)
        assertEquals(ago(128), first.firstSeen)
        assertEquals("Retrying job #8812 (attempt 2/5)", page.items.last().message)
    }

    @Test
    fun eventsFilterByLevelProjectFileAndQuery() {
        fun page(query: String) =
            json.decodeFromString(LogEventPageResponse.serializer(), ok("GET", "/log/events?$query")).data!!

        val errorsUp = page("min_level=ERROR")
        assertEquals(6, errorsUp.items.size)
        assertTrue(errorsUp.items.all { it.level == "ERROR" || it.level == "CRITICAL" })

        assertTrue(page("project_id=102").items.all { it.projectId == 102 })
        assertEquals(listOf("payment.log"), page("logfile_id=1002").items.map { it.logfileName })
        assertEquals(
            listOf("TimeoutError: upstream auth-service"),
            page("q=auth-SERVICE").items.map { it.message },
        )
        assertEquals("Retrying job #8812 (attempt 2/5)", page("sort=oldest").items.first().message)
        assertEquals("CRITICAL", page("sort=highest").items.first().level)
        assertEquals(23, page("grouped=false&limit=200").items.size)
    }

    @Test
    fun eventsPageWithACursor() {
        val first = json.decodeFromString(LogEventPageResponse.serializer(), ok("GET", "/log/events?limit=4")).data!!
        assertEquals(4, first.items.size)
        val cursor = first.nextCursor
        assertNotNull(cursor)
        val rest = json.decodeFromString(
            LogEventPageResponse.serializer(),
            ok("GET", "/log/events?limit=50&cursor=$cursor"),
        ).data!!
        assertEquals(7, rest.items.size)
        assertNull(rest.nextCursor)
        assertTrue((first.items.map { it.id } intersect rest.items.map { it.id }.toSet()).isEmpty())
        assertEquals(400, call("GET", "/log/events?cursor=bogus").statusCode())
    }

    @Test
    fun eventDetailDescribesTheRowAndItsGroup() {
        val latest = json.decodeFromString(LogEventPageResponse.serializer(), ok("GET", "/log/events")).data!!.items.first()
        val detail = json.decodeFromString(LogEventDetailResponse.serializer(), ok("GET", "/log/events/${latest.id}")).data!!
        assertEquals(latest.id, detail.id)
        assertEquals(3, detail.occurrences)
        assertEquals("ERROR", detail.rowLevel)
        assertEquals(ago(1), detail.occurredAt)
        assertTrue(detail.raw!!.startsWith("java.lang.NullPointerException"))
        assertEquals(2, detail.recentIds.size)
        assertFalse(latest.id in detail.recentIds)

        val older = json.decodeFromString(
            LogEventDetailResponse.serializer(),
            ok("GET", "/log/events/${detail.recentIds.last()}"),
        ).data!!
        assertEquals(ago(128), older.occurredAt)
        assertEquals(3, older.occurrences)
        assertNotEquals(latest.id, older.id)

        assertEquals(404, call("GET", "/log/events/999").statusCode())
        assertEquals(401, call("GET", "/log/events/${latest.id}", bearer = false).statusCode())
    }

    @Test
    fun projectTokenIsReadableAndRotates() {
        val token = json.decodeFromString(ProjectResponseWithToken.serializer(), ok("GET", "/project/101/token")).data!!
        assertEquals("api-server", token.name)
        assertEquals("qa-project-token-101", token.token)
        val rotated = json.decodeFromString(
            ProjectResponseWithToken.serializer(),
            ok("POST", "/project/101/rotate-token"),
        ).data!!
        assertNotEquals(token.token, rotated.token)
        val after = json.decodeFromString(ProjectResponseWithToken.serializer(), ok("GET", "/project/101/token")).data!!
        assertEquals(rotated.token, after.token)
        assertEquals(404, call("GET", "/project/999/token").statusCode())
    }

    @Test
    fun fcmTokenUnregistersOnlyWhatWasRegistered() {
        val body = """{"fcm_token":"device-1"}"""
        val unknown = json.decodeFromString(RemovedResponse.serializer(), ok("DELETE", "/fcm/token", body))
        assertFalse(unknown.data!!.removed)
        json.decodeFromString(FcmTokenResponse.serializer(), ok("POST", "/fcm/token", body))
        val removed = json.decodeFromString(RemovedResponse.serializer(), ok("DELETE", "/fcm/token", body))
        assertTrue(removed.data!!.removed)
    }
}
