package com.logflare.qa.server

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
import java.time.Duration

class MockServerContractTest {
    private lateinit var server: MockServer
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    @Before
    fun setUp() {
        server = MockServer(host = "127.0.0.1", port = 0)
        server.start()
    }

    @After
    fun tearDown() {
        server.stop()
    }

    private fun url(path: String): String = "http://127.0.0.1:${server.boundPort}$path"

    private data class HttpResult(
        val code: Int,
        val body: String,
        val contentType: String?,
        val contentLength: Long,
    )

    private fun request(
        method: String,
        path: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): HttpResult {
        val builder = HttpRequest.newBuilder(URI.create(url(path)))
            .timeout(Duration.ofSeconds(5))
            .header("Accept", "application/json")
        headers.forEach { (k, v) -> builder.header(k, v) }
        val publisher = if (body != null) {
            builder.header("Content-Type", "application/json; charset=utf-8")
            HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)
        } else {
            HttpRequest.BodyPublishers.noBody()
        }
        builder.method(method, publisher)
        val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        return HttpResult(
            code = response.statusCode(),
            body = response.body(),
            contentType = response.headers().firstValue("Content-Type").orElse(null),
            contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1),
        )
    }

    private fun authHeaders(): Map<String, String> =
        mapOf("Authorization" to "Bearer qa-token-fixed")

    private fun parseObject(body: String): JsonObject = json.parseToJsonElement(body).jsonObject

    private fun assertEnvelope(obj: JsonObject, success: Boolean? = true) {
        assertTrue(obj.containsKey("success"))
        assertTrue(obj.containsKey("message"))
        if (success != null) {
            assertEquals(success, obj["success"]!!.jsonPrimitive.boolean)
        }
    }

    @Test
    fun healthReturnsOk() {
        val result = request("GET", "/__qa/health")
        assertEquals(200, result.code)
        assertTrue(result.contentType!!.contains("application/json"))
        val obj = parseObject(result.body)
        assertEquals("ok", obj["status"]!!.jsonPrimitive.content)
    }

    @Test
    fun resetRestoresSeededStateAfterMutation() {
        val create = request(
            "POST",
            "/user/",
            body = """{"username":"tmp","permission":2,"password":"x"}""",
            headers = authHeaders(),
        )
        assertEquals(200, create.code)
        assertTrue(parseObject(create.body)["success"]!!.jsonPrimitive.boolean)

        val beforeReset = request("GET", "/user/", headers = authHeaders())
        val usersBefore = parseObject(beforeReset.body)["data"]!!.jsonArray
        assertTrue(usersBefore.size >= 3)

        val reset = request("POST", "/__qa/reset")
        assertEquals(200, reset.code)

        val afterReset = request("GET", "/user/", headers = authHeaders())
        val usersAfter = parseObject(afterReset.body)["data"]!!.jsonArray
        assertEquals(2, usersAfter.size)
        assertEquals("qa-admin", usersAfter[0].jsonObject["username"]!!.jsonPrimitive.content)
        assertEquals("qa-member", usersAfter[1].jsonObject["username"]!!.jsonPrimitive.content)

        val projects = request("GET", "/project/", headers = authHeaders())
        val projectData = parseObject(projects.body)["data"]!!.jsonArray
        assertEquals(2, projectData.size)
        assertEquals(101, projectData[0].jsonObject["id"]!!.jsonPrimitive.int)
        assertEquals(202, projectData[1].jsonObject["id"]!!.jsonPrimitive.int)
    }

    @Test
    fun authAcceptsQaCredentialsAndRejectsOthers() {
        val ok = request(
            "POST",
            "/user/auth",
            body = """{"username":"qa-admin","password":"qa-password","keep_logged_in":true}""",
        )
        assertEquals(200, ok.code)
        val okObj = parseObject(ok.body)
        assertEnvelope(okObj, success = true)
        assertEquals("qa-token-fixed", okObj["data"]!!.jsonPrimitive.content)

        val bad = request(
            "POST",
            "/user/auth",
            body = """{"username":"qa-admin","password":"wrong","keep_logged_in":false}""",
        )
        assertEquals(401, bad.code)
        assertFalse(parseObject(bad.body)["success"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun protectedRoutesRequireBearerToken() {
        val unauthorized = request("GET", "/user/")
        assertEquals(401, unauthorized.code)

        val wrong = request("GET", "/user/", headers = mapOf("Authorization" to "Bearer wrong"))
        assertEquals(401, wrong.code)

        val ok = request("GET", "/user/", headers = authHeaders())
        assertEquals(200, ok.code)
    }

    @Test
    fun getAllUsersReturnsDeterministicOrderAndShape() {
        val result = request("GET", "/user/", headers = authHeaders())
        assertEquals(200, result.code)
        assertTrue(result.contentType!!.lowercase().contains("application/json"))
        assertTrue(
            result.contentType!!.lowercase().contains("utf-8") ||
                result.contentType!!.lowercase().contains("charset"),
        )
        val obj = parseObject(result.body)
        assertEnvelope(obj)
        val data = obj["data"]!!.jsonArray
        assertEquals(2, data.size)
        assertEquals(1, data[0].jsonObject["idx"]!!.jsonPrimitive.int)
        assertEquals("qa-admin", data[0].jsonObject["username"]!!.jsonPrimitive.content)
        assertEquals(100, data[0].jsonObject["permission"]!!.jsonPrimitive.int)
        assertEquals(2, data[1].jsonObject["idx"]!!.jsonPrimitive.int)
        assertEquals("qa-member", data[1].jsonObject["username"]!!.jsonPrimitive.content)
        assertEquals(0, data[1].jsonObject["permission"]!!.jsonPrimitive.int)
    }

    @Test
    fun postUserCreatesDeterministicId() {
        val result = request(
            "POST",
            "/user/",
            body = """{"username":"new-user","permission":2,"password":"secret"}""",
            headers = authHeaders(),
        )
        assertEquals(200, result.code)
        val obj = parseObject(result.body)
        assertEnvelope(obj)
        val data = obj["data"]!!.jsonObject
        assertEquals(3, data["idx"]!!.jsonPrimitive.int)
        assertEquals("new-user", data["username"]!!.jsonPrimitive.content)
        assertEquals(2, data["permission"]!!.jsonPrimitive.int)
    }

    @Test
    fun getMeAndGetUserByName() {
        val me = request("GET", "/user/me", headers = authHeaders())
        assertEquals(200, me.code)
        val meObj = parseObject(me.body)
        assertEnvelope(meObj)
        assertEquals("qa-admin", meObj["data"]!!.jsonObject["username"]!!.jsonPrimitive.content)
        assertEquals(100, meObj["data"]!!.jsonObject["permission"]!!.jsonPrimitive.int)

        val byName = request("GET", "/user/name?username=qa-member", headers = authHeaders())
        assertEquals(200, byName.code)
        val nameObj = parseObject(byName.body)
        assertEnvelope(nameObj)
        assertEquals(2, nameObj["data"]!!.jsonObject["idx"]!!.jsonPrimitive.int)
        assertEquals("qa-member", nameObj["data"]!!.jsonObject["username"]!!.jsonPrimitive.content)
        assertEquals(0, nameObj["data"]!!.jsonObject["permission"]!!.jsonPrimitive.int)
    }

    @Test
    fun patchAndDeleteUserMutateState() {
        val patched = request(
            "PATCH",
            "/user/2",
            body = """{"username":"qa-member-renamed","permission":1,"password":null}""",
            headers = authHeaders(),
        )
        assertEquals(200, patched.code)
        val patchedData = parseObject(patched.body)["data"]!!.jsonObject
        assertEquals("qa-member-renamed", patchedData["username"]!!.jsonPrimitive.content)
        assertEquals(1, patchedData["permission"]!!.jsonPrimitive.int)

        val deleted = request("DELETE", "/user/2", headers = authHeaders())
        assertEquals(200, deleted.code)
        assertTrue(parseObject(deleted.body)["success"]!!.jsonPrimitive.boolean)

        val users = request("GET", "/user/", headers = authHeaders())
        val data = parseObject(users.body)["data"]!!.jsonArray
        assertEquals(1, data.size)
        assertEquals("qa-admin", data[0].jsonObject["username"]!!.jsonPrimitive.content)
    }

    @Test
    fun projectRoutesCoverListCreatePatchDeleteAndPerms() {
        val list = request("GET", "/project/", headers = authHeaders())
        assertEquals(200, list.code)
        val listObj = parseObject(list.body)
        assertEnvelope(listObj)
        val projects = listObj["data"]!!.jsonArray
        assertEquals(2, projects.size)
        val first = projects[0].jsonObject
        assertEquals(101, first["id"]!!.jsonPrimitive.int)
        assertEquals("Payments", first["name"]!!.jsonPrimitive.content)
        val logfiles = first["logfiles"]!!.jsonArray
        assertEquals(1, logfiles.size)
        assertEquals(1001, logfiles[0].jsonObject["id"]!!.jsonPrimitive.int)
        assertEquals(101, logfiles[0].jsonObject["project_id"]!!.jsonPrimitive.int)
        assertEquals("/var/log/payments.log", logfiles[0].jsonObject["file_path"]!!.jsonPrimitive.content)
        assertEquals("payments.log", logfiles[0].jsonObject["file_name"]!!.jsonPrimitive.content)

        val created = request(
            "POST",
            "/project/",
            body = """{"name":"Billing"}""",
            headers = authHeaders(),
        )
        assertEquals(200, created.code)
        val createdObj = parseObject(created.body)
        assertEnvelope(createdObj)
        val createdData = createdObj["data"]!!.jsonObject
        assertEquals(303, createdData["id"]!!.jsonPrimitive.int)
        assertEquals("Billing", createdData["name"]!!.jsonPrimitive.content)
        assertNotNull(createdData["token"]!!.jsonPrimitive.contentOrNull)

        val renamed = request(
            "PATCH",
            "/project/101",
            body = """{"name":"Payments-Updated"}""",
            headers = authHeaders(),
        )
        assertEquals(200, renamed.code)
        assertEquals(
            "Payments-Updated",
            parseObject(renamed.body)["data"]!!.jsonObject["name"]!!.jsonPrimitive.content,
        )

        val perms = request("GET", "/project/101/perm", headers = authHeaders())
        assertEquals(200, perms.code)
        val permsObj = parseObject(perms.body)
        assertEnvelope(permsObj)
        val permData = permsObj["data"]!!.jsonArray
        assertTrue(permData.size >= 1)
        val perm0 = permData[0].jsonObject
        assertTrue(perm0.containsKey("id"))
        assertTrue(perm0.containsKey("project_id"))
        assertTrue(perm0.containsKey("user_id"))
        assertTrue(perm0.containsKey("view"))

        val batch = request(
            "POST",
            "/project/perm/batch/reset",
            body = """{"projectid":101,"usernames":["qa-admin","qa-member"]}""",
            headers = authHeaders(),
        )
        assertEquals(200, batch.code)
        val batchObj = parseObject(batch.body)
        assertEnvelope(batchObj)
        assertEquals(2, batchObj["data"]!!.jsonArray.size)

        val deleted = request("DELETE", "/project/202", headers = authHeaders())
        assertEquals(200, deleted.code)
        assertEnvelope(parseObject(deleted.body))
        val afterDelete = request("GET", "/project/", headers = authHeaders())
        val remaining = parseObject(afterDelete.body)["data"]!!.jsonArray
        assertTrue(remaining.none { it.jsonObject["id"]!!.jsonPrimitive.int == 202 })
    }

    @Test
    fun logErrorAndLogFileRoutes() {
        val errors = request("GET", "/log/error?project_id=101&limit=50&offset=0", headers = authHeaders())
        assertEquals(200, errors.code)
        val errorsObj = parseObject(errors.body)
        assertEnvelope(errorsObj)
        val errorData = errorsObj["data"]!!.jsonArray
        assertTrue(errorData.size >= 1)
        val err = errorData[0].jsonObject
        assertTrue(err.containsKey("id"))
        assertTrue(err.containsKey("project_id"))
        assertTrue(err.containsKey("message"))
        assertTrue(err.containsKey("level"))
        assertTrue(err.containsKey("timestamp"))

        val postError = request(
            "POST",
            "/log/error",
            body = """{"errortype":"NPE","level":"ERROR","message":"boom"}""",
            headers = mapOf("ProjectKey" to "tok", "Project" to "Payments"),
        )
        assertEquals(200, postError.code)

        val logs = request(
            "GET",
            "/log/101/1001?limit=50&offset=0",
            headers = authHeaders(),
        )
        assertEquals(200, logs.code)
        val logsObj = parseObject(logs.body)
        assertEnvelope(logsObj)
        val lines = logsObj["data"]!!.jsonArray
        assertTrue(lines.size >= 1)
        assertTrue(lines[0] is JsonPrimitive)
    }

    @Test
    fun fcmDataReturnsAppLevelFailureAndTokenRegisters() {
        val config = request("GET", "/fcm/data", headers = authHeaders())
        assertEquals(200, config.code)
        val configObj = parseObject(config.body)
        assertEnvelope(configObj, success = false)
        assertTrue(configObj["data"] == null || configObj["data"] is JsonNull)

        val token = request(
            "POST",
            "/fcm/token",
            body = """{"fcm_token":"device-token"}""",
            headers = authHeaders(),
        )
        assertEquals(200, token.code)
        val tokenObj = parseObject(token.body)
        assertEnvelope(tokenObj, success = true)
        val data = tokenObj["data"]!!.jsonObject
        assertTrue(data.containsKey("idx"))
        assertTrue(data.containsKey("user_idx"))
    }

    @Test
    fun invalidMethodAndNotFound() {
        val methodNotAllowed = request("PUT", "/user/", headers = authHeaders())
        assertEquals(405, methodNotAllowed.code)

        val missing = request("GET", "/does-not-exist", headers = authHeaders())
        assertEquals(404, missing.code)
    }

    @Test
    fun everyStaticRouteRejectsUnsupportedMethodsWith405() {
        val cases = listOf(
            Triple("POST", "/__qa/health", emptyMap<String, String>()),
            Triple("GET", "/__qa/reset", emptyMap()),
            Triple("GET", "/user/auth", emptyMap()),
            Triple("POST", "/user/me", authHeaders()),
            Triple("POST", "/user/name?username=qa-admin", authHeaders()),
            Triple("GET", "/project/perm/batch/reset", authHeaders()),
            Triple("POST", "/fcm/data", authHeaders()),
            Triple("GET", "/fcm/token", authHeaders()),
        )

        cases.forEach { (method, path, headers) ->
            assertEquals("$method $path", 405, request(method, path, headers = headers).code)
        }
    }

    @Test
    fun postLogErrorRequiresNonblankProjectHeaders() {
        val body = """{"errortype":"NPE","level":"ERROR","message":"boom"}"""
        assertEquals(400, request("POST", "/log/error", body = body).code)
        assertEquals(
            400,
            request(
                "POST",
                "/log/error",
                body = body,
                headers = mapOf("ProjectKey" to " ", "Project" to "Payments"),
            ).code,
        )
        assertEquals(
            400,
            request(
                "POST",
                "/log/error",
                body = body,
                headers = mapOf("ProjectKey" to "token", "Project" to " "),
            ).code,
        )
    }

    @Test
    fun serverCanStartStopAndStartAgain() {
        val reusable = MockServer(host = "127.0.0.1", port = 0)
        reusable.start()
        val firstPort = reusable.boundPort
        reusable.stop()

        reusable.start()
        try {
            val response = client.send(
                HttpRequest.newBuilder(
                    URI.create("http://127.0.0.1:${reusable.boundPort}/__qa/health"),
                ).GET().build(),
                HttpResponse.BodyHandlers.ofString(),
            )
            assertEquals(200, response.statusCode())
            assertTrue(firstPort > 0)
        } finally {
            reusable.stop()
        }
    }

    @Test
    fun urlDecodingForQueryAndPath() {
        request(
            "POST",
            "/user/",
            body = """{"username":"spaced user","permission":2,"password":"x"}""",
            headers = authHeaders(),
        )
        val result = request("GET", "/user/name?username=spaced%20user", headers = authHeaders())
        assertEquals(200, result.code)
        assertEquals(
            "spaced user",
            parseObject(result.body)["data"]!!.jsonObject["username"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun responsesIncludeContentLength() {
        val result = request("GET", "/__qa/health")
        assertEquals(200, result.code)
        assertTrue(result.contentLength > 0)
        assertEquals(result.body.toByteArray(StandardCharsets.UTF_8).size.toLong(), result.contentLength)
    }
}
