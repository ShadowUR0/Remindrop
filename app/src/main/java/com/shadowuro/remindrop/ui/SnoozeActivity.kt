package com.shadowuro.remindrop.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shadowuro.remindrop.data.ReminderStore
import com.shadowuro.remindrop.reminder.ReminderNotifier
import com.shadowuro.remindrop.reminder.ReminderScheduler
import com.shadowuro.remindrop.ui.theme.RemindropTheme

class SnoozeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id < 0L || ReminderStore.find(this, id) == null) {
            finish()
            return
        }

        setContent {
            RemindropTheme {
                SnoozeSheet(
                    onDismiss = { finish() },
                    onSnooze = { scheduledAt ->
                        val updated = ReminderStore.reschedule(this, id, scheduledAt)
                        if (updated != null) {
                            ReminderNotifier.cancel(this, id)
                            ReminderScheduler.cancel(this, id)
                            ReminderScheduler.schedule(this, updated)
                        }
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_ID = "reminder_id"
    }
}
