package com.zk.lifeos.model

/**
 * Turns a period of the workbench into Markdown. Pure — no Android, no resources, no clock.
 *
 * Format choices, all pointed at「十年后还能读」:
 * - Headings and bullets only. No tables, no HTML, no front-matter. It should read as plain text
 *   even in an editor that has never heard of Markdown.
 * - Days run oldest → newest, which is how you read a period back. (The app's *screens* show newest
 *   first, because there you are looking for something recent.)
 * - Empty prompts are skipped rather than printed with nothing under them.
 */
object MarkdownReport {

    fun render(days: List<ExportDay>, labels: ExportLabels): String = buildString {
        appendLine("# ${labels.documentTitle}")
        appendLine()
        appendLine("*${labels.generatedAt}*")

        days.forEach { day ->
            appendLine()
            appendLine("## ${labels.dateHeading(day.date)}")

            day.entry?.let { entry ->
                section(labels.done, entry.done)
                section(labels.win, entry.win)
                section(labels.problems, entry.problems)
                section(labels.tomorrow, entry.tomorrowMit)
            }

            // Only what the review doesn't already say. Someone who used 「带出已经打过勾的」 has these
            // lines in their own words already, and repeating them under a second heading would make
            // the document look padded.
            val alreadyWritten = day.entry?.mentionedLines().orEmpty()
            val extra = (day.taskTitles + day.habitNames).filter { it.trim() !in alreadyWritten }
            if (extra.isNotEmpty()) {
                appendLine()
                appendLine("### ${labels.ticked}")
                extra.forEach { appendLine("- $it") }
            }
        }
    }

    private fun StringBuilder.section(heading: String, body: String) {
        if (body.isBlank()) return
        appendLine()
        appendLine("**$heading**")
        appendLine()
        appendLine(body.trimEnd())
    }
}

/**
 * Every non-empty line of a review, stripped of its bullet — used to avoid repeating a task under
 * 「已完成」when the user already wrote it out.
 */
private fun JournalEntry.mentionedLines(): Set<String> =
    listOf(done, win, problems, tomorrowMit)
        .flatMap { it.lines() }
        .map { it.trim().removePrefix("- ").trim() }
        .filter { it.isNotEmpty() }
        .toSet()
