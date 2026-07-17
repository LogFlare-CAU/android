package com.logflare.qa.server

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

class FixturePermissionCodesTest {
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

    @Test
    fun seededUsersMatchAppPermissionEnum() {
        val body = request("GET", "/user/")
        val data = json.parseToJsonElement(body).jsonObject["data"]!!.jsonArray
        assertEquals(100, data[0].jsonObject["permission"]!!.jsonPrimitive.int) // SUPER_USER
        assertEquals("qa-admin", data[0].jsonObject["username"]!!.jsonPrimitive.content)
        assertEquals(0, data[1].jsonObject["permission"]!!.jsonPrimitive.int) // USER
        assertEquals("qa-member", data[1].jsonObject["username"]!!.jsonPrimitive.content)
    }

    private fun request(method: String, path: String): String {
        val builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:${server.boundPort}$path"))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer qa-token-fixed")
        val response = client.send(
            builder.method(method, HttpRequest.BodyPublishers.noBody()).build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8),
        )
        assertEquals(200, response.statusCode())
        return response.body()
    }
}
