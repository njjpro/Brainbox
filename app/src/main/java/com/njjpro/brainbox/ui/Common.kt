package com.njjpro.brainbox.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.njjpro.brainbox.data.Category
import com.njjpro.brainbox.data.Note
import com.njjpro.brainbox.data.centsToText
import com.njjpro.brainbox.data.repeatLabel
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

private val zone: ZoneId get() = ZoneId.systemDefault()

private val fmtDate = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)
private val fmtTime = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
private val fmtDateTime = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)

fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(fmtDate)

fun formatTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(fmtTime)

fun formatDateTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(zone).format(fmtDateTime)

fun formatAmount(cents: Long): String = "¥" + centsToText(cents)

fun yearMonthOf(millis: Long): YearMonth {
    val local = Instant.ofEpochMilli(millis).atZone(zone)
    return YearMonth.of(local.year, local.monthValue)
}

fun monthLabel(month: YearMonth): String = "${month.year}年${month.monthValue}月"

fun startOfDay(year: Int, month: Int, day: Int): Long {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, day, 0, 0, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

fun reminderText(note: Note): String {
    val at = note.reminderAt ?: return "无提醒"
    val repeat = note.repeatRule
    return if (repeat == "NONE") formatDateTime(at) else "${formatDateTime(at)} · ${repeatLabel(repeat)}"
}

/** 先选日期再选时间，返回毫秒时间戳。 */
fun showDateTimePicker(context: Context, initialMillis: Long, onPicked: (Long) -> Unit) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialMillis }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val picked = Calendar.getInstance()
                    picked.set(year, month, day, hour, minute, 0)
                    picked.set(Calendar.MILLISECOND, 0)
                    onPicked(picked.timeInMillis)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true,
            ).show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH),
    ).show()
}

@Composable
fun CategoryBadge(category: Category, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = "${category.emoji} ${category.label}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
