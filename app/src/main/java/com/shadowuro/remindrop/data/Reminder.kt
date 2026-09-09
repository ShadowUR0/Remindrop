package com.shadowuro.remindrop.data

enum class ReminderState {
    PENDING,
    DONE
}

data class Reminder(
    val id: Long,
    val title: String,
    val content: String,
    val url: String?,
    val sourceId: String = SourceTag.TEXT_ID,
    val scheduledAt: Long,
    val createdAt: Long,
    val state: ReminderState = ReminderState.PENDING,
    val completedAt: Long? = null,
)
