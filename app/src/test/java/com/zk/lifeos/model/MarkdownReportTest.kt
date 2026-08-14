package com.zk.lifeos.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The exported file is the copy meant to outlive the app, so what it does and doesn't contain is
 * worth pinning down rather than eyeballing once on a device.
 */
class MarkdownReportTest {

    private val labels = ExportLabels(
        documentTitle = "LifeOS · 测试",
        generatedAt = "导出于 2026-08-14",
        dateHeading = { it.toString() },
        done = "今天完成了什么",
        win = "今天最大的收获",
        problems = "今天遇到的问题",
        tomorrow = "明天最重要的一件事",
        ticked = "这天打过勾的",
    )

    private fun day(
        date: String,
        entry: JournalEntry? = null,
        tasks: List<String> = emptyList(),
        habits: List<String> = emptyList(),
    ) = ExportDay(LocalDate.parse(date), entry, tasks, habits)

    @Test
    fun `blank prompts leave no empty heading behind`() {
        val out = MarkdownReport.render(
            listOf(day("2026-08-14", JournalEntry(LocalDate.parse("2026-08-14"), win = "想通了一件事"))),
            labels,
        )
        assertTrue(out.contains("**今天最大的收获**"))
        assertFalse(out.contains("**今天完成了什么**"))
        assertFalse(out.contains("**今天遇到的问题**"))
    }

    @Test
    fun `what the review already says is not repeated under 已完成`() {
        val entry = JournalEntry(LocalDate.parse("2026-08-14"), done = "- 写周报\n- 阅读")
        val out = MarkdownReport.render(
            listOf(day("2026-08-14", entry, tasks = listOf("写周报"), habits = listOf("阅读", "跑步"))),
            labels,
        )
        // Only the one it doesn't mention.
        assertTrue(out.contains("### 这天打过勾的"))
        assertEquals(1, out.split("跑步").size - 1)
        assertEquals(1, out.split("写周报").size - 1)
        assertEquals(1, out.split("阅读").size - 1)
    }

    @Test
    fun `a mention anywhere in the review counts, not just the done field`() {
        val entry = JournalEntry(LocalDate.parse("2026-08-14"), problems = "跑步跑岔气了")
        val out = MarkdownReport.render(listOf(day("2026-08-14", entry, habits = listOf("跑步"))), labels)
        // The line is 「跑步跑岔气了」, not 「跑步」 — a different sentence, so the habit still lists.
        assertTrue(out.contains("### 这天打过勾的"))
    }

    @Test
    fun `a day with only completions still gets a heading`() {
        val out = MarkdownReport.render(listOf(day("2026-08-14", tasks = listOf("修 bug"))), labels)
        assertTrue(out.contains("## 2026-08-14"))
        assertTrue(out.contains("- 修 bug"))
    }

    @Test
    fun `a day whose completions are all already written produces no 已完成 section`() {
        val entry = JournalEntry(LocalDate.parse("2026-08-14"), done = "- 修 bug")
        val out = MarkdownReport.render(listOf(day("2026-08-14", entry, tasks = listOf("修 bug"))), labels)
        assertFalse(out.contains("### 这天打过勾的"))
    }

    @Test
    fun `days come out in the order they were given, oldest first`() {
        val out = MarkdownReport.render(
            listOf(day("2026-08-12", tasks = listOf("a")), day("2026-08-14", tasks = listOf("b"))),
            labels,
        )
        // Matched on the heading, not the bare date: the generated-at line at the top of the
        // document also contains a date, and an unanchored indexOf finds that one first.
        assertTrue(out.indexOf("## 2026-08-12") < out.indexOf("## 2026-08-14"))
    }

    @Test
    fun `the document opens with its title and when it was made`() {
        val out = MarkdownReport.render(emptyList(), labels)
        assertEquals("# LifeOS · 测试", out.lineSequence().first())
        assertTrue(out.contains("*导出于 2026-08-14*"))
    }
}
