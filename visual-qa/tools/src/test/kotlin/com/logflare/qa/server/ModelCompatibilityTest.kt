package com.logflare.qa.server

import com.example.logflare.core.model.FcmConfigResponse
import com.example.logflare.core.model.FcmTokenResponse
import com.example.logflare.core.model.LogEventDetailResponse
import com.example.logflare.core.model.LogEventPageResponse
import com.example.logflare.core.model.LogSummaryResponse
import com.example.logflare.core.model.ProjectPermsSequenceResponse
import com.example.logflare.core.model.ProjectResponse
import com.example.logflare.core.model.ProjectResponseWithToken
import com.example.logflare.core.model.ProjectSequenceResponse
import com.example.logflare.core.model.RemovedResponse
import com.example.logflare.core.model.StringResponse
import com.example.logflare.core.model.UserResponse
import com.example.logflare.core.model.UserSequenceResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

class ModelCompatibilityTest {
    private lateinit var server: MockServer
    private val client = HttpClient.newHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        server = MockServer(host = "127.0.0.1", port = 0)
        server.start()
    }

    @After
    fun tearDown() {
        server.stop()
    }

    private fun request(method: String, path: String, body: String? = null, bearer: Boolean = true): String {
        val builder = HttpRequest.newBuilder(
            URI.create("http://127.0.0.1:${server.boundPort}$path"),
        ).header("Accept", "application/json")
        if (bearer) builder.header("Authorization", "Bearer qa-token-fixed")
        val publisher = body?.let {
            builder.header("Content-Type", "application/json; charset=utf-8")
            HttpRequest.BodyPublishers.ofString(it, StandardCharsets.UTF_8)
        } ?: HttpRequest.BodyPublishers.noBody()
        val response = client.send(
            builder.method(method, publisher).build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8),
        )
        assertTrue("unexpected HTTP ${response.statusCode()}: ${response.body()}", response.statusCode() in 200..299)
        return response.body()
    }

    @Test
    fun everyLogflareApiResponseDecodesWithRealModelSerializersAfterMutations() {
        val auth = json.decodeFromString<StringResponse>(
            request(
                "POST",
                "/user/auth",
                """{"username":"qa-admin","password":"qa-password","keep_logged_in":true}""",
                bearer = false,
            ),
        )
        assertEquals("qa-token-fixed", auth.data)

        val initialUsers = json.decodeFromString<UserSequenceResponse>(request("GET", "/user/"))
        val seededUsers = listOf("qa-admin", "jihyun", "devops-bot", "minsu", "yuna")
        assertEquals(seededUsers, initialUsers.data!!.map { it.username })

        val createdUser = json.decodeFromString<UserResponse>(
            request("POST", "/user/", """{"username":"compat-user","permission":2,"password":"pw"}"""),
        )
        assertEquals(6, createdUser.data!!.idx)

        val updatedUser = json.decodeFromString<UserResponse>(
            request("PATCH", "/user/6", """{"username":"compat-renamed","permission":1}"""),
        )
        assertEquals("compat-renamed", updatedUser.data!!.username)
        json.decodeFromString<UserResponse>(request("GET", "/user/me"))
        json.decodeFromString<UserResponse>(request("DELETE", "/user/6"))
        val usersAfterMutation = json.decodeFromString<UserSequenceResponse>(request("GET", "/user/"))
        assertEquals(seededUsers, usersAfterMutation.data!!.map { it.username })

        val initialProjects = json.decodeFromString<ProjectSequenceResponse>(request("GET", "/project/"))
        assertEquals(listOf(101, 102, 103, 104), initialProjects.data!!.map { it.id })

        val createdProject = json.decodeFromString<ProjectResponseWithToken>(
            request("POST", "/project/", """{"name":"Compatibility"}"""),
        )
        assertEquals(105, createdProject.data!!.id)
        assertNotNull(createdProject.data!!.token)
        json.decodeFromString<ProjectResponseWithToken>(request("GET", "/project/105/token"))
        json.decodeFromString<ProjectResponseWithToken>(request("POST", "/project/105/rotate-token"))

        val updatedProject = json.decodeFromString<ProjectResponse>(
            request("PATCH", "/project/105", """{"name":"Compatibility-Renamed"}"""),
        )
        assertEquals("Compatibility-Renamed", updatedProject.data!!.name)

        val perms = json.decodeFromString<ProjectPermsSequenceResponse>(
            request(
                "POST",
                "/project/perm/batch/reset",
                """{"projectid":105,"usernames":["jihyun","minsu"]}""",
            ),
        )
        assertEquals(2, perms.data!!.size)
        json.decodeFromString<ProjectPermsSequenceResponse>(request("GET", "/project/105/perm"))

        val summary = json.decodeFromString<LogSummaryResponse>(request("GET", "/log/summary?hours=24"))
        assertEquals(5, summary.data!!.projects.size)

        val events = json.decodeFromString<LogEventPageResponse>(
            request("GET", "/log/events?min_level=ERROR&sort=newest&grouped=true&limit=50"),
        )
        val first = events.data!!.items.first()
        val detail = json.decodeFromString<LogEventDetailResponse>(request("GET", "/log/events/${first.id}"))
        assertEquals(first.message, detail.data!!.message)

        val fcm = json.decodeFromString<FcmConfigResponse>(request("GET", "/fcm/data"))
        assertFalse(fcm.success)
        assertEquals(null, fcm.data)

        val token = json.decodeFromString<FcmTokenResponse>(
            request("POST", "/fcm/token", """{"fcm_token":"compat-token"}"""),
        )
        assertEquals(1, token.data!!.userIdx)
        val removed = json.decodeFromString<RemovedResponse>(
            request("DELETE", "/fcm/token", """{"fcm_token":"compat-token"}"""),
        )
        assertTrue(removed.data!!.removed)

        val deletedProject = json.decodeFromString<StringResponse>(request("DELETE", "/project/105"))
        assertTrue(deletedProject.success)
        val projectsAfterMutation = json.decodeFromString<ProjectSequenceResponse>(request("GET", "/project/"))
        assertEquals(listOf(101, 102, 103, 104), projectsAfterMutation.data!!.map { it.id })
    }
}
