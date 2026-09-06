package com.shadowuro.remindrop.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shadowuro.remindrop.data.ReminderStore

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DONE) return
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id < 0L) return
        ReminderStore.markDone(context, id)
        ReminderScheduler.cancel(context, id)
        ReminderNotifier.cancel(context, id)
    }

    companion object {
        const val ACTION_DONE = "com.shadowuro.remindrop.action.DONE"
        private const val EXTRA_ID = "reminder_id"
    }
}
