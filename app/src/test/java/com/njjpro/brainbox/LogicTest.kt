package com.njjpro.brainbox

import com.njjpro.brainbox.data.Note
import com.njjpro.brainbox.data.REPEAT_DAILY
import com.njjpro.brainbox.data.REPEAT_MONTHLY
import com.njjpro.brainbox.data.REPEAT_NONE
import com.njjpro.brainbox.data.REPEAT_WEEKLY
import com.njjpro.brainbox.data.centsToText
import com.njjpro.brainbox.data.parseAmountToCents
import com.njjpro.brainbox.notify.ReminderScheduler
import com.njjpro.brainbox.update.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** 纯逻辑单测：金额解析、版本比较、提醒时间计算。不依赖 Android 运行时。 */
class LogicTest {

    // ---------- 金额 ----------

    @Test
    fun parseAmount_handlesCommonInput() {
        assertEquals(1230L, parseAmountToCents("12.30"))
        assertEquals(1230L, parseAmountToCents("12.3"))
        assertEquals(123456L, parseAmountToCents(" ¥1,234.56 "))
        assertEquals(0L, parseAmountToCents("0"))
        assertEquals(100L, parseAmountToCents("1"))
    }

    @Test
    fun parseAmount_rejectsInvalidInput() {
        assertNull(parseAmountToCents(""))
        assertNull(parseAmountToCents("   "))
        assertNull(parseAmountToCents("abc"))
        assertNull(parseAmountToCents("-5"))
        assertNull(parseAmountToCents("12.3.4"))
    }

    @Test
    fun parseAmount_roundsToTwoDecimals() {
        assertEquals(1234L, parseAmountToCents("12.344"))
        assertEquals(1235L, parseAmountToCents("12.345"))
    }

    @Test
    fun centsToText_keepsTwoDecimals() {
        assertEquals("0.00", centsToText(0))
        assertEquals("12.30", centsToText(1230))
        assertEquals("1079.09", centsToText(107909))
    }

    // ---------- 版本号 ----------

    @Test
    fun compareVersions_ordering() {
        assertTrue(UpdateChecker.compareVersions("1.0.1", "1.0.0") > 0)
        assertTrue(UpdateChecker.compareVersions("2.0.0", "2.0.1") < 0)
        assertTrue(UpdateChecker.compareVersions("1.10.0", "1.9.0") > 0)
    }

    @Test
    fun compareVersions_equalForms() {
        assertEquals(0, UpdateChecker.compareVersions("v1.0.0", "1.0.0"))
        assertEquals(0, UpdateChecker.compareVersions("V1.0", "1.0.0"))
    }

    @Test
    fun sizeText_formats() {
        assertEquals("未知大小", UpdateChecker.sizeText(0))
        assertEquals("2 KB", UpdateChecker.sizeText(2048))
        assertEquals("10.3 MB", UpdateChecker.sizeText(10_790_946))
    }

    // ---------- 提醒时间 ----------

    private val zone: ZoneId = ZoneId.systemDefault()

    private fun millisOf(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    private fun zoned(millis: Long): ZonedDateTime = Instant.ofEpochMilli(millis).atZone(zone)

    @Test
    fun nextOccurrence_perRule() {
        val base = millisOf(2026, 3, 15, 9, 30)
        assertEquals(
            zoned(base).plusDays(1).toInstant().toEpochMilli(),
            ReminderScheduler.nextOccurrence(base, REPEAT_DAILY),
        )
        assertEquals(
            zoned(base).plusWeeks(1).toInstant().toEpochMilli(),
            ReminderScheduler.nextOccurrence(base, REPEAT_WEEKLY),
        )
        assertEquals(
            zoned(base).plusMonths(1).toInstant().toEpochMilli(),
            ReminderScheduler.nextOccurrence(base, REPEAT_MONTHLY),
        )
        // 未知规则原样返回
        assertEquals(base, ReminderScheduler.nextOccurrence(base, REPEAT_NONE))
    }

    @Test
    fun nextOccurrence_monthlyClampsShortMonth() {
        val jan31 = millisOf(2026, 1, 31, 8, 0)
        val next = zoned(ReminderScheduler.nextOccurrence(jan31, REPEAT_MONTHLY))
        assertEquals(2026, next.year)
        assertEquals(2, next.monthValue)
        assertEquals(28, next.dayOfMonth)
        assertEquals(8, next.hour)
    }

    @Test
    fun rollForward_movesPastDailyReminderToFuture() {
        val now = System.currentTimeMillis()
        val past = now - TimeUnit.DAYS.toMillis(3)
        val note = Note(reminderAt = past, repeatRule = REPEAT_DAILY, reminderActive = true)

        val next = ReminderScheduler.rollForward(note).reminderAt!!

        assertTrue("应当推进到未来", next > now)
        assertTrue("不应推进过头", next < now + TimeUnit.DAYS.toMillis(2))
        assertEquals(zoned(past).toLocalTime(), zoned(next).toLocalTime())
    }

    @Test
    fun rollForward_leavesOthersUntouched() {
        val now = System.currentTimeMillis()

        val oneShot = Note(reminderAt = now - TimeUnit.HOURS.toMillis(1), repeatRule = REPEAT_NONE)
        assertEquals(oneShot, ReminderScheduler.rollForward(oneShot))

        val future = now + TimeUnit.HOURS.toMillis(5)
        val upcoming = Note(reminderAt = future, repeatRule = REPEAT_DAILY)
        assertEquals(upcoming, ReminderScheduler.rollForward(upcoming))

        val noReminder = Note(reminderAt = null, repeatRule = REPEAT_DAILY)
        assertEquals(noReminder, ReminderScheduler.rollForward(noReminder))
    }

    // ---------- 标题回退 ----------

    @Test
    fun displayTitle_fallsBackToContent() {
        assertEquals("标题", Note(title = "标题", content = "正文").displayTitle)
        assertEquals("买菜", Note(title = "  ", content = "\n  买菜\n第二行").displayTitle)
        assertEquals("（空记录）", Note(title = "", content = "").displayTitle)
        assertEquals("一二三四五六七八九十一二三四五六七八九十一二三四", Note(content = "一二三四五六七八九十一二三四五六七八九十一二三四五六七八").displayTitle)
    }
}
