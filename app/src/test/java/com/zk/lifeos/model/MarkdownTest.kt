package com.zk.lifeos.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownTest {

    // ---- inline ----

    private fun spans(text: String) = Markdown.inlineSpans(text)

    @Test
    fun `plain text is one unstyled run`() {
        assertEquals(listOf(MdSpan("今天写完了周报")), spans("今天写完了周报"))
    }

    @Test
    fun `bold italic and code`() {
        assertEquals(
            listOf(MdSpan("很"), MdSpan("重要", bold = true)),
            spans("很**重要**"),
        )
        assertEquals(listOf(MdSpan("斜", italic = true)), spans("*斜*"))
        assertEquals(listOf(MdSpan("斜", italic = true)), spans("_斜_"))
        assertEquals(listOf(MdSpan("val x", code = true)), spans("`val x`"))
    }

    @Test
    fun `bold can contain italic`() {
        assertEquals(
            listOf(
                MdSpan("粗 ", bold = true),
                MdSpan("斜", bold = true, italic = true),
                MdSpan(" 体", bold = true),
            ),
            spans("**粗 *斜* 体**"),
        )
    }

    @Test
    fun `three asterisks in a row read literally, on purpose`() {
        // 「**粗*斜***」 is ambiguous — where the bold ends depends on flanking rules this parser
        // deliberately doesn't implement. It picks the first closing pair and leaves the leftover
        // asterisk visible, which is the safe direction: you can see something is off and fix it,
        // rather than having a character silently disappear. Pinned so a future change is a choice.
        assertEquals(
            listOf(MdSpan("粗*斜", bold = true), MdSpan("*")),
            spans("**粗*斜***"),
        )
    }

    @Test
    fun `an unpaired marker stays a literal character`() {
        // 「3 * 4」 must not italicise the rest of the line, and a lone ** must not eat anything.
        assertEquals(listOf(MdSpan("3 * 4 = 12")), spans("3 * 4 = 12"))
        assertEquals(listOf(MdSpan("未闭合 **粗")), spans("未闭合 **粗"))
        assertEquals(listOf(MdSpan("a ` b")), spans("a ` b"))
    }

    @Test
    fun `an empty marker pair is left alone rather than swallowed`() {
        assertEquals(listOf(MdSpan("**")), spans("**"))
    }

    @Test
    fun `markers inside code are literal`() {
        assertEquals(listOf(MdSpan("a**b**c", code = true)), spans("`a**b**c`"))
    }

    // ---- blocks ----

    @Test
    fun `consecutive bullets are one list`() {
        assertEquals(
            listOf(MdBlock.Bullets(listOf("写周报", "阅读", "跑步"))),
            Markdown.parse("- 写周报\n- 阅读\n- 跑步"),
        )
    }

    @Test
    fun `a blank line separates blocks`() {
        assertEquals(
            listOf(MdBlock.Paragraph("开头"), MdBlock.Bullets(listOf("一件事"))),
            Markdown.parse("开头\n\n- 一件事"),
        )
    }

    @Test
    fun `headings quotes and numbered lists`() {
        assertEquals(listOf(MdBlock.Heading(2, "小结")), Markdown.parse("## 小结"))
        assertEquals(listOf(MdBlock.Quote("引用的话")), Markdown.parse("> 引用的话"))
        assertEquals(
            listOf(MdBlock.Numbered(listOf("先做这个", "再做那个"))),
            Markdown.parse("1. 先做这个\n2. 再做那个"),
        )
    }

    @Test
    fun `a dash that is not a bullet stays paragraph text`() {
        // 「-30 度」 and a bare 「-」 are text, not list markers.
        assertEquals(listOf(MdBlock.Paragraph("-30 度")), Markdown.parse("-30 度"))
        assertEquals(listOf(MdBlock.Paragraph("-")), Markdown.parse("-"))
    }

    @Test
    fun `a line break inside a paragraph is kept`() {
        // The user pressed enter on purpose; this is a journal, not prose reflow.
        assertEquals(listOf(MdBlock.Paragraph("第一行\n第二行")), Markdown.parse("第一行\n第二行"))
    }

    @Test
    fun `switching list type starts a new block`() {
        assertEquals(
            listOf(MdBlock.Bullets(listOf("a")), MdBlock.Numbered(listOf("b"))),
            Markdown.parse("- a\n1. b"),
        )
    }

    @Test
    fun `empty source parses to nothing`() {
        assertEquals(emptyList<MdBlock>(), Markdown.parse(""))
        assertEquals(emptyList<MdBlock>(), Markdown.parse("\n\n  \n"))
    }

    // ---- plain text ----

    @Test
    fun `plain text drops every marker`() {
        assertEquals(
            "写周报  阅读",
            Markdown.toPlainText("- 写周报\n- 阅读"),
        )
        assertEquals("很重要的一件事", Markdown.toPlainText("很**重要**的一件事"))
        assertEquals("小结", Markdown.toPlainText("## 小结"))
    }

    @Test
    fun `plain text is a single line`() {
        val out = Markdown.toPlainText("第一行\n第二行\n\n- 列表")
        assertEquals(false, out.contains("\n"))
    }
}
