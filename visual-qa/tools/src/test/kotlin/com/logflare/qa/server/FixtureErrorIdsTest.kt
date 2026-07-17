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

class FixtureErrorIdsTest {
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
    fun seededErrorLogIdsAreFixedAt5001And5002WithNext5003() {
        val list = request("GET", "/log/error?limit=50&offset=0")
        val data = json.parseToJsonElement(list).jsonObject["data"]!!.jsonArray
        assertEquals(2, data.size)
        assertEquals(5001, data[0].jsonObject["id"]!!.jsonPrimitive.int)
        assertEquals(101, data[0].jsonObject["project_id"]!!.jsonPrimitive.int)
        assertEquals(5002, data[1].jsonObject["id"]!!.jsonPrimitive.int)
        assertEquals(202, data[1].jsonObject["project_id"]!!.jsonPrimitive.int)

        request(
            "POST",
            "/log/error",
            body = """{"errortype":"NPE","level":"ERROR","message":"next-id"}""",
            projectHeaders = true,
        )
        val after = request("GET", "/log/error?limit=50&offset=0")
        val afterData = json.parseToJsonElement(after).jsonObject["data"]!!.jsonArray
        assertEquals(3, afterData.size)
        assertEquals(5003, afterData[2].jsonObject["id"]!!.jsonPrimitive.int)
        assertEquals("next-id", afterData[2].jsonObject["message"]!!.jsonPrimitive.content)
    }

    private fun request(
        method: String,
        path: String,
        body: String? = null,
        projectHeaders: Boolean = false,
    ): String {
        val builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:${server.boundPort}$path"))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer qa-token-fixed")
        if (projectHeaders) {
            builder.header("ProjectKey", "qa-project-token-101")
            builder.header("Project", "Payments")
        }
        val publisher = body?.let {
            builder.header("Content-Type", "application/json; charset=utf-8")
            HttpRequest.BodyPublishers.ofString(it, StandardCharsets.UTF_8)
        } ?: HttpRequest.BodyPublishers.noBody()
        val response = client.send(
            builder.method(method, publisher).build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8),
        )
        assertEquals(200, response.statusCode())
        return response.body()
    }
}
