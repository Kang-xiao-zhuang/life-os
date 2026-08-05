package com.zk.lifeos.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import com.zk.lifeos.R

/**
 * The little counters in card corners («3 项» / «3 items»).
 *
 * They exist as functions rather than inline `pluralStringResource` calls because the same six
 * counters appear on nearly every screen, and English needs singular/plural where Chinese needs
 * a measure word — both live in the resource file, not in the layout.
 */

/** Generic count of rows in a list. */
@Composable
fun itemCount(count: Int): String = pluralStringResource(R.plurals.count_items, count, count)

/** Tasks specifically, where the word "task" carries meaning ("12 个任务" vs. plain "12 项"). */
@Composable
fun taskCount(count: Int): String = pluralStringResource(R.plurals.count_tasks, count, count)

/** Countable things: projects, habits — 个 in Chinese. */
@Composable
fun pieceCount(count: Int): String = pluralStringResource(R.plurals.count_pieces, count, count)

/** Captured notes waiting in the inbox. */
@Composable
fun noteCount(count: Int): String = pluralStringResource(R.plurals.count_notes, count, count)

/** Habit check-ins. */
@Composable
fun checkinCount(count: Int): String = pluralStringResource(R.plurals.count_checkins, count, count)

/** Written journal entries. */
@Composable
fun entryCount(count: Int): String = pluralStringResource(R.plurals.count_entries, count, count)
