package com.shadowuro.remindrop.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.shadowuro.remindrop.R
import com.shadowuro.remindrop.data.ReminderStore
import com.shadowuro.remindrop.data.SourceTags
import com.shadowuro.remindrop.reminder.ReminderNotifier
import com.shadowuro.remindrop.reminder.ReminderScheduler
import com.shadowuro.remindrop.reminder.TimePresets
import com.shadowuro.remindrop.share.ShareParser
import com.shadowuro.remindrop.ui.theme.RemindropTheme

class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val shared = ShareParser.parse(this)
        if (shared == null) {
            finish()
            return
        }
        ReminderNotifier.ensureChannel(this)

        setContent {
            RemindropTheme {
                var pendingTime by remember { mutableLongStateOf(0L) }
                var waitingForPermission by remember { mutableStateOf(false) }

                val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    waitingForPermission = false
                    if (granted && pendingTime > 0L) {
                        saveReminder(
                            title = shared.title,
                            content = shared.content,
                            url = shared.url,
                            source = shared.source,
                            scheduledAt = pendingTime,
                        )
                    } else {
                        Toast.makeText(this, R.string.permission_needed, Toast.LENGTH_LONG).show()
                        finish()
                    }
                }

                if (!waitingForPermission) {
                    QuickScheduleSheet(
                        title = shared.title,
                        content = shared.content,
                        onDismiss = { finish() },
                        onSchedule = { scheduledAt ->
                            pendingTime = scheduledAt
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                waitingForPermission = true
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                saveReminder(
                                    title = shared.title,
                                    content = shared.content,
                                    url = shared.url,
                                    source = shared.source,
                                    scheduledAt = scheduledAt,
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    private fun saveReminder(
        title: String,
        content: String,
        url: String?,
        source: String?,
        scheduledAt: Long,
    ) {
        val reminder = ReminderStore.add(
            context = this,
            title = title,
            content = content,
            url = url,
            sourceId = SourceTags.resolveId(url, source),
            scheduledAt = scheduledAt,
        )
        ReminderScheduler.schedule(this, reminder)
        Toast.makeText(
            this,
            "${getString(R.string.app_name)} · ${TimePresets.format(scheduledAt)}",
            Toast.LENGTH_SHORT,
        ).show()
        finish()
    }
}
