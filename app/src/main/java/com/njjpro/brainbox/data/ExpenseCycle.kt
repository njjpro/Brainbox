package com.njjpro.brainbox.data

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** 报销结算日默认值：每月 26 日。 */
const val DEFAULT_CUTOFF_DAY = 26

/**
 * 这笔支出属于哪一期。期次用「结算月」表示：
 * 结算日 26 日时，11 月这一期覆盖 10-27 ~ 11-26（含 26 日），11-27 起算 12 月这期。
 */
fun cycleOf(
    millis: Long,
    cutoffDay: Int = DEFAULT_CUTOFF_DAY,
    zone: ZoneId = ZoneId.systemDefault(),
): YearMonth {
    val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    val cutoff = cutoffDay.coerceIn(1, 31)
    val thisMonth = YearMonth.of(date.year, date.monthValue)
    return if (date.dayOfMonth > cutoff) thisMonth.plusMonths(1) else thisMonth
}

/** 这一期的起止毫秒时间戳，左闭右开。 */
fun cycleRange(
    cycle: YearMonth,
    cutoffDay: Int = DEFAULT_CUTOFF_DAY,
    zone: ZoneId = ZoneId.systemDefault(),
): Pair<Long, Long> {
    val cutoff = cutoffDay.coerceIn(1, 31)
    val previous = cycle.minusMonths(1)
    val start: LocalDate = previous.atDay(minOf(cutoff, previous.lengthOfMonth())).plusDays(1)
    val end: LocalDate = cycle.atDay(minOf(cutoff, cycle.lengthOfMonth())).plusDays(1)
    return start.atStartOfDay(zone).toInstant().toEpochMilli() to
        end.atStartOfDay(zone).toInstant().toEpochMilli()
}

/** 「10-27 ~ 11-26」 */
fun cycleRangeText(cycle: YearMonth, cutoffDay: Int = DEFAULT_CUTOFF_DAY): String {
    val cutoff = cutoffDay.coerceIn(1, 31)
    val previous = cycle.minusMonths(1)
    val startDay = minOf(cutoff, previous.lengthOfMonth()) + 1
    val endDay = minOf(cutoff, cycle.lengthOfMonth())
    val start = if (startDay > previous.lengthOfMonth()) {
        // 结算日刚好是月末，起始日落到本月 1 号
        "${cycle.monthValue}-01"
    } else {
        "${previous.monthValue}-${startDay.toString().padStart(2, '0')}"
    }
    return "$start ~ ${cycle.monthValue}-${endDay.toString().padStart(2, '0')}"
}
