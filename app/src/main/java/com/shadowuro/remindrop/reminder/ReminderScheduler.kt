package com.shadowuro.remindrop.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.shadowuro.remindrop.data.Reminder
import com.shadowuro.remindrop.data.ReminderState
import com.shadowuro.remindrop.data.ReminderStore

object ReminderScheduler {
    private const val EXTRA_ID = "reminder_id"

    fun schedule(context: Context, reminder: Reminder) {
        if (reminder.state != ReminderState.PENDING) return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAt = reminder.scheduledAt.coerceAtLeast(System.currentTimeMillis() + 1_000L)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent(context, reminder.id),
        )
    }

    fun cancel(context: Context, reminderId: Long) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(pendingIntent(context, reminderId))
    }

    fun reschedulePending(context: Context) {
        ReminderStore.all(context)
            .asSequence()
            .filter { it.state == ReminderState.PENDING }
            .forEach { schedule(context, it) }
    }

    fun reminderId(intent: Intent): Long = intent.getLongExtra(EXTRA_ID, -1L)

    fun intent(context: Context, reminderId: Long): Intent =
        Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_ID, reminderId)

    private fun pendingIntent(context: Context, reminderId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode(reminderId),
            intent(context, reminderId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun requestCode(id: Long): Int = (id xor (id ushr 32)).toInt()
}
