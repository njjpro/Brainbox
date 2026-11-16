package com.njjpro.brainbox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.sp
import com.njjpro.brainbox.data.Category
import com.njjpro.brainbox.data.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: BrainboxViewModel,
    onOpenCategory: (Category) -> Unit,
    onOpenReminders: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val notes by vm.notes.collectAsState()
    val expenses by vm.expenses.collectAsState()

    val now = System.currentTimeMillis()
    val upcoming = notes.filter { it.reminderActive && (it.reminderAt ?: 0L) > now }
    val pendingAmount = expenses.filter { !it.reimbursed }.sumOf { it.amountCents }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Brainbox", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("待提醒", "${upcoming.size} 条", Modifier.weight(1f))
                    StatCard("待报销", formatAmount(pendingAmount), Modifier.weight(1f))
                }
            }

            item { SectionTitle("记点东西") }

            items(Category.entries.toList()) { category ->
                val count = if (category == Category.EXPENSE) {
                    expenses.size
                } else {
                    notes.count { it.category == category.id }
                }
                CategoryCard(
                    category = category,
                    count = count,
                    onClick = {
                        when (category) {
                            Category.EXPENSE -> onOpenExpenses()
                            Category.REMINDER -> onOpenReminders()
                            else -> onOpenCategory(category)
                        }
                    },
                )
            }

            item { SectionTitle("最近记录") }

            if (notes.isEmpty()) {
                item { EmptyState("还没有记录。点上面任意分类开始写第一条。") }
            } else {
                items(notes.take(6)) { note ->
                    NoteRow(note = note, onClick = { onOpenNote(note.id) })
                }
            }

            item { Spacer(Modifier.width(4.dp)) }
        }
    }
}

@Composable
private fun CategoryCard(category: Category, count: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = category.emoji, fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = category.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (count > 0) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun NoteRow(note: Note, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = note.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (note.pinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "已置顶",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(16.dp),
                    )
                }
            }
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(note.categoryEnum)
                Text(
                    text = if (note.reminderActive && note.reminderAt != null) {
                        "⏰ " + formatDateTime(note.reminderAt!!)
                    } else {
                        formatDateTime(note.updatedAt)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (note.reminderActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
