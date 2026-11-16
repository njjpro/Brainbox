package com.njjpro.brainbox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.njjpro.brainbox.data.Note
import com.njjpro.brainbox.data.repeatLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    vm: BrainboxViewModel,
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onNewReminder: () -> Unit,
) {
    val notes by vm.notes.collectAsState()
    val now = System.currentTimeMillis()

    val withReminder = notes.filter { it.reminderAt != null }
    val upcoming = withReminder
        .filter { it.reminderActive && (it.reminderAt ?: 0L) > now }
        .sortedBy { it.reminderAt }
    val past = withReminder
        .filter { !it.reminderActive || (it.reminderAt ?: 0L) <= now }
        .sortedByDescending { it.reminderAt }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⏰ 提醒") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewReminder) {
                Icon(Icons.Default.Add, contentDescription = "新建提醒")
            }
        },
    ) { innerPadding ->
        if (withReminder.isEmpty()) {
            EmptyState("还没有提醒。点右下角 + 新建一条，比如「明天 9:00 去买菜」。", Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (upcoming.isNotEmpty()) {
                    item { SectionTitle("即将到来") }
                    items(upcoming, key = { it.id }) { note ->
                        ReminderRow(
                            note = note,
                            onOpen = { onOpenNote(note.id) },
                            onSnooze = { vm.snoozeReminder(note, 10) },
                            onDisable = { vm.disableReminder(note) },
                        )
                    }
                }
                if (past.isNotEmpty()) {
                    item { SectionTitle("已过期 / 已关闭") }
                    items(past, key = { "past-${it.id}" }) { note ->
                        ReminderRow(
                            note = note,
                            onOpen = { onOpenNote(note.id) },
                            onSnooze = { vm.snoozeReminder(note, 10) },
                            onDisable = null,
                            dimmed = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderRow(
    note: Note,
    onOpen: () -> Unit,
    onSnooze: () -> Unit,
    onDisable: (() -> Unit)?,
    dimmed: Boolean = false,
) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = note.reminderAt?.let { formatDateTime(it) } ?: "—",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                )
                if (note.repeatRule != "NONE") {
                    Text(
                        text = "  ·  ${repeatLabel(note.repeatRule)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = note.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val preview = note.content.trim().replace("\n", " ")
            if (preview.isNotEmpty() && preview != note.displayTitle) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onSnooze, label = { Text("延后 10 分钟") })
                if (onDisable != null) {
                    AssistChip(onClick = onDisable, label = { Text("关闭提醒") })
                }
            }
        }
    }
}
