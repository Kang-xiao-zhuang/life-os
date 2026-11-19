package com.zk.lifeos.model

/**
 * A retired habit, with the amount of history it is holding.
 *
 * [checkCount] exists so 彻底删除 can say what it actually destroys — "删除" on its own hides the
 * fact that a year of check-ins goes with it.
 */
data class ArchivedHabit(
    val id: Long,
    val name: String,
    val emoji: String,
    val checkCount: Int,
)
