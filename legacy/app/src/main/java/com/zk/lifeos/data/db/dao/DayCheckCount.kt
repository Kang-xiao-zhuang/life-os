package com.zk.lifeos.data.db.dao

/** Query result: how many habits were checked on one day (epoch day). */
data class DayCheckCount(
    val date: Int,
    val count: Int,
)
