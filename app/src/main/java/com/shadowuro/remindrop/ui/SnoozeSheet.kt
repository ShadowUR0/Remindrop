package com.shadowuro.remindrop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shadowuro.remindrop.R
import com.shadowuro.remindrop.reminder.TimePresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnoozeSheet(
    onDismiss: () -> Unit,
    onSnooze: (Long) -> Unit,
) {
    val context = LocalContext.current
    val options = listOf(
        stringResource(R.string.snooze_30) to TimePresets.inThirtyMinutes(),
        stringResource(R.string.snooze_1h) to TimePresets.inOneHour(),
        stringResource(R.string.snooze_tonight) to TimePresets.thisEvening(),
        stringResource(R.string.snooze_tomorrow) to TimePresets.tomorrow(),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.snooze),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(16.dp))
            TimeOptionGroup(
                options = options,
                showTimes = true,
                onPick = onSnooze,
                onCustom = { showDateTimePicker(context, onSnooze) },
            )
        }
    }
}
