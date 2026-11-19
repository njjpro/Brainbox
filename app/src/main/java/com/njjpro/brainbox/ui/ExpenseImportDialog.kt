package com.njjpro.brainbox.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.njjpro.brainbox.data.EXPENSE_CATEGORIES
import com.njjpro.brainbox.data.Expense
import com.njjpro.brainbox.data.ExpenseImport
import com.njjpro.brainbox.data.centsToText

/**
 * 粘贴账单文本 → 解析 → 逐条确认类别 → 批量写入。
 * 解析规则：行首日期 + 可选 [类别] + 事项 + ¥金额；标题行与合计行自动跳过。
 */
@Composable
fun ExpenseImportDialog(
    onDismiss: () -> Unit,
    onImport: (List<Expense>) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<ExpenseImport.Row>?>(null) }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var expanded by remember { mutableStateOf(-1) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (rows == null) "导入报销账单" else "确认导入",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                val parsed = rows
                if (parsed == null) {
                    Text(
                        text = "把账单整段粘贴进来，每行一条，例如「11-05 周四  [打车]  学校-公司  ¥25.29」。\n标题行、合计行会自动跳过。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(min = 160.dp),
                        label = { Text("账单文本") },
                        minLines = 6,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        TextButton(
                            onClick = {
                                val result = ExpenseImport.parse(text)
                                rows = result.rows
                                errors = result.errors
                            },
                            enabled = text.isNotBlank(),
                        ) { Text("解析") }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            // 列表长的时候要能被压缩，否则底部按钮会被挤出对话框
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "共 ${parsed.size} 笔，合计 ¥${centsToText(parsed.sumOf { it.amountCents })}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "点类别可以改；认不出的会默认放进「其他」。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        parsed.forEachIndexed { index, row ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            ) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = formatDate(row.date),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = row.item,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            text = "¥${centsToText(row.amountCents)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        EXPENSE_CATEGORIES.forEach { category ->
                                            FilterChip(
                                                selected = row.category == category,
                                                onClick = {
                                                    rows = parsed.toMutableList().also {
                                                        it[index] = row.copy(category = category)
                                                    }
                                                },
                                                label = { Text(category) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (errors.isNotEmpty()) {
                            Text(
                                text = errors.joinToString("\n"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { rows = null }) { Text("重新粘贴") }
                        TextButton(onClick = onDismiss) { Text("取消") }
                        TextButton(
                            onClick = {
                                onImport(
                                    parsed.map { row ->
                                        Expense(
                                            date = row.date,
                                            amountCents = row.amountCents,
                                            item = row.item,
                                            category = row.category,
                                        )
                                    },
                                )
                            },
                            enabled = parsed.isNotEmpty(),
                        ) { Text("导入 ${parsed.size} 笔") }
                    }
                }
            }
        }
    }
}
