package com.zk.lifeos.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * What a Markdown export covers.
 *
 * The backup zip holds a SQLite file that only this build of this app can open. That is the right
 * format for restoring, and the wrong one for *reading* — the value of a year of reviews is being
 * able to open them in ten years, on any machine, with anything. Hence a second export that is
 * plain text and deliberately lossy.
 */
sealed interface ExportRange {

    val from: LocalDate
    val to: LocalDate

    /** One calendar month — the unit a review period is actually reviewed in. */
    data class Month(val month: YearMonth) : ExportRange {
        override val from: LocalDate get() = month.atDay(1)
        override val to: LocalDate get() = month.atEndOfMonth()
    }

    /** Everything, for archiving or moving to another tool. */
    data class All(override val from: LocalDate, override val to: LocalDate) : ExportRange
}

/**
 * One day as the export sees it: what was written, and what was ticked off.
 *
 * Days where both are empty never make it this far — an export full of empty headings would bury
 * the days that do say something.
 */
data class ExportDay(
    val date: LocalDate,
    val entry: JournalEntry?,
    val taskTitles: List<String> = emptyList(),
    val habitNames: List<String> = emptyList(),
)

/**
 * Every fixed word in the exported document, supplied by the UI.
 *
 * The service layer holds no resources — the same rule that makes [BackupFailure] a type the UI puts
 * into words. It also means the document comes out in the language the app is set to at the moment
 * of export, which is what someone exporting expects.
 */
data class ExportLabels(
    /** The document's `# ` heading, range already worded in — e.g.「LifeOS · 2026 年 8 月」. */
    val documentTitle: String,
    /** One line under it saying when the file was made. */
    val generatedAt: String,
    /** A day's `## ` heading — the UI knows how this app writes dates and weekdays. */
    val dateHeading: (LocalDate) -> String,
    val done: String,
    val win: String,
    val problems: String,
    val tomorrow: String,
    /** Heading for the list of things ticked off that day. */
    val ticked: String,
)
