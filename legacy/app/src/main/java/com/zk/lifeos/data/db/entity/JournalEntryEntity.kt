package com.zk.lifeos.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 每日复盘 —— one entry per day, matching the four prompts in the spec. Bodies are Markdown
 * text; rendering is a UI concern, the data layer just stores the strings.
 */
@Entity(
    tableName = "journal_entries",
    indices = [Index(value = ["date"], unique = true)],
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Epoch day. Unique — one entry per day. */
    val date: Int,

    /** 今天完成了什么 */
    val done: String = "",

    /** 今天最大的收获 */
    val win: String = "",

    /** 今天遇到的问题 */
    val problems: String = "",

    /** 明天最重要的一件事 */
    val tomorrowMit: String = "",

    val createdAt: Long,
    val updatedAt: Long,
)
