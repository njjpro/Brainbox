package com.njjpro.brainbox.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.njjpro.brainbox.data.EXPENSE_CATEGORIES
import com.njjpro.brainbox.data.Expense
import com.njjpro.brainbox.data.centsToText
import com.njjpro.brainbox.data.cycleOf
import com.njjpro.brainbox.data.cycleRangeText
import com.njjpro.brainbox.data.parseAmountToCents
import java.time.YearMonth
import java.time.ZoneId
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    vm: BrainboxViewModel,
    onBack: () -> Unit,
) {
    val expenses by vm.expenses.collectAsState()
    val cutoff = vm.expenseCutoffDay
    // 报销按「周期」看：结算日 26 日时，11 月这期是 10-27 ~ 11-26
    var month by remember { mutableStateOf(cycleOf(System.currentTimeMillis(), cutoff)) }
    var editing by remember { mutableStateOf<Expense?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }

    val monthExpenses = expenses.filter { cycleOf(it.date, cutoff) == month }
    val cycleText = "${monthLabel(month)}（${cycleRangeText(month, cutoff)}）"
    val monthTotal = monthExpenses.sumOf { it.amountCents }
    val monthUnpaid = monthExpenses.filter { !it.reimbursed }.sumOf { it.amountCents }
    val allUnpaid = expenses.filter { !it.reimbursed }.sumOf { it.amountCents }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🧾 每月报销") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showImport = true }) {
                        Icon(Icons.Default.Upload, contentDescription = "导入账单")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "记一笔")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { month = month.minusMonths(1) }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "上个月")
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = monthLabel(month),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = cycleRangeText(month, cutoff),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { month = month.plusMonths(1) }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "下个月")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            Column {
                                Text("本期合计", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatAmount(monthTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("未报销", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    formatAmount(monthUnpaid),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            Column {
                                Text("笔数", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${monthExpenses.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            if (monthExpenses.isEmpty()) {
                item { EmptyState("$cycleText 还没有记录。点右下角 + 记一笔，或用右上角导入。") }
            } else {
                items(monthExpenses, key = { it.id }) { expense ->
                    ExpenseRow(
                        expense = expense,
                        onToggle = { vm.toggleReimbursed(expense) },
                        onEdit = {
                            editing = expense
                            showDialog = true
                        },
                        onDelete = { vm.deleteExpense(expense) },
                    )
                }
            }

            item {
                Text(
                    text = "全部未报销合计：${formatAmount(allUnpaid)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }

    if (showImport) {
        ExpenseImportDialog(
            onDismiss = { showImport = false },
            onImport = { list ->
                vm.saveExpenses(list)
                showImport = false
            },
        )
    }

    if (showDialog) {
        ExpenseDialog(
            initial = editing,
            defaultMonth = month,
            onDismiss = { showDialog = false },
            onConfirm = { expense ->
                vm.saveExpense(expense)
                showDialog = false
            },
        )
    }
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = expense.reimbursed, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = expense.item.ifBlank { "（未填事项）" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${formatDate(expense.date)} · ${expense.category}" +
                        if (expense.reimbursed) " · 已报" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "¥${centsToText(expense.amountCents)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (expense.reimbursed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExpenseDialog(
    initial: Expense?,
    defaultMonth: YearMonth,
    onDismiss: () -> Unit,
    onConfirm: (Expense) -> Unit,
) {
    val context = LocalContext.current
    val defaultDate = remember(defaultMonth) {
        val now = Calendar.getInstance()
        if (yearMonthOf(now.timeInMillis) == defaultMonth) {
            now.timeInMillis
        } else {
            val calendar = Calendar.getInstance()
            calendar.set(defaultMonth.year, defaultMonth.monthValue - 1, 1, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
    }

    var date by remember { mutableStateOf(initial?.date ?: defaultDate) }
    var amountText by remember { mutableStateOf(initial?.let { centsToText(it.amountCents) } ?: "") }
    var item by remember { mutableStateOf(initial?.item ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: EXPENSE_CATEGORIES.first()) }
    var reimbursed by remember { mutableStateOf(initial?.reimbursed ?: false) }
    val parsed = parseAmountToCents(amountText)

    fun openDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val picked = Calendar.getInstance()
                picked.set(year, month, day, 0, 0, 0)
                picked.set(Calendar.MILLISECOND, 0)
                date = picked.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "记一笔" else "修改这笔") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssistChip(onClick = { openDatePicker() }, label = { Text(formatDate(date)) })
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("金额") },
                        singleLine = true,
                        isError = amountText.isNotBlank() && parsed == null,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = item,
                    onValueChange = { item = it },
                    label = { Text("事项") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EXPENSE_CATEGORIES.forEach { name ->
                        FilterChip(
                            selected = category == name,
                            onClick = { category = name },
                            label = { Text(name) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("已报销", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = reimbursed, onCheckedChange = { reimbursed = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cents = parsed ?: return@TextButton
                    onConfirm(
                        (initial ?: Expense()).copy(
                            date = date,
                            amountCents = cents,
                            item = item.trim(),
                            category = category,
                            reimbursed = reimbursed,
                        ),
                    )
                },
                enabled = parsed != null,
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
