package com.logflare.qa.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean

class MockServer(
    private val host: String = "127.0.0.1",
    private val port: Int = 8000,
) {
    private val state = FixtureState()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val executor = Executors.newSingleThreadExecutor(
        ThreadFactory { runnable ->
            Thread(runnable, "qa-mock-server").apply { isDaemon = false }
        },
    )
    private var server: HttpServer? = null
    private val started = AtomicBoolean(false)

    val boundPort: Int
        get() = server?.address?.port ?: error("Server not started")

    fun start() {
        check(started.compareAndSet(false, true)) { "Server already started" }
        val httpServer = HttpServer.create(InetSocketAddress(host, port), 0)
        httpServer.executor = executor
        httpServer.createContext("/") { exchange ->
            try {
                handle(exchange)
            } catch (t: Throwable) {
                writeJson(
                    exchange,
                    500,
                    envelope(success = false, message = t.message ?: "internal error"),
                )
            }
        }
        httpServer.start()
        server = httpServer
    }

    fun stop() {
        server?.stop(0)
        server = null
        executor.shutdownNow()
        started.set(false)
    }

    private fun handle(exchange: HttpExchange) {
        val method = exchange.requestMethod.uppercase()
        val rawPath = exchange.requestURI.path ?: "/"
        val path = normalizePath(rawPath)
        val query = parseQuery(exchange.requestURI.rawQuery)

        when {
            method == "GET" && path == "/__qa/health" -> {
                writeRawJson(exchange, 200, """{"status":"ok"}""")
            }
            method == "POST" && path == "/__qa/reset" -> {
                state.reset()
                writeJson(exchange, 200, envelope(success = true, message = "reset"))
            }
            method == "POST" && path == "/user/auth" -> handleAuth(exchange)
            path == "/user/" || path == "/user" -> handleUsersCollection(exchange, method)
            path == "/user/me" -> requireAuth(exchange) {
                val user = state.getUser(1)!!
                writeJson(exchange, 200, userEnvelope(user))
            }
            path == "/user/name" -> requireAuth(exchange) {
                val username = query["username"]
                if (username.isNullOrBlank()) {
                    writeJson(exchange, 400, envelope(false, "username required"))
                    return@requireAuth
                }
                val user = state.getUserByName(username)
                if (user == null) {
                    writeJson(exchange, 404, envelope(false, "user not found"))
                } else {
                    writeJson(exchange, 200, userEnvelope(user))
                }
            }
            USER_ID_PATH.matches(path) -> handleUserById(exchange, method, path)
            path == "/project/" || path == "/project" -> handleProjectsCollection(exchange, method)
            path == "/project/perm/batch/reset" -> requireAuth(exchange) {
                if (method != "POST") {
                    writeJson(exchange, 405, envelope(false, "method not allowed"))
                    return@requireAuth
                }
                handlePermBatchReset(exchange)
            }
            PROJECT_PERM_PATH.matches(path) -> requireAuth(exchange) {
                if (method != "GET") {
                    writeJson(exchange, 405, envelope(false, "method not allowed"))
                    return@requireAuth
                }
                val projectId = PROJECT_PERM_PATH.matchEntire(path)!!.groupValues[1].toInt()
                val perms = state.getPerms(projectId)
                writeJson(
                    exchange,
                    200,
                    envelope(
                        success = true,
                        message = "ok",
                        data = JsonArray(perms.map { permToJson(it) }),
                    ),
                )
            }
            PROJECT_ID_PATH.matches(path) -> handleProjectById(exchange, method, path)
            path == "/log/error" -> handleLogError(exchange, method, query)
            LOG_FILE_PATH.matches(path) -> requireAuth(exchange) {
                if (method != "GET") {
                    writeJson(exchange, 405, envelope(false, "method not allowed"))
                    return@requireAuth
                }
                val match = LOG_FILE_PATH.matchEntire(path)!!
                val projectId = match.groupValues[1].toInt()
                val logFileId = match.groupValues[2].toInt()
                val lines = state.getLogLines(projectId, logFileId)
                writeJson(
                    exchange,
                    200,
                    envelope(
                        success = true,
                        message = "ok",
                        data = JsonArray(lines.map { JsonPrimitive(it) }),
                    ),
                )
            }
            path == "/fcm/data" -> requireAuth(exchange) {
                if (method != "GET") {
                    writeJson(exchange, 405, envelope(false, "method not allowed"))
                    return@requireAuth
                }
                writeJson(
                    exchange,
                    200,
                    envelope(success = false, message = "fcm disabled for qa", data = null),
                )
            }
            path == "/fcm/token" -> requireAuth(exchange) {
                if (method != "POST") {
                    writeJson(exchange, 405, envelope(false, "method not allowed"))
                    return@requireAuth
                }
                val idx = state.nextFcmTokenIdx()
                writeJson(
                    exchange,
                    200,
                    envelope(
                        success = true,
                        message = "ok",
                        data = buildJsonObject {
                            put("idx", JsonPrimitive(idx))
                            put("user_idx", JsonPrimitive(1))
                            put("last_delivery", JsonNull)
                        },
                    ),
                )
            }
            else -> writeJson(exchange, 404, envelope(false, "not found"))
        }
    }

    private fun handleAuth(exchange: HttpExchange) {
        val body = readBody(exchange)
        val obj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        if (obj == null) {
            writeJson(exchange, 400, envelope(false, "invalid json"))
            return
        }
        val username = obj["username"]?.jsonPrimitive?.contentOrNull
        val password = obj["password"]?.jsonPrimitive?.contentOrNull
        if (username == "qa-admin" && password == "qa-password") {
            writeJson(
                exchange,
                200,
                envelope(success = true, message = "ok", data = JsonPrimitive("qa-token-fixed")),
            )
        } else {
            writeJson(exchange, 401, envelope(false, "invalid credentials"))
        }
    }

    private fun handleUsersCollection(exchange: HttpExchange, method: String) {
        when (method) {
            "GET" -> requireAuth(exchange) {
                val users = state.listUsers()
                writeJson(
                    exchange,
                    200,
                    envelope(
                        success = true,
                        message = "ok",
                        data = JsonArray(users.map { userToJson(it) }),
                    ),
                )
            }
            "POST" -> requireAuth(exchange) {
                val body = readBody(exchange)
                val obj = json.parseToJsonElement(body).jsonObject
                val username = obj["username"]?.jsonPrimitive?.contentOrNull
                val permission = obj["permission"]?.jsonPrimitive?.intOrNull
                if (username.isNullOrBlank() || permission == null) {
                    writeJson(exchange, 400, envelope(false, "invalid body"))
                    return@requireAuth
                }
                val user = state.createUser(username, permission)
                writeJson(exchange, 200, userEnvelope(user))
            }
            else -> writeJson(exchange, 405, envelope(false, "method not allowed"))
        }
    }

    private fun handleUserById(exchange: HttpExchange, method: String, path: String) {
        val id = USER_ID_PATH.matchEntire(path)!!.groupValues[1].toInt()
        requireAuth(exchange) {
            when (method) {
                "PATCH" -> {
                    val body = readBody(exchange)
                    val obj = json.parseToJsonElement(body).jsonObject
                    val username = obj["username"]?.let {
                        if (it is JsonNull) null else it.jsonPrimitive.contentOrNull
                    }
                    val permission = obj["permission"]?.let {
                        if (it is JsonNull) null else it.jsonPrimitive.intOrNull
                    }
                    val updated = state.updateUser(id, username, permission)
                    if (updated == null) {
                        writeJson(exchange, 404, envelope(false, "user not found"))
                    } else {
                        writeJson(exchange, 200, userEnvelope(updated))
                    }
                }
                "DELETE" -> {
                    val deleted = state.deleteUser(id)
                    if (deleted == null) {
                        writeJson(exchange, 404, envelope(false, "user not found"))
                    } else {
                        writeJson(exchange, 200, userEnvelope(deleted))
                    }
                }
                else -> writeJson(exchange, 405, envelope(false, "method not allowed"))
            }
        }
    }

    private fun handleProjectsCollection(exchange: HttpExchange, method: String) {
        when (method) {
            "GET" -> requireAuth(exchange) {
                val projects = state.listProjects()
                writeJson(
                    exchange,
                    200,
                    envelope(
                        success = true,
                        message = "ok",
                        data = JsonArray(projects.map { projectToJson(it) }),
                    ),
                )
            }
            "POST" -> requireAuth(exchange) {
                val body = readBody(exchange)
                val name = json.parseToJsonElement(body).jsonObject["name"]?.jsonPrimitive?.contentOrNull
                if (name.isNullOrBlank()) {
                    writeJson(exchange, 400, envelope(false, "invalid body"))
                    return@requireAuth
                }
                val (project, token) = state.createProject(name)
                writeJson(
                    exchange,
                    200,
                    envelope(
                        success = true,
                        message = "ok",
                        data = projectToJsonWithToken(project, token),
                    ),
                )
            }
            else -> writeJson(exchange, 405, envelope(false, "method not allowed"))
        }
    }

    private fun handleProjectById(exchange: HttpExchange, method: String, path: String) {
        val id = PROJECT_ID_PATH.matchEntire(path)!!.groupValues[1].toInt()
        requireAuth(exchange) {
            when (method) {
                "PATCH" -> {
                    val body = readBody(exchange)
                    val name = json.parseToJsonElement(body).jsonObject["name"]?.jsonPrimitive?.contentOrNull
                    if (name.isNullOrBlank()) {
                        writeJson(exchange, 400, envelope(false, "invalid body"))
                        return@requireAuth
                    }
                    val updated = state.renameProject(id, name)
                    if (updated == null) {
                        writeJson(exchange, 404, envelope(false, "project not found"))
                    } else {
                        writeJson(
                            exchange,
                            200,
                            envelope(success = true, message = "ok", data = projectToJson(updated)),
                        )
                    }
                }
                "DELETE" -> {
                    val removed = state.deleteProject(id)
                    if (!removed) {
                        writeJson(exchange, 404, envelope(false, "project not found"))
                    } else {
                        writeJson(
                            exchange,
                            200,
                            envelope(success = true, message = "ok", data = JsonPrimitive("deleted")),
                        )
                    }
                }
                else -> writeJson(exchange, 405, envelope(false, "method not allowed"))
            }
        }
    }

    private fun handlePermBatchReset(exchange: HttpExchange) {
        val body = readBody(exchange)
        val obj = json.parseToJsonElement(body).jsonObject
        val projectId = obj["projectid"]?.jsonPrimitive?.intOrNull
        val usernames = obj["usernames"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?.toSet()
            .orEmpty()
        if (projectId == null) {
            writeJson(exchange, 400, envelope(false, "invalid body"))
            return
        }
        val perms = state.resetPerms(projectId, usernames)
        writeJson(
            exchange,
            200,
            envelope(
                success = true,
                message = "ok",
                data = JsonArray(perms.map { permToJson(it) }),
            ),
        )
    }

    private fun handleLogError(exchange: HttpExchange, method: String, query: Map<String, String>) {
        when (method) {
            "GET" -> requireAuth(exchange) {
                val projectId = query["project_id"]?.toIntOrNull()
                val errors = state.listErrors(projectId)
                writeJson(
                    exchange,
                    200,
                    envelope(
                        success = true,
                        message = "ok",
                        data = JsonArray(errors.map { errorToJson(it) }),
                    ),
                )
            }
            "POST" -> {
                // Authenticated via ProjectKey/Project headers for ingestion path.
                val body = readBody(exchange)
                val obj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
                if (obj == null) {
                    writeJson(exchange, 400, envelope(false, "invalid json"))
                    return
                }
                val message = obj["message"]?.jsonPrimitive?.contentOrNull
                val level = obj["level"]?.jsonPrimitive?.contentOrNull
                if (message.isNullOrBlank() || level.isNullOrBlank()) {
                    writeJson(exchange, 400, envelope(false, "invalid body"))
                    return
                }
                val errortype = obj["errortype"]?.let {
                    if (it is JsonNull) null else it.jsonPrimitive.contentOrNull
                }
                state.addError(projectId = 101, errortype = errortype, level = level, message = message)
                writeEmpty(exchange, 200)
            }
            else -> writeJson(exchange, 405, envelope(false, "method not allowed"))
        }
    }

    private inline fun requireAuth(exchange: HttpExchange, block: () -> Unit) {
        val auth = exchange.requestHeaders.getFirst("Authorization")
        if (auth != "Bearer qa-token-fixed") {
            writeJson(exchange, 401, envelope(false, "unauthorized"))
            return
        }
        block()
    }

    private fun readBody(exchange: HttpExchange): String =
        exchange.requestBody.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

    private fun writeJson(exchange: HttpExchange, status: Int, body: JsonObject) {
        writeRawJson(exchange, status, json.encodeToString(JsonObject.serializer(), body))
    }

    private fun writeRawJson(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun writeEmpty(exchange: HttpExchange, status: Int) {
        exchange.sendResponseHeaders(status, -1)
        exchange.responseBody.close()
    }

    private fun envelope(
        success: Boolean,
        message: String,
        data: JsonElement? = null,
        errorCode: Int? = null,
    ): JsonObject = buildJsonObject {
        put("success", JsonPrimitive(success))
        put("message", JsonPrimitive(message))
        put("error_code", errorCode?.let { JsonPrimitive(it) } ?: JsonNull)
        put("data", data ?: JsonNull)
    }

    private fun userEnvelope(user: QaUser): JsonObject =
        envelope(success = true, message = "ok", data = userToJson(user))

    private fun userToJson(user: QaUser): JsonObject = buildJsonObject {
        put("idx", JsonPrimitive(user.idx))
        put("username", JsonPrimitive(user.username))
        put("permission", JsonPrimitive(user.permission))
    }

    private fun projectToJson(project: QaProject): JsonObject = buildJsonObject {
        put("id", JsonPrimitive(project.id))
        put("name", JsonPrimitive(project.name))
        put("alias", JsonNull)
        put("description", JsonNull)
        put(
            "logfiles",
            buildJsonArray {
                for (log in project.logfiles) {
                    add(
                        buildJsonObject {
                            put("id", JsonPrimitive(log.id))
                            put("project_id", JsonPrimitive(log.project_id))
                            put("file_path", JsonPrimitive(log.file_path))
                            put("file_name", JsonPrimitive(log.file_name))
                        },
                    )
                }
            },
        )
    }

    private fun projectToJsonWithToken(project: QaProject, token: String): JsonObject =
        buildJsonObject {
            put("id", JsonPrimitive(project.id))
            put("name", JsonPrimitive(project.name))
            put("alias", JsonNull)
            put("description", JsonNull)
            put("token", JsonPrimitive(token))
            put(
                "logfiles",
                buildJsonArray {
                    for (log in project.logfiles) {
                        add(
                            buildJsonObject {
                                put("id", JsonPrimitive(log.id))
                                put("project_id", JsonPrimitive(log.project_id))
                                put("file_path", JsonPrimitive(log.file_path))
                                put("file_name", JsonPrimitive(log.file_name))
                            },
                        )
                    }
                },
            )
        }

    private fun permToJson(perm: QaProjectPerm): JsonObject = buildJsonObject {
        put("id", JsonPrimitive(perm.id))
        put("project_id", JsonPrimitive(perm.project_id))
        put("user_id", JsonPrimitive(perm.user_id))
        put("view", JsonPrimitive(perm.view))
        put("project", JsonNull)
    }

    private fun errorToJson(error: QaErrorLog): JsonObject = buildJsonObject {
        put("id", JsonPrimitive(error.id))
        put("project_id", JsonPrimitive(error.project_id))
        put("errortype", error.errortype?.let { JsonPrimitive(it) } ?: JsonNull)
        put("message", JsonPrimitive(error.message))
        put("level", JsonPrimitive(error.level))
        put("timestamp", JsonPrimitive(error.timestamp))
    }

    companion object {
        private val USER_ID_PATH = Regex("""^/user/(\d+)$""")
        private val PROJECT_ID_PATH = Regex("""^/project/(\d+)$""")
        private val PROJECT_PERM_PATH = Regex("""^/project/(\d+)/perm$""")
        private val LOG_FILE_PATH = Regex("""^/log/(\d+)/(\d+)$""")

        private fun normalizePath(path: String): String {
            if (path.length > 1 && path.endsWith('/')) {
                // Keep collection trailing slash semantics for /user/ and /project/.
                val trimmed = path.trimEnd('/')
                return when (trimmed) {
                    "/user", "/project" -> "$trimmed/"
                    else -> trimmed.ifEmpty { "/" }
                }
            }
            return path
        }

        private fun parseQuery(raw: String?): Map<String, String> {
            if (raw.isNullOrBlank()) return emptyMap()
            return raw.split('&').mapNotNull { part ->
                if (part.isBlank()) return@mapNotNull null
                val idx = part.indexOf('=')
                if (idx < 0) {
                    decode(part) to ""
                } else {
                    decode(part.substring(0, idx)) to decode(part.substring(idx + 1))
                }
            }.toMap()
        }

        private fun decode(value: String): String =
            URLDecoder.decode(value, StandardCharsets.UTF_8)
    }
}
