package com.zk.lifeos.data.db.dao

import androidx.room.Embedded
import com.zk.lifeos.data.db.entity.HabitEntity

/** Query result: a habit plus how many check-ins it has recorded. */
data class HabitWithCheckCount(
    @Embedded val habit: HabitEntity,
    val checkCount: Int,
)
