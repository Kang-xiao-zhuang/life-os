package com.zk.lifeos.model

/**
 * A deliberately small Markdown reader for text this app wrote down itself.
 *
 * Compose's `Text` renders no Markdown at all — `**粗体**` shows the asterisks — and the reviews have
 * always been stored verbatim, so the markers were sitting there in plain sight. This is the missing
 * display half, hand-written rather than pulled in as a dependency: the whole grammar below is what
 * a personal journal actually uses, and a full CommonMark implementation would be a large library
 * kept around for four of its features.
 *
 * **Supported:** `#`/`##`/`###` headings, `-`/`*`/`+` bullets, `1.` numbered lists, `>` quotes,
 * paragraphs, and inline `**bold**`, `*italic*`/`_italic_`, `` `code` `` (nested one inside another).
 *
 * **Deliberately not supported**, and each for a reason:
 * - **Links.** The app declares no `INTERNET` permission, so there is nothing to open.
 * - **Images.** No image feature anywhere in the app.
 * - **Tables, footnotes, raw HTML.** Nobody types these into a phone at 22:00.
 *
 * Anything unsupported is left **exactly as typed** rather than swallowed. A journal must never lose
 * a character it was given, so the failure mode is "you see the marker", not "your line vanished".
 * The same rule settles the ambiguous cases: `**粗*斜***` picks the first closing pair and leaves the
 * spare asterisk on screen, because a visible oddity you can fix beats a character that vanished.
 */
object Markdown {

    fun parse(source: String): List<MdBlock> {
        val blocks = mutableListOf<MdBlock>()
        val paragraph = mutableListOf<String>()
        val bullets = mutableListOf<String>()
        val numbered = mutableListOf<String>()
        val quote = mutableListOf<String>()

        // Each run of like lines is one block, so a list stays a list instead of becoming five
        // separate one-item lists.
        fun flush() {
            if (paragraph.isNotEmpty()) { blocks += MdBlock.Paragraph(paragraph.joinToString("\n")); paragraph.clear() }
            if (bullets.isNotEmpty()) { blocks += MdBlock.Bullets(bullets.toList()); bullets.clear() }
            if (numbered.isNotEmpty()) { blocks += MdBlock.Numbered(numbered.toList()); numbered.clear() }
            if (quote.isNotEmpty()) { blocks += MdBlock.Quote(quote.joinToString("\n")); quote.clear() }
        }

        source.lines().forEach { raw ->
            val line = raw.trimEnd()
            val trimmed = line.trimStart()
            when {
                trimmed.isEmpty() -> flush()

                trimmed.startsWith("### ") -> { flush(); blocks += MdBlock.Heading(3, trimmed.removePrefix("### ")) }
                trimmed.startsWith("## ") -> { flush(); blocks += MdBlock.Heading(2, trimmed.removePrefix("## ")) }
                trimmed.startsWith("# ") -> { flush(); blocks += MdBlock.Heading(1, trimmed.removePrefix("# ")) }

                trimmed.startsWith("> ") -> {
                    if (paragraph.isNotEmpty() || bullets.isNotEmpty() || numbered.isNotEmpty()) flush()
                    quote += trimmed.removePrefix("> ")
                }

                isBullet(trimmed) -> {
                    if (paragraph.isNotEmpty() || numbered.isNotEmpty() || quote.isNotEmpty()) flush()
                    bullets += trimmed.substring(2)
                }

                numberedBody(trimmed) != null -> {
                    if (paragraph.isNotEmpty() || bullets.isNotEmpty() || quote.isNotEmpty()) flush()
                    numbered += numberedBody(trimmed)!!
                }

                else -> {
                    if (bullets.isNotEmpty() || numbered.isNotEmpty() || quote.isNotEmpty()) flush()
                    paragraph += line
                }
            }
        }
        flush()
        return blocks
    }

    /**
     * The same text with every marker removed — for the places that show one squeezed line of a
     * review or a note, where a `-` or a `**` is noise rather than formatting.
     */
    fun toPlainText(source: String): String = parse(source)
        .joinToString("  ") { block ->
            when (block) {
                is MdBlock.Heading -> strip(block.text)
                is MdBlock.Paragraph -> strip(block.text).replace('\n', ' ')
                is MdBlock.Bullets -> block.items.joinToString("  ") { strip(it) }
                is MdBlock.Numbered -> block.items.joinToString("  ") { strip(it) }
                is MdBlock.Quote -> strip(block.text).replace('\n', ' ')
            }
        }
        .trim()

    private fun strip(text: String): String = inlineSpans(text).joinToString("") { it.text }

    /** `- x`, `* x`, `+ x` — but not `-x`, and not a bare `-` on its own. */
    private fun isBullet(line: String): Boolean =
        line.length > 2 && line[0] in "-*+" && line[1] == ' '

    /** `1. x` → `x`. Returns null when the line isn't a numbered item. */
    private fun numberedBody(line: String): String? {
        val dot = line.indexOf(". ")
        if (dot <= 0 || dot > 3) return null
        if (!line.substring(0, dot).all { it.isDigit() }) return null
        return line.substring(dot + 2)
    }

    /**
     * Inline markers, as a flat list of styled runs.
     *
     * A marker only counts when its closing partner is on the same line — otherwise the `*` in
     * 「3 * 4」 would italicise everything after it. Unpaired markers stay as literal characters.
     */
    fun inlineSpans(text: String): List<MdSpan> {
        val out = mutableListOf<MdSpan>()
        val buffer = StringBuilder()
        var i = 0

        fun flush() {
            if (buffer.isNotEmpty()) {
                out += MdSpan(buffer.toString())
                buffer.clear()
            }
        }

        while (i < text.length) {
            val char = text[i]
            val bold = text.startsWith("**", i)
            val close = if (bold) text.indexOf("**", i + 2) else -1

            when {
                bold && close > i + 2 -> {
                    flush()
                    // Recurse so 「**粗 *斜* 体**」 keeps both; the inner text is always shorter.
                    out += inlineSpans(text.substring(i + 2, close)).map { it.copy(bold = true) }
                    i = close + 2
                }

                char == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i + 1) {
                        flush()
                        // Code is literal by definition: no marker inside it means anything.
                        out += MdSpan(text.substring(i + 1, end), code = true)
                        i = end + 1
                    } else {
                        buffer.append(char); i++
                    }
                }

                char == '*' || char == '_' -> {
                    val end = text.indexOf(char, i + 1)
                    if (end > i + 1) {
                        flush()
                        out += inlineSpans(text.substring(i + 1, end)).map { it.copy(italic = true) }
                        i = end + 1
                    } else {
                        buffer.append(char); i++
                    }
                }

                else -> { buffer.append(char); i++ }
            }
        }
        flush()
        return out
    }
}

/** One block of a parsed document. */
sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Bullets(val items: List<String>) : MdBlock
    data class Numbered(val items: List<String>) : MdBlock
    data class Quote(val text: String) : MdBlock
}

/** A run of text with the inline styles that apply to it. */
data class MdSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
)
