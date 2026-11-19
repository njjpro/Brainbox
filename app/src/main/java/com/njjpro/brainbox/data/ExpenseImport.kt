package com.njjpro.brainbox.data

import java.time.LocalDate
import java.time.ZoneId

/**
 * 把粘贴进来的报销文本解析成一条条账目。
 *
 * 支持这种格式（月份标题行、合计行会自动跳过）：
 * ```
 * 2026年11月 报销明细
 * 11-01 周日  [打车]  补上个月的  ¥10.00
 * 合计 7 笔  ¥1709.24
 * ```
 */
object ExpenseImport {

    data class Row(
        /** 当天本地零点的毫秒时间戳 */
        val date: Long,
        val amountCents: Long,
        val item: String,
        val category: String,
        val source: String,
    )

    data class Result(val rows: List<Row>, val errors: List<String>) {
        val totalCents: Long get() = rows.sumOf { it.amountCents }
    }

    private val HEADER = Regex("""(\d{4})\s*年\s*(\d{1,2})\s*月""")
    private val DATE_HEAD = Regex("""^\s*(\d{4}\s*年\s*)?(\d{1,2})\s*[-/.月]\s*(\d{1,2})\s*日?""")
    private val TAG = Regex("""^\s*[\[【（(]\s*([^\]】）)]{1,10})\s*[\]】）)]""")
    private val AMOUNT_YEN = Regex("""[¥￥]\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""")
    private val AMOUNT_TAIL = Regex("""([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*元?\s*$""")
    private val REPEAT_BLANK = Regex("""[ \t]{2,}""")

    /** 按事项里的关键词判断类别；判断不出再用文本里给的标签，最后落到「其他」。 */
    fun guessCategory(item: String, tag: String): String {
        val text = item + " " + tag
        KEYWORDS.forEach { (category, words) ->
            if (words.any { text.contains(it) }) return category
        }
        val normalized = tag.trim()
        return when {
            normalized.isEmpty() -> "其他"
            EXPENSE_CATEGORIES.contains(normalized) -> normalized
            else -> CATEGORY_ALIASES.entries.firstOrNull { normalized.contains(it.key) }?.value ?: "其他"
        }
    }

    fun parse(text: String, today: LocalDate = LocalDate.now()): Result {
        val headerYear = HEADER.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val rows = mutableListOf<Row>()
        val errors = mutableListOf<String>()

        text.lineSequence().forEach { rawLine ->
            val line = rawLine.replace('\u00A0', ' ').trimEnd()
            if (line.isBlank()) return@forEach

            val dateMatch = DATE_HEAD.find(line) ?: return@forEach // 标题行 / 合计行
            val month = dateMatch.groupValues[2].toIntOrNull()
            val day = dateMatch.groupValues[3].toIntOrNull()
            if (month == null || day == null) return@forEach

            val year = dateMatch.groupValues[1].let { it.ifBlank { headerYear?.toString() ?: today.year.toString() } }
                .filter { it.isDigit() }
                .toIntOrNull() ?: today.year

            var rest = line.substring(dateMatch.range.last + 1).trim()
            // 去掉「周日」「星期一」这类星期
            rest = rest.replace(Regex("""^周\s*[一二三四五六日天]\s*"""), "")
                .replace(Regex("""^(星期|礼拜)\s*[一二三四五六日天]\s*"""), "")
                .trim()

            val tag = TAG.find(rest)?.let { match ->
                rest = rest.substring(match.range.last + 1).trim()
                match.groupValues[1]
            } ?: ""

            val yen = AMOUNT_YEN.findAll(rest).lastOrNull()
            val amountText = yen?.groupValues?.getOrNull(1)
                ?: AMOUNT_TAIL.find(rest)?.groupValues?.getOrNull(1)
            val amountCents = amountText?.let { parseAmountToCents(it) }

            if (amountCents == null || amountCents <= 0L) {
                errors += "无法识别金额：$rawLine"
                return@forEach
            }

            var item = rest
            yen?.let { item = item.removeRange(it.range) }
            if (yen == null) {
                AMOUNT_TAIL.find(item)?.let { item = item.removeRange(it.range) }
            }
            item = item.replace(REPEAT_BLANK, " ")
                .trim()
                .trimEnd('-', '—', '·', '|')
                .trim()
                .ifBlank { "（未填事项）" }

            val date = runCatching {
                LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.getOrElse {
                errors += "日期不合法：$rawLine"
                return@forEach
            }

            rows += Row(
                date = date,
                amountCents = amountCents,
                item = item,
                category = guessCategory(item, tag),
                source = rawLine.trim(),
            )
        }

        return Result(rows = rows.sortedWith(compareBy({ it.date }, { it.item })), errors = errors)
    }

    /** 关键词按优先级从高到低；命中即停。 */
    private val KEYWORDS: List<Pair<String, List<String>>> = listOf(
        "版面费" to listOf("版面费", "审稿费", "出版费", "论文费"),
        "会议" to listOf("会议", "注册费", "报名费", "会务", "研讨会", "论坛"),
        "住宿" to listOf("住宿", "酒店", "宾馆", "民宿", "旅馆"),
        "耗材" to listOf(
            "耗材", "电源", "转接", "线材", "线缆", "元件", "器件", "芯片", "天线",
            "焊", "万用表", "示波器", "开发板", "面包板", "仪器", "工具", "配件",
        ),
        "餐饮" to listOf("餐", "饭", "食品", "外卖", "咖啡", "奶茶", "零食", "聚餐"),
        "交通" to listOf(
            "打车", "出租", "滴滴", "网约车", "地铁", "公交", "高铁", "火车", "动车",
            "机票", "航班", "加油", "停车", "过路费", "交通", "大巴",
        ),
    )

    private val CATEGORY_ALIASES: Map<String, String> = mapOf(
        "打车" to "交通",
        "出租" to "交通",
        "餐食" to "餐饮",
        "吃饭" to "餐饮",
        "住" to "住宿",
        "会" to "会议",
        "版" to "版面费",
        "其" to "其他",
    )
}
