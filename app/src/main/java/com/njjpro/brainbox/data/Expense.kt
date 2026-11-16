package com.njjpro.brainbox.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.math.RoundingMode

val EXPENSE_CATEGORIES = listOf("交通", "餐饮", "住宿", "耗材", "会议", "版面费", "其他")

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** 记账日期，当天本地零点的毫秒时间戳 */
    val date: Long = System.currentTimeMillis(),
    /** 金额，单位「分」，避免浮点误差 */
    val amountCents: Long = 0L,
    val item: String = "",
    val category: String = "其他",
    val reimbursed: Boolean = false,
    val note: String = "",
) {
    val amountText: String get() = centsToText(amountCents)
}

fun centsToText(cents: Long): String =
    BigDecimal(cents).divide(BigDecimal(100)).setScale(2, RoundingMode.HALF_UP).toPlainString()

/** 解析用户输入的金额，返回「分」。非法输入返回 null。 */
fun parseAmountToCents(input: String): Long? {
    val cleaned = input.trim().replace("¥", "").replace(",", "")
    if (cleaned.isEmpty()) return null
    return runCatching {
        BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toLong()
    }.getOrNull()?.takeIf { it >= 0 }
}
