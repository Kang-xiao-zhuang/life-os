package com.zk.lifeos.data.db.dao

/** Query result: which habit was checked on which day (epoch day), for grouping on export. */
data class DayHabitName(
    val date: Int,
    val name: String,
)
