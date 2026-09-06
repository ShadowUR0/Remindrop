package com.shadowuro.remindrop.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.shadowuro.remindrop.R
import com.shadowuro.remindrop.data.Reminder
import com.shadowuro.remindrop.data.ReminderState
import com.shadowuro.remindrop.data.ReminderStore
import com.shadowuro.remindrop.reminder.ReminderNotifier
import com.shadowuro.remindrop.reminder.ReminderScheduler
import com.shadowuro.remindrop.reminder.TimePresets
import com.shadowuro.remindrop.ui.theme.RemindropTheme

class MainActivity : ComponentActivity() {
    private var reminders by mutableStateOf<List<Reminder>>(emptyList())
    private var notificationsAllowed by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderNotifier.ensureChannel(this)
        refresh()

        setContent {
            RemindropTheme {
                var showSettings by rememberSaveable { mutableStateOf(false) }
                var snoozeId by remember { mutableStateOf<Long?>(null) }
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {
                    refreshPermissionState()
                }

                if (showSettings) {
                    SettingsScreen(
                        notificationsAllowed = notificationsAllowed,
                        onBack = { showSettings = false },
                        onRequestNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                    )
                } else {
                    ReminderHome(
                        reminders = reminders,
                        notificationsAllowed = notificationsAllowed,
                        onOpenSettings = { showSettings = true },
                        onOpen = ::openReminder,
                        onDone = ::markDone,
                        onSnooze = { snoozeId = it.id },
                        onDelete = ::deleteReminder,
                    )
                }

                val id = snoozeId
                if (id != null) {
                    SnoozeSheet(
                        onDismiss = { snoozeId = null },
                        onSnooze = { time ->
                            val updated = ReminderStore.reschedule(this, id, time)
                            if (updated != null) {
                                ReminderNotifier.cancel(this, id)
                                ReminderScheduler.cancel(this, id)
                                ReminderScheduler.schedule(this, updated)
                            }
                            snoozeId = null
                            refresh()
                        },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        reminders = ReminderStore.all(this)
        refreshPermissionState()
    }

    private fun refreshPermissionState() {
        notificationsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openReminder(reminder: Reminder) {
        val url = reminder.url ?: return
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    private fun markDone(reminder: Reminder) {
        ReminderStore.markDone(this, reminder.id)
        ReminderScheduler.cancel(this, reminder.id)
        ReminderNotifier.cancel(this, reminder.id)
        refresh()
    }

    private fun deleteReminder(reminder: Reminder) {
        ReminderStore.delete(this, reminder.id)
        ReminderScheduler.cancel(this, reminder.id)
        ReminderNotifier.cancel(this, reminder.id)
        refresh()
    }
}

private enum class HomeTab { NOW, LATER, DONE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderHome(
    reminders: List<Reminder>,
    notificationsAllowed: Boolean,
    onOpenSettings: () -> Unit,
    onOpen: (Reminder) -> Unit,
    onDone: (Reminder) -> Unit,
    onSnooze: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(HomeTab.NOW) }
    val now = System.currentTimeMillis()
    val nowItems = reminders
        .filter { it.state == ReminderState.PENDING && it.scheduledAt <= now }
        .sortedBy { it.scheduledAt }
    val laterItems = reminders
        .filter { it.state == ReminderState.PENDING && it.scheduledAt > now }
        .sortedBy { it.scheduledAt }
    val doneItems = reminders
        .filter { it.state == ReminderState.DONE }
        .sortedByDescending { it.completedAt ?: it.createdAt }
    val visibleItems = when (tab) {
        HomeTab.NOW -> nowItems
        HomeTab.LATER -> laterItems
        HomeTab.DONE -> doneItems
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold)
                        Text(
                            text = stringResource(R.string.tagline),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (!notificationsAllowed) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        text = stringResource(R.string.permission_needed),
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = tab == HomeTab.NOW,
                    onClick = { tab = HomeTab.NOW },
                    label = { Text("${stringResource(R.string.now)} ${nowItems.size}") },
                )
                FilterChip(
                    selected = tab == HomeTab.LATER,
                    onClick = { tab = HomeTab.LATER },
                    label = { Text("${stringResource(R.string.later)} ${laterItems.size}") },
                )
                FilterChip(
                    selected = tab == HomeTab.DONE,
                    onClick = { tab = HomeTab.DONE },
                    label = { Text("${stringResource(R.string.done)} ${doneItems.size}") },
                )
            }

            if (visibleItems.isEmpty()) {
                EmptyState(tab = tab, modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(visibleItems, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onOpen = onOpen,
                            onDone = onDone,
                            onSnooze = onSnooze,
                            onDelete = onDelete,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(tab: HomeTab, modifier: Modifier = Modifier) {
    val text = when (tab) {
        HomeTab.NOW -> stringResource(R.string.all_caught_up)
        HomeTab.LATER -> stringResource(R.string.nothing_later)
        HomeTab.DONE -> stringResource(R.string.nothing_done)
    }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
            if (tab != HomeTab.DONE) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.empty_share_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    onOpen: (Reminder) -> Unit,
    onDone: (Reminder) -> Unit,
    onSnooze: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title.ifBlank { reminder.url ?: stringResource(R.string.app_name) },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (reminder.content.isNotBlank() && reminder.content != reminder.title) {
                        Text(
                            text = reminder.content,
                            modifier = Modifier.padding(top = 5.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (!reminder.url.isNullOrBlank()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.open)) },
                                onClick = {
                                    menuExpanded = false
                                    onOpen(reminder)
                                },
                            )
                        }
                        if (reminder.state == ReminderState.PENDING) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.mark_done)) },
                                onClick = {
                                    menuExpanded = false
                                    onDone(reminder)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.snooze)) },
                                onClick = {
                                    menuExpanded = false
                                    onSnooze(reminder)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = {
                                menuExpanded = false
                                onDelete(reminder)
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = TimePresets.format(reminder.scheduledAt),
                style = MaterialTheme.typography.labelLarge,
                color = if (reminder.state == ReminderState.PENDING && reminder.scheduledAt <= System.currentTimeMillis()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (!reminder.source.isNullOrBlank()) {
                Text(
                    text = reminder.source,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    notificationsAllowed: Boolean,
    onBack: () -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SettingsCard(
                    title = stringResource(R.string.privacy),
                    body = stringResource(R.string.privacy_detail),
                )
            }
            item {
                SettingsCard(
                    title = stringResource(R.string.reliability),
                    body = stringResource(R.string.reliability_detail),
                ) {
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${context.packageName}"),
                                )
                            )
                        },
                    ) {
                        Text(stringResource(R.string.battery_settings))
                    }
                }
            }
            if (!notificationsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    SettingsCard(
                        title = stringResource(R.string.notification_channel),
                        body = stringResource(R.string.permission_needed),
                    ) {
                        OutlinedButton(onClick = onRequestNotifications) {
                            Text(stringResource(R.string.notification_channel))
                        }
                    }
                }
            }
            item {
                SettingsCard(
                    title = stringResource(R.string.about),
                    body = stringResource(R.string.version),
                ) {
                    TextButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/ShadowUR0/Remindrop"),
                                    )
                                )
                            }
                        },
                    ) {
                        Text("GitHub")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    body: String,
    content: @Composable (() -> Unit)? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = body,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (content != null) {
                Spacer(Modifier.height(10.dp))
                content()
            }
        }
    }
}
