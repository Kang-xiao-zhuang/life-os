package com.zk.lifeos.ui.theme

import androidx.compose.ui.unit.dp

/**
 * The vertical rhythm, as a scale where **the size of a gap says how related two things are**.
 *
 * Before this existed the app used ten different gap values picked one at a time, and the two that
 * mattered most had landed almost on top of each other: **14dp between cards, 12dp inside a card**.
 * A boundary between two separate sections looked the same weight as a boundary inside one section,
 * so nothing told the eye where a block began or ended. Every screen read as one undifferentiated
 * column — which is what「单调」actually is, mechanically.
 *
 * The rule now: each step is roughly 1.5× the one below it, and you pick by relationship, not by
 * eye. Two things a step apart look related; two things two steps apart look separate.
 */
object Space {

    /** Lines of one thing — a task and its note, a date and its weight. */
    val row = 4.dp

    /** A tight group: rows inside a quiet card, chips on one line. */
    val tight = 8.dp

    /** Inside a card — its title to its content. */
    val inner = 14.dp

    /** Between cards. Deliberately well clear of [inner]: this is the boundary that was missing. */
    val block = 22.dp

    /** Between parts of a screen that are about different things. */
    val section = 32.dp
}
