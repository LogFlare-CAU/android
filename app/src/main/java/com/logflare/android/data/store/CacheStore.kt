package com.logflare.android.data.store

import com.logflare.android.data.session.SessionKey
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

data class Cached<T>(val value: T, val fetchedAt: Long)

/** Key/value cache whose entries are scoped to one [SessionKey] (server + user). */
interface CacheStore {
    suspend fun <T> read(session: SessionKey, key: String, serializer: KSerializer<T>): Cached<T>?

    /**
     * Stores [value]. When [generation] is given and [session] was [clear]ed since that
     * [generation] was taken, the write is dropped, so a fetch that was started before a
     * sign-out can't put that session's data back.
     */
    suspend fun <T> write(
        session: SessionKey,
        key: String,
        value: T,
        serializer: KSerializer<T>,
        fetchedAt: Long,
        generation: Long? = null,
    )

    /** Changes every time [session] is [clear]ed. Take it before a fetch and pass it to [write]. */
    fun generation(session: SessionKey): Long

    /** Removes every entry of [session], in memory and on disk. Other sessions are untouched. */
    suspend fun clear(session: SessionKey)
}

/**
 * [CacheStore] backed by `root/<session>/<sha1(key)>.json` files with an in-memory layer in front.
 *
 * Files are written atomically (temp file + rename), so a crash mid-write never leaves a torn
 * entry. Unreadable or undecodable files are treated as missing. A failed disk write keeps the
 * in-memory value: caching is best-effort and must never fail the caller.
 *
 * Every memory and disk update, and every load from disk into memory, happens under one lock,
 * which [clear] also holds, so nothing from before a [clear] can come back after it.
 */
class FileCacheStore(
    private val root: File,
    private val json: Json,
    private val io: CoroutineDispatcher,
) : CacheStore {

    private val memory = ConcurrentHashMap<String, Cached<*>>()
    private val generations = ConcurrentHashMap<String, Long>()
    private val lock = Mutex()

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> read(session: SessionKey, key: String, serializer: KSerializer<T>): Cached<T>? {
        val id = memoryId(session, key)
        (memory[id] as Cached<T>?)?.let { return it }
        return withContext(io) {
            lock.withLock {
                (memory[id] as Cached<T>?)
                    ?: readFile(file(session, key), serializer)?.also { memory[id] = it }
            }
        }
    }

    override suspend fun <T> write(
        session: SessionKey,
        key: String,
        value: T,
        serializer: KSerializer<T>,
        fetchedAt: Long,
        generation: Long?,
    ) {
        val text = buildJsonObject {
            put(FIELD_FETCHED_AT, fetchedAt)
            put(FIELD_VALUE, json.encodeToJsonElement(serializer, value))
        }.toString()
        withContext(io) {
            lock.withLock {
                if (generation != null && generation != generation(session)) return@withLock
                memory[memoryId(session, key)] = Cached(value, fetchedAt)
                try {
                    writeAtomically(file(session, key), text)
                } catch (_: IOException) {
                    // Best-effort: the in-memory entry still serves this process.
                }
            }
        }
    }

    override fun generation(session: SessionKey): Long = generations[session.value] ?: 0L

    override suspend fun clear(session: SessionKey) {
        val prefix = "${session.value}|"
        withContext(io) {
            lock.withLock {
                generations.merge(session.value, 1L, Long::plus)
                memory.keys.removeIf { it.startsWith(prefix) }
                sessionDir(session).deleteRecursively()
            }
        }
    }

    private fun <T> readFile(file: File, serializer: KSerializer<T>): Cached<T>? {
        if (!file.isFile) return null
        return try {
            val obj = json.parseToJsonElement(file.readText()).jsonObject
            val fetchedAt = obj.getValue(FIELD_FETCHED_AT).jsonPrimitive.long
            Cached(json.decodeFromJsonElement(serializer, obj.getValue(FIELD_VALUE)), fetchedAt)
        } catch (_: IOException) {
            null
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            // Wrong JSON shape (e.g. not an object, missing field, fetchedAt not a number).
            null
        } catch (_: NoSuchElementException) {
            null
        }
    }

    private fun writeAtomically(target: File, text: String) {
        val dir = target.parentFile ?: throw IOException("No parent for $target")
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Cannot create $dir")
        val temp = File(dir, target.name + ".tmp")
        temp.writeText(text)
        if (temp.renameTo(target)) return
        // Some platforms (Windows) refuse to rename onto an existing file.
        target.delete()
        if (temp.renameTo(target)) return
        try {
            temp.copyTo(target, overwrite = true)
        } finally {
            temp.delete()
        }
    }

    private fun sessionDir(session: SessionKey) = File(root, session.value)

    private fun file(session: SessionKey, key: String) = File(sessionDir(session), "${sha1(key)}.json")

    private fun memoryId(session: SessionKey, key: String) = "${session.value}|$key"

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val FIELD_FETCHED_AT = "fetchedAt"
        const val FIELD_VALUE = "value"
    }
}
