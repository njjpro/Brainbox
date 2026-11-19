package com.njjpro.brainbox

import com.njjpro.brainbox.data.cycleOf
import com.njjpro.brainbox.data.cycleRange
import com.njjpro.brainbox.data.cycleRangeText
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class ExpenseCycleTest {

    private fun millis(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        LocalDate.of(year, month, day)
            .atTime(hour, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    @Test
    fun cutoff26GroupsByCycleNotCalendarMonth() {
        val cutoff = 26
        assertEquals(YearMonth.of(2026, 11), cycleOf(millis(2026, 10, 27), cutoff))
        assertEquals(YearMonth.of(2026, 10), cycleOf(millis(2026, 10, 26), cutoff))
        assertEquals(YearMonth.of(2026, 11), cycleOf(millis(2026, 11, 1), cutoff))
        assertEquals(YearMonth.of(2026, 11), cycleOf(millis(2026, 11, 26, hour = 23), cutoff))
        assertEquals(YearMonth.of(2026, 12), cycleOf(millis(2026, 11, 27), cutoff))
    }

    @Test
    fun rangeCoversPreviousCutoffToThisCutoff() {
        val (start, end) = cycleRange(YearMonth.of(2026, 11), 26)
        assertEquals(millis(2026, 10, 27, hour = 0), start)
        assertEquals(millis(2026, 11, 27, hour = 0), end)
        // 半开区间：10-27 属于本期，11-27 不属于
        assertEquals(YearMonth.of(2026, 11), cycleOf(start, 26))
        assertEquals(YearMonth.of(2026, 12), cycleOf(end, 26))
    }

    @Test
    fun rangeTextIsReadable() {
        assertEquals("10-27 ~ 11-26", cycleRangeText(YearMonth.of(2026, 11), 26))
        assertEquals("2-22 ~ 3-21", cycleRangeText(YearMonth.of(2026, 3), 21))
    }

    @Test
    fun cutoff31ClampsToShortMonths() {
        val (start, end) = cycleRange(YearMonth.of(2027, 3), 31)
        assertEquals(millis(2027, 3, 1, hour = 0), start)
        assertEquals(millis(2027, 4, 1, hour = 0), end)
        assertEquals("3-01 ~ 3-31", cycleRangeText(YearMonth.of(2027, 3), 31))
    }

    @Test
    fun cutoffIsClampedIntoValidRange() {
        val (start, end) = cycleRange(YearMonth.of(2026, 11), 99)
        assertEquals(millis(2026, 11, 1, hour = 0), start)
        assertEquals(millis(2026, 12, 1, hour = 0), end)
    }
}
