package com.shadowuro.remindrop.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.shadowuro.remindrop.R
import com.shadowuro.remindrop.data.Reminder
import com.shadowuro.remindrop.data.ReminderState
import com.shadowuro.remindrop.data.ReminderStore
import com.shadowuro.remindrop.data.SourceTag
import com.shadowuro.remindrop.data.SourceTags
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

                BackHandler(enabled = showSettings) { showSettings = false }

                AnimatedContent(
                    targetState = showSettings,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        val direction = if (targetState) 1 else -1
                        val enter = slideInHorizontally(tween(260)) { width -> direction * width / 6 } +
                            fadeIn(tween(200))
                        val exit = slideOutHorizontally(tween(260)) { width -> -direction * width / 6 } +
                            fadeOut(tween(160))
                        enter togetherWith exit
                    },
                    label = "screen",
                ) { settings ->
                    if (settings) {
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

private data class HomeSelection(val tab: HomeTab, val sourceId: String?)

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
    var sourceId by rememberSaveable { mutableStateOf<String?>(null) }
    val now = System.currentTimeMillis()
    val tags = remember(reminders) { reminders.associate { it.id to SourceTags.of(it) } }
    val sources = remember(tags) { sourcesOf(tags) }
    val counts = HomeTab.entries.associateWith { remindersFor(reminders, tags, it, sourceId, now).size }

    LaunchedEffect(sources) {
        if (sourceId != null && sources.none { it.id == sourceId }) sourceId = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                        )
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
            AnimatedVisibility(
                visible = !notificationsAllowed,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                NotificationBanner()
            }

            SectionChips(selected = tab, counts = counts, onSelect = { tab = it })

            AnimatedVisibility(
                visible = sources.size > 1,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                SourceChips(
                    sources = sources,
                    selectedId = sourceId,
                    onSelect = { sourceId = it },
                )
            }

            AnimatedContent(
                targetState = HomeSelection(tab, sourceId),
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val steps = targetState.tab.ordinal - initialState.tab.ordinal
                    val transform = if (steps == 0) {
                        fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                    } else {
                        val direction = if (steps > 0) 1 else -1
                        val enter = slideInHorizontally(tween(260)) { width -> direction * width / 10 } +
                            fadeIn(tween(200))
                        enter togetherWith fadeOut(tween(140))
                    }
                    transform using SizeTransform(clip = false)
                },
                label = "reminders",
            ) { selection ->
                val items = remindersFor(reminders, tags, selection.tab, selection.sourceId, now)
                if (items.isEmpty()) {
                    EmptyState(tab = selection.tab, modifier = Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        itemsIndexed(items, key = { _, reminder -> reminder.id }) { index, reminder ->
                            ReminderCard(
                                reminder = reminder,
                                tag = tags[reminder.id] ?: SourceTags.of(reminder),
                                position = cardPosition(index, items.size),
                                modifier = Modifier.animateItem(),
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
}

private fun sourcesOf(tags: Map<Long, SourceTag>): List<SourceTag> = tags.values
    .groupingBy { it }
    .eachCount()
    .entries
    .sortedWith(
        compareBy<Map.Entry<SourceTag, Int>> { it.key.id == SourceTag.TEXT_ID }
            .thenByDescending { it.value }
            .thenBy { it.key.label }
    )
    .map { it.key }

private fun remindersFor(
    reminders: List<Reminder>,
    tags: Map<Long, SourceTag>,
    tab: HomeTab,
    sourceId: String?,
    now: Long,
): List<Reminder> {
    val scoped = if (sourceId == null) {
        reminders
    } else {
        reminders.filter { tags[it.id]?.id == sourceId }
    }
    return when (tab) {
        HomeTab.NOW -> scoped
            .filter { it.state == ReminderState.PENDING && it.scheduledAt <= now }
            .sortedBy { it.scheduledAt }
        HomeTab.LATER -> scoped
            .filter { it.state == ReminderState.PENDING && it.scheduledAt > now }
            .sortedBy { it.scheduledAt }
        HomeTab.DONE -> scoped
            .filter { it.state == ReminderState.DONE }
            .sortedByDescending { it.completedAt ?: it.createdAt }
    }
}

@Composable
private fun sourceLabel(tag: SourceTag): String =
    if (tag.id == SourceTag.TEXT_ID) stringResource(R.string.tag_text) else tag.label

@Composable
private fun sectionIcon(tab: HomeTab): Painter = when (tab) {
    HomeTab.NOW -> painterResource(R.drawable.ic_schedule)
    HomeTab.LATER -> rememberVectorPainter(Icons.Default.DateRange)
    HomeTab.DONE -> rememberVectorPainter(Icons.Default.CheckCircle)
}

@Composable
private fun sectionLabel(tab: HomeTab): String = when (tab) {
    HomeTab.NOW -> stringResource(R.string.now)
    HomeTab.LATER -> stringResource(R.string.later)
    HomeTab.DONE -> stringResource(R.string.done)
}

@Composable
private fun NotificationBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.permission_needed),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SectionChips(
    selected: HomeTab,
    counts: Map<HomeTab, Int>,
    onSelect: (HomeTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HomeTab.entries.forEach { entry ->
            FilterChip(
                selected = selected == entry,
                onClick = { onSelect(entry) },
                label = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(sectionLabel(entry))
                        Text(
                            text = (counts[entry] ?: 0).toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = LocalContentColor.current.copy(alpha = 0.7f),
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        painter = sectionIcon(entry),
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
            )
        }
    }
}

private val selectedChipIcon: @Composable () -> Unit = {
    Icon(
        imageVector = Icons.Default.Check,
        contentDescription = null,
        modifier = Modifier.size(FilterChipDefaults.IconSize),
    )
}

@Composable
private fun SourceChips(
    sources: List<SourceTag>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            FilterChip(
                selected = selectedId == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.filter_all)) },
                modifier = Modifier.animateItem(),
                leadingIcon = if (selectedId == null) selectedChipIcon else null,
            )
        }
        items(sources, key = { it.id }) { source ->
            FilterChip(
                selected = selectedId == source.id,
                onClick = { onSelect(if (selectedId == source.id) null else source.id) },
                label = { Text(sourceLabel(source)) },
                modifier = Modifier.animateItem(),
                leadingIcon = if (selectedId == source.id) selectedChipIcon else null,
            )
        }
    }
}

@Composable
private fun EmptyState(tab: HomeTab, modifier: Modifier = Modifier) {
    val icon = when (tab) {
        HomeTab.NOW -> Icons.Default.Check
        HomeTab.LATER -> Icons.Default.DateRange
        HomeTab.DONE -> Icons.Default.CheckCircle
    }
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
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            if (tab != HomeTab.DONE) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.empty_share_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    tag: SourceTag,
    position: CardPosition,
    onOpen: (Reminder) -> Unit,
    onDone: (Reminder) -> Unit,
    onSnooze: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val canOpen = !reminder.url.isNullOrBlank()
    val isPending = reminder.state == ReminderState.PENDING
    val isDue = isPending && reminder.scheduledAt <= System.currentTimeMillis()
    val openLabel = stringResource(R.string.open)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (canOpen) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClickLabel = openLabel,
                        onClick = { onOpen(reminder) },
                    )
                } else {
                    Modifier
                }
            ),
        shape = groupedShape(position, pressed),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    SourcePill(tag)
                    Text(
                        text = reminder.title.ifBlank { reminder.url ?: stringResource(R.string.app_name) },
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (reminder.content.isNotBlank() && reminder.content != reminder.title) {
                        Text(
                            text = reminder.content,
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
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
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDelete(reminder)
                            },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = TimePresets.format(reminder.scheduledAt),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDue) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (canOpen) {
                    IconButton(onClick = { onOpen(reminder) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_open_in_new),
                            contentDescription = openLabel,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                if (isPending) {
                    IconButton(onClick = { onSnooze(reminder) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_schedule),
                            contentDescription = stringResource(R.string.snooze),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    FilledTonalIconButton(onClick = { onDone(reminder) }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.mark_done),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourcePill(tag: SourceTag) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = sourceLabel(tag),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

private class SettingsEntry(
    val title: String,
    val body: String,
    val action: (@Composable () -> Unit)? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    notificationsAllowed: Boolean,
    onBack: () -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val context = LocalContext.current
    val entries = buildList {
        add(
            SettingsEntry(
                title = stringResource(R.string.privacy),
                body = stringResource(R.string.privacy_detail),
            )
        )
        add(
            SettingsEntry(
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
                    Icon(
                        painter = painterResource(R.drawable.ic_open_in_new),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.battery_settings))
                }
            }
        )
        if (!notificationsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(
                SettingsEntry(
                    title = stringResource(R.string.notification_channel),
                    body = stringResource(R.string.permission_needed),
                ) {
                    OutlinedButton(onClick = onRequestNotifications) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.notification_channel))
                    }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            item(key = "header") {
                SettingsHeader(
                    onOpenGitHub = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/ShadowUR0/Remindrop"),
                                )
                            )
                        }
                    },
                )
            }
            itemsIndexed(entries, key = { _, entry -> entry.title }) { index, entry ->
                SettingsCard(
                    title = entry.title,
                    body = entry.body,
                    position = cardPosition(index, entries.size),
                    modifier = Modifier.animateItem(),
                    content = entry.action,
                )
            }
        }
    }
}

@Composable
private fun SettingsHeader(onOpenGitHub: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(84.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_notification),
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.padding(top = 14.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = stringResource(R.string.version),
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onOpenGitHub,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_github),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("GitHub")
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    body: String,
    position: CardPosition,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = groupedShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
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
