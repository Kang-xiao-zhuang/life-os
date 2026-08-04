package com.zk.lifeos.data.repository

import com.zk.lifeos.data.db.dao.ProjectWithCounts
import com.zk.lifeos.data.db.entity.CaptureEntity
import com.zk.lifeos.data.db.entity.JournalEntryEntity
import com.zk.lifeos.data.db.entity.TaskEntity
import com.zk.lifeos.model.CaptureItem
import com.zk.lifeos.model.JournalEntry
import com.zk.lifeos.model.ProjectSummary
import com.zk.lifeos.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Entity ↔ model mapping, kept in one place so the storage representation (epoch days,
 * epoch millis) never leaks past the repository layer.
 */

internal fun Int.toLocalDate(): LocalDate = LocalDate.ofEpochDay(toLong())

internal fun LocalDate.toEpochDayInt(): Int = toEpochDay().toInt()

/** Local midnight as epoch millis — used to ask "was this finished today?". */
internal fun LocalDate.startOfDayMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

internal fun Long.toLocalDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

internal fun TaskEntity.toModel(): Task = Task(
    id = id,
    title = title,
    notes = notes,
    projectId = projectId,
    done = done,
    dueDate = dueDate?.toLocalDate(),
    isMit = isMit,
)

internal fun ProjectWithCounts.toModel(): ProjectSummary = ProjectSummary(
    id = project.id,
    name = project.name,
    emoji = project.emoji,
    openTasks = openTasks,
    doneTasks = doneTasks,
)

internal fun JournalEntryEntity.toModel(): JournalEntry = JournalEntry(
    date = date.toLocalDate(),
    done = done,
    win = win,
    problems = problems,
    tomorrowMit = tomorrowMit,
)

internal fun CaptureEntity.toModel(): CaptureItem = CaptureItem(
    id = id,
    text = text,
    createdAt = createdAt.toLocalDateTime(),
)
