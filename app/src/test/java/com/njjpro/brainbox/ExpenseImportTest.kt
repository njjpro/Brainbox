package com.njjpro.brainbox

import com.njjpro.brainbox.data.ExpenseImport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ExpenseImportTest {

    private val sample = """
        2026年11月 报销明细
        11-01 周日  [打车]  补上个月的  ¥10.00
        11-01 周日  [餐食]  数据传输  ¥25.00
        11-05 周四  [打车]  买直流可调电源  ¥611.81
        11-05 周四  [打车]  学校-公司  ¥25.29
        11-05 周四  [打车]  公司-学校  ¥19.14
        11-05 周四  [打车]  亚热带热带会议注册费  ¥1000.00
        11-07 周六  [打车]  买天线的转接线  ¥18.00
        合计 7 笔  ¥1709.24
        其中  打车 ¥1684.24  ·  餐食 ¥25.00
    """.trimIndent()

    private fun millis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun parsesSampleMonth() {
        val result = ExpenseImport.parse(sample, LocalDate.of(2026, 11, 19))

        assertEquals(7, result.rows.size)
        assertTrue(result.errors.toString(), result.errors.isEmpty())
        assertEquals(170924L, result.totalCents)

        val byItem = result.rows.associateBy { it.item }
        assertEquals(setOf("补上个月的", "数据传输", "买直流可调电源", "学校-公司", "公司-学校", "亚热带热带会议注册费", "买天线的转接线"), byItem.keys)

        // 事项关键词优先于文本里标的 [打车]：电源和转接线算耗材，注册费算会议
        assertEquals("交通", byItem.getValue("补上个月的").category)
        assertEquals("餐饮", byItem.getValue("数据传输").category)
        assertEquals("耗材", byItem.getValue("买直流可调电源").category)
        assertEquals("交通", byItem.getValue("学校-公司").category)
        assertEquals("交通", byItem.getValue("公司-学校").category)
        assertEquals("会议", byItem.getValue("亚热带热带会议注册费").category)
        assertEquals("耗材", byItem.getValue("买天线的转接线").category)

        assertEquals(1000L, byItem.getValue("补上个月的").amountCents)
        assertEquals(61181L, byItem.getValue("买直流可调电源").amountCents)
        assertEquals(100000L, byItem.getValue("亚热带热带会议注册费").amountCents)

        assertEquals(millis(2026, 11, 1), byItem.getValue("补上个月的").date)
        assertEquals(millis(2026, 11, 7), byItem.getValue("买天线的转接线").date)
    }

    @Test
    fun skipsHeaderAndTotalLines() {
        val result = ExpenseImport.parse(sample, LocalDate.of(2026, 11, 19))
        assertEquals(7, result.rows.size)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun fallsBackToOtherForUnknownCategory() {
        val text = "11-09 周一  [待定]  某个说不清的开销  ¥88.80"
        val row = ExpenseImport.parse(text, LocalDate.of(2026, 11, 19)).rows.single()
        assertEquals("其他", row.category)
        assertEquals(8880L, row.amountCents)
        assertEquals("某个说不清的开销", row.item)
    }

    @Test
    fun usesRememberedTagWhenNoKeywordMatched() {
        val text = "11-09 周一  [餐饮]  和师兄  ¥30"
        val row = ExpenseImport.parse(text, LocalDate.of(2026, 11, 19)).rows.single()
        assertEquals("餐饮", row.category)
        assertEquals(3000L, row.amountCents)
    }

    @Test
    fun acceptsChineseDateAndFullWidthBrackets() {
        val text = "3月8日 星期日 【住宿】住酒店 ￥680.5"
        val row = ExpenseImport.parse(text, LocalDate.of(2026, 11, 19)).rows.single()
        assertEquals(millis(2026, 3, 8), row.date)
        assertEquals("住宿", row.category)
        assertEquals(68050L, row.amountCents)
        assertEquals("住酒店", row.item)
    }

    @Test
    fun reportsLinesWithUnreadableAmount() {
        val result = ExpenseImport.parse("11-09 周一  [打车]  忘了写金额", LocalDate.of(2026, 11, 19))
        assertTrue(result.rows.isEmpty())
        assertEquals(1, result.errors.size)
    }
}
