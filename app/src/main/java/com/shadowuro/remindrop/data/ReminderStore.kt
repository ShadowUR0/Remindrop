package com.shadowuro.remindrop.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.random.Random

object ReminderStore {
    private const val FILE_NAME = "reminders.json"
    private const val MAX_CONTENT_LENGTH = 12_000
    private val lock = Any()

    fun all(context: Context): List<Reminder> = synchronized(lock) {
        readUnsafe(context)
    }

    fun find(context: Context, id: Long): Reminder? = synchronized(lock) {
        readUnsafe(context).firstOrNull { it.id == id }
    }

    fun add(
        context: Context,
        title: String,
        content: String,
        url: String?,
        source: String?,
        scheduledAt: Long,
    ): Reminder = synchronized(lock) {
        val items = readUnsafe(context).toMutableList()
        val now = System.currentTimeMillis()
        var id = now * 1000L + Random.nextInt(1000)
        while (items.any { it.id == id }) id++

        val reminder = Reminder(
            id = id,
            title = title.trim().take(160),
            content = content.trim().take(MAX_CONTENT_LENGTH),
            url = url?.trim()?.take(2_048),
            source = source?.trim()?.take(120),
            scheduledAt = scheduledAt,
            createdAt = now,
        )
        items += reminder
        writeUnsafe(context, items)
        reminder
    }

    fun markDone(context: Context, id: Long): Reminder? = synchronized(lock) {
        val items = readUnsafe(context).toMutableList()
        val index = items.indexOfFirst { it.id == id }
        if (index == -1) return@synchronized null
        val updated = items[index].copy(
            state = ReminderState.DONE,
            completedAt = System.currentTimeMillis(),
        )
        items[index] = updated
        writeUnsafe(context, items)
        updated
    }

    fun reschedule(context: Context, id: Long, scheduledAt: Long): Reminder? = synchronized(lock) {
        val items = readUnsafe(context).toMutableList()
        val index = items.indexOfFirst { it.id == id }
        if (index == -1) return@synchronized null
        val updated = items[index].copy(
            scheduledAt = scheduledAt,
            state = ReminderState.PENDING,
            completedAt = null,
        )
        items[index] = updated
        writeUnsafe(context, items)
        updated
    }

    fun delete(context: Context, id: Long): Boolean = synchronized(lock) {
        val items = readUnsafe(context).toMutableList()
        val removed = items.removeAll { it.id == id }
        if (removed) writeUnsafe(context, items)
        removed
    }

    private fun readUnsafe(context: Context): List<Reminder> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return runCatching {
            val text = AtomicFile(file).openRead().bufferedReader().use { it.readText() }
            val array = JSONArray(text)
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    add(
                        Reminder(
                            id = json.getLong("id"),
                            title = json.optString("title"),
                            content = json.optString("content"),
                            url = json.optNullableString("url"),
                            source = json.optNullableString("source"),
                            scheduledAt = json.getLong("scheduledAt"),
                            createdAt = json.getLong("createdAt"),
                            state = runCatching {
                                ReminderState.valueOf(json.optString("state", ReminderState.PENDING.name))
                            }.getOrDefault(ReminderState.PENDING),
                            completedAt = json.optNullableLong("completedAt"),
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun writeUnsafe(context: Context, reminders: List<Reminder>) {
        val array = JSONArray()
        reminders.forEach { reminder ->
            array.put(
                JSONObject().apply {
                    put("id", reminder.id)
                    put("title", reminder.title)
                    put("content", reminder.content)
                    put("url", reminder.url ?: JSONObject.NULL)
                    put("source", reminder.source ?: JSONObject.NULL)
                    put("scheduledAt", reminder.scheduledAt)
                    put("createdAt", reminder.createdAt)
                    put("state", reminder.state.name)
                    put("completedAt", reminder.completedAt ?: JSONObject.NULL)
                }
            )
        }

        val atomicFile = AtomicFile(File(context.filesDir, FILE_NAME))
        val stream = atomicFile.startWrite()
        try {
            stream.write(array.toString().toByteArray(Charsets.UTF_8))
            stream.flush()
            atomicFile.finishWrite(stream)
        } catch (error: Throwable) {
            atomicFile.failWrite(stream)
            throw error
        }
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.optNullableLong(key: String): Long? =
        if (isNull(key) || !has(key)) null else optLong(key)
}
