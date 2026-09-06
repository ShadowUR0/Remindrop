package com.shadowuro.remindrop.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.shadowuro.remindrop.R
import com.shadowuro.remindrop.data.Reminder
import com.shadowuro.remindrop.ui.MainActivity
import com.shadowuro.remindrop.ui.SnoozeActivity

object ReminderNotifier {
    private const val CHANNEL_ID = "reminders"
    private const val EXTRA_ID = "reminder_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                enableVibration(true)
            }
        )
    }

    fun show(context: Context, reminder: Reminder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannel(context)

        val openPendingIntent = PendingIntent.getActivity(
            context,
            ReminderScheduler.requestCode(reminder.id) xor 0x42,
            openIntent(context, reminder),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            ReminderScheduler.requestCode(reminder.id) xor 0x21,
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(NotificationActionReceiver.ACTION_DONE)
                .putExtra(EXTRA_ID, reminder.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snoozePendingIntent = PendingIntent.getActivity(
            context,
            ReminderScheduler.requestCode(reminder.id) xor 0x63,
            Intent(context, SnoozeActivity::class.java)
                .putExtra(EXTRA_ID, reminder.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val displayTitle = reminder.title.ifBlank {
            reminder.url?.let { runCatching { Uri.parse(it).host }.getOrNull() }
                ?: context.getString(R.string.app_name)
        }
        val body = reminder.content.ifBlank { reminder.url.orEmpty() }.take(2_000)

        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notification_channel))
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(displayTitle)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openPendingIntent)
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setWhen(reminder.scheduledAt)
            .setShowWhen(true)
            .addAction(0, context.getString(R.string.mark_done), donePendingIntent)
            .addAction(0, context.getString(R.string.snooze), snoozePendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId(reminder.id), notification)
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(reminderId))
    }

    private fun openIntent(context: Context, reminder: Reminder): Intent {
        val url = reminder.url
        return if (!url.isNullOrBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_ID, reminder.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    private fun notificationId(id: Long): Int = ReminderScheduler.requestCode(id)
}
