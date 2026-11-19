package com.zk.lifeos.model

/** How much a backup contains — shown after an export or import so the number is verifiable. */
data class BackupCounts(
    val projects: Int = 0,
    val tasks: Int = 0,
    val habits: Int = 0,
    val habitChecks: Int = 0,
    val captures: Int = 0,
    val journalEntries: Int = 0,
) {
    val total: Int get() = projects + tasks + habits + habitChecks + captures + journalEntries
}

/**
 * Why a backup failed, as a *type* rather than a message.
 *
 * The data and service layers must not build user-facing text: they have no access to string
 * resources, so any message they wrote would be stuck in one language. The UI turns these into
 * words.
 */
sealed interface BackupFailure {
    /** The chosen destination could not be opened for writing. */
    data object CannotWrite : BackupFailure

    /** The chosen file could not be opened for reading. */
    data object CannotRead : BackupFailure

    /** No `database.db` inside — almost certainly not a LifeOS backup. */
    data object NotABackup : BackupFailure

    /** The archive was written by a version with a different schema. */
    data class SchemaMismatch(val backupVersion: Int, val appVersion: Int) : BackupFailure

    /** Anything else; [message] is a technical detail, not a translated sentence. */
    data class Unexpected(val message: String?) : BackupFailure
}

/** Thrown inside the backup code so the service can map it to a [BackupFailure] without parsing text. */
class BackupException(val failure: BackupFailure) : Exception(failure.toString())

/** Outcome of an export or import. */
sealed interface BackupResult {
    data class Success(val counts: BackupCounts) : BackupResult
    data class Failure(val failure: BackupFailure) : BackupResult
}
