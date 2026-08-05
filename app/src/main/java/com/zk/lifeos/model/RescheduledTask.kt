package com.zk.lifeos.model

import java.time.LocalDate

/** One task that a bulk reschedule moved, and the date it had before — enough to undo it. */
data class RescheduledTask(
    val id: Long,
    val previousDueDate: LocalDate?,
)
