package com.njjpro.brainbox.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.njjpro.brainbox.data.Category
import com.njjpro.brainbox.data.Note
import com.njjpro.brainbox.data.REPEAT_NONE
import com.njjpro.brainbox.data.REPEAT_OPTIONS
import com.njjpro.brainbox.data.repeatLabel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    vm: BrainboxViewModel,
    noteId: Long,
    category: Category,
    onBack: () -> Unit,
) {
    val notes by vm.notes.collectAsState()
    val existing = remember(noteId, notes) { notes.firstOrNull { it.id == noteId } }
    val context = LocalContext.current

    var title by rememberSaveable(noteId) { mutableStateOf("") }
    var content by rememberSaveable(noteId) { mutableStateOf("") }
    var reminderAt by rememberSaveable(noteId) { mutableStateOf(0L) }
    var repeatRule by rememberSaveable(noteId) { mutableStateOf(REPEAT_NONE) }
    var pinned by rememberSaveable(noteId) { mutableStateOf(false) }
    var loaded by remember(noteId) { mutableStateOf(noteId <= 0L) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        val note = existing
        if (!loaded && note != null) {
            title = note.title
            content = note.content
            reminderAt = note.reminderAt ?: 0L
            repeatRule = note.repeatRule
            pinned = note.pinned
            loaded = true
        }
    }

    fun persist() {
        val base = existing ?: Note(category = category.id)
        vm.saveNote(
            base.copy(
                title = title.trim(),
                content = content.trimEnd(),
                reminderAt = reminderAt.takeIf { it > 0L },
                repeatRule = repeatRule,
                pinned = pinned,
            ),
        )
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (noteId <= 0L) "新建${category.label}" else category.label,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { pinned = !pinned }) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "置顶",
                            tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (existing != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                    IconButton(onClick = { persist() }) {
                        Icon(Icons.Default.Check, contentDescription = "保存")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("标题") },
                singleLine = true,
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                label = { Text("内容") },
                minLines = 6,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("定时提醒", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Switch(
                            checked = reminderAt > 0L,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    reminderAt = defaultReminderTime()
                                } else {
                                    reminderAt = 0L
                                    repeatRule = REPEAT_NONE
                                }
                            },
                        )
                    }

                    val current = reminderAt.takeIf { it > 0L }
                    if (current != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AssistChip(
                                onClick = {
                                    showDateTimePicker(context, current) { picked -> reminderAt = picked }
                                },
                                label = { Text("${formatDate(current)} ${formatTime(current)}") },
                            )
                            AssistChip(
                                onClick = { reminderAt = defaultReminderTime() },
                                label = { Text("+1 小时") },
                            )
                        }

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            REPEAT_OPTIONS.forEach { (rule, label) ->
                                FilterChip(
                                    selected = repeatRule == rule,
                                    onClick = { repeatRule = rule },
                                    label = { Text(label) },
                                )
                            }
                        }

                        val isPast = reminderAt <= System.currentTimeMillis()
                        Text(
                            text = "到点会发系统通知；重复提醒在下一次会自动续上。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (isPast) {
                            Text(
                                text = if (repeatRule == REPEAT_NONE) {
                                    "⚠️ 这个时间已经过去了，保存后不会响，请换成未来的时间。"
                                } else {
                                    "⚠️ 这个时间已经过去了，保存时会顺延到下一个周期。"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else {
                        Text(
                            text = "打开开关即可设置时间，比如「明天 9:00 去买菜」。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (existing != null && existing.repeatRule != REPEAT_NONE) {
                Text(
                    text = "当前为「${repeatLabel(existing.repeatRule)}」重复提醒",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showDeleteDialog && existing != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除这条记录？") },
            text = { Text("删除后不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteNote(existing)
                    showDeleteDialog = false
                    onBack()
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            },
        )
    }
}

private fun defaultReminderTime(): Long {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR_OF_DAY, 1)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
