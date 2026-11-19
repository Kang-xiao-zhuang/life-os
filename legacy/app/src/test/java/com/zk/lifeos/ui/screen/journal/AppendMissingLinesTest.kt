package com.zk.lifeos.ui.screen.journal

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 「带出已经打过勾的 N 项」 writes into a box the user may already have typed in, so the rule it has to
 * keep is narrow and absolute: add what is missing, touch nothing else.
 */
class AppendMissingLinesTest {

    @Test
    fun `an empty field just gets the bullets`() {
        assertEquals(
            "- 写周报\n- 阅读",
            appendMissingLines("", listOf("写周报", "阅读")),
        )
    }

    @Test
    fun `existing text is kept and the new lines go under it`() {
        assertEquals(
            "今天状态不错\n- 写周报",
            appendMissingLines("今天状态不错", listOf("写周报")),
        )
    }

    @Test
    fun `pressing twice adds nothing the second time`() {
        val once = appendMissingLines("", listOf("写周报", "阅读"))
        assertEquals(once, appendMissingLines(once, listOf("写周报", "阅读")))
    }

    @Test
    fun `something typed by hand blocks its generated twin`() {
        // Written without a bullet, but it is the same thing — adding it again would read as if it
        // had been done twice.
        assertEquals(
            "跑步\n- 写周报",
            appendMissingLines("跑步", listOf("跑步", "写周报")),
        )
    }

    @Test
    fun `only the missing ones are added, in order`() {
        assertEquals(
            "- 阅读\n- 写周报\n- 跑步",
            appendMissingLines("- 阅读", listOf("写周报", "阅读", "跑步")),
        )
    }

    @Test
    fun `a duplicate inside the incoming list is added once`() {
        assertEquals("- 写周报", appendMissingLines("", listOf("写周报", "写周报")))
    }

    @Test
    fun `nothing to add leaves the text byte for byte alone`() {
        val existing = "- 写周报\n\n随手记了点别的"
        assertEquals(existing, appendMissingLines(existing, listOf("写周报")))
    }

    @Test
    fun `a trailing newline is not doubled`() {
        assertEquals("写了点东西\n- 写周报", appendMissingLines("写了点东西\n", listOf("写周报")))
    }

    @Test
    fun `differing by a word means two different things`() {
        // Deliberately exact: guessing at near-matches would silently drop something really done.
        assertEquals(
            "- 写周报\n- 写周报初稿",
            appendMissingLines("- 写周报", listOf("写周报初稿")),
        )
    }
}
