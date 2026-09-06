package com.shadowuro.remindrop.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shadowuro.remindrop.R
import com.shadowuro.remindrop.reminder.TimePresets
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickScheduleSheet(
    title: String,
    content: String,
    onDismiss: () -> Unit,
    onSchedule: (Long) -> Unit,
) {
    val context = LocalContext.current
    val oneHour = TimePresets.inOneHour()
    val evening = TimePresets.thisEvening()
    val tomorrow = TimePresets.tomorrow()
    val weekend = TimePresets.thisWeekend()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.share_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.choose_when),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(Modifier.padding(16.dp)) {
                    if (title.isNotBlank()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (content.isNotBlank() && content != title) {
                        Text(
                            text = content,
                            modifier = Modifier.padding(top = if (title.isBlank()) 0.dp else 6.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            ScheduleRow(stringResource(R.string.in_one_hour), TimePresets.format(oneHour)) { onSchedule(oneHour) }
            ScheduleRow(stringResource(R.string.this_evening), TimePresets.format(evening)) { onSchedule(evening) }
            ScheduleRow(stringResource(R.string.tomorrow), TimePresets.format(tomorrow)) { onSchedule(tomorrow) }
            ScheduleRow(stringResource(R.string.this_weekend), TimePresets.format(weekend)) { onSchedule(weekend) }
            ScheduleRow(stringResource(R.string.custom_time), null) {
                showDateTimePicker(context, onSchedule)
            }
        }
    }
}

@Composable
private fun ScheduleRow(
    label: String,
    supporting: String?,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = supporting?.let { value -> { Text(value) } },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

fun showDateTimePicker(context: Context, onPicked: (Long) -> Unit) {
    val initial = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val picked = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    if (picked > System.currentTimeMillis()) onPicked(picked)
                },
                initial.get(Calendar.HOUR_OF_DAY),
                initial.get(Calendar.MINUTE),
                android.text.format.DateFormat.is24HourFormat(context),
            ).show()
        },
        initial.get(Calendar.YEAR),
        initial.get(Calendar.MONTH),
        initial.get(Calendar.DAY_OF_MONTH),
    ).apply {
        datePicker.minDate = System.currentTimeMillis() - 1_000L
    }.show()
}
