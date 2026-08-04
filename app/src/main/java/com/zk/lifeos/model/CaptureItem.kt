package com.zk.lifeos.model

import java.time.LocalDateTime

/** One line from the inbox — no structure by design. */
data class CaptureItem(
    val id: Long,
    val text: String,
    val createdAt: LocalDateTime,
)
