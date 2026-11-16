package com.njjpro.brainbox.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

const val REPEAT_NONE = "NONE"
const val REPEAT_DAILY = "DAILY"
const val REPEAT_WEEKLY = "WEEKLY"
const val REPEAT_MONTHLY = "MONTHLY"

val REPEAT_OPTIONS = listOf(
    REPEAT_NONE to "不重复",
    REPEAT_DAILY to "每天",
    REPEAT_WEEKLY to "每周",
    REPEAT_MONTHLY to "每月",
)

fun repeatLabel(rule: String): String =
    REPEAT_OPTIONS.firstOrNull { it.first == rule }?.second ?: "不重复"

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val category: String = Category.THOUGHT.id,
    val title: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val pinned: Boolean = false,
    val reminderAt: Long? = null,
    @ColumnInfo(name = "repeat_rule") val repeatRule: String = REPEAT_NONE,
    val reminderActive: Boolean = false,
) {
    val categoryEnum: Category get() = Category.of(category)

    val displayTitle: String
        get() = title.ifBlank {
            content.trim().lineSequence().firstOrNull { it.isNotBlank() }?.take(24) ?: "（空记录）"
        }
}
