package com.logflare.qa.server

import com.example.logflare.core.model.ErrorSequenceResponse
import com.example.logflare.core.model.FcmConfigResponse
import com.example.logflare.core.model.FcmTokenResponse
import com.example.logflare.core.model.ProjectPermsSequenceResponse
import com.example.logflare.core.model.ProjectResponse
import com.example.logflare.core.model.ProjectResponseWithToken
import com.example.logflare.core.model.ProjectSequenceResponse
import com.example.logflare.core.model.StringResponse
import com.example.logflare.core.model.StringSequenceResponse
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
        assertEquals(listOf("qa-admin", "qa-member"), initialUsers.data!!.map { it.username })

        val createdUser = json.decodeFromString<UserResponse>(
            request("POST", "/user/", """{"username":"compat-user","permission":2,"password":"pw"}"""),
        )
        assertEquals(3, createdUser.data!!.idx)

        val updatedUser = json.decodeFromString<UserResponse>(
            request("PATCH", "/user/3", """{"username":"compat-renamed","permission":1}"""),
        )
        assertEquals("compat-renamed", updatedUser.data!!.username)
        json.decodeFromString<UserResponse>(request("GET", "/user/me"))
        json.decodeFromString<UserResponse>(request("GET", "/user/name?username=compat-renamed"))
        json.decodeFromString<UserResponse>(request("DELETE", "/user/3"))
        val usersAfterMutation = json.decodeFromString<UserSequenceResponse>(request("GET", "/user/"))
        assertEquals(listOf("qa-admin", "qa-member"), usersAfterMutation.data!!.map { it.username })

        val initialProjects = json.decodeFromString<ProjectSequenceResponse>(request("GET", "/project/"))
        assertEquals(listOf(101, 202), initialProjects.data!!.map { it.id })

        val createdProject = json.decodeFromString<ProjectResponseWithToken>(
            request("POST", "/project/", """{"name":"Compatibility"}"""),
        )
        assertEquals(303, createdProject.data!!.id)
        assertNotNull(createdProject.data!!.token)

        val updatedProject = json.decodeFromString<ProjectResponse>(
            request("PATCH", "/project/303", """{"name":"Compatibility-Renamed"}"""),
        )
        assertEquals("Compatibility-Renamed", updatedProject.data!!.name)

        val perms = json.decodeFromString<ProjectPermsSequenceResponse>(
            request(
                "POST",
                "/project/perm/batch/reset",
                """{"projectid":303,"usernames":["qa-admin","qa-member"]}""",
            ),
        )
        assertEquals(2, perms.data!!.size)
        json.decodeFromString<ProjectPermsSequenceResponse>(request("GET", "/project/303/perm"))

        val errors = json.decodeFromString<ErrorSequenceResponse>(request("GET", "/log/error?project_id=101"))
        assertTrue(errors.data!!.isNotEmpty())

        val lines = json.decodeFromString<StringSequenceResponse>(request("GET", "/log/101/1001"))
        assertTrue(lines.data!!.isNotEmpty())

        val fcm = json.decodeFromString<FcmConfigResponse>(request("GET", "/fcm/data"))
        assertFalse(fcm.success)
        assertEquals(null, fcm.data)

        val token = json.decodeFromString<FcmTokenResponse>(
            request("POST", "/fcm/token", """{"fcm_token":"compat-token"}"""),
        )
        assertEquals(1, token.data!!.userIdx)

        val deletedProject = json.decodeFromString<StringResponse>(request("DELETE", "/project/303"))
        assertTrue(deletedProject.success)
        val projectsAfterMutation = json.decodeFromString<ProjectSequenceResponse>(request("GET", "/project/"))
        assertEquals(listOf(101, 202), projectsAfterMutation.data!!.map { it.id })
    }
}
