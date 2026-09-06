package com.shadowuro.remindrop.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shadowuro.remindrop.data.ReminderState
import com.shadowuro.remindrop.data.ReminderStore

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = ReminderScheduler.reminderId(intent)
        if (id < 0L) return
        val reminder = ReminderStore.find(context, id) ?: return
        if (reminder.state == ReminderState.PENDING) {
            ReminderNotifier.show(context, reminder)
        }
    }
}
