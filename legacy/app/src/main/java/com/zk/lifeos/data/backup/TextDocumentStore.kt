package com.zk.lifeos.data.backup

import android.content.Context
import android.net.Uri
import com.zk.lifeos.model.BackupException
import com.zk.lifeos.model.BackupFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Writes a plain-text document to a [Uri] the user picked.
 *
 * Separate from [BackupStore] on purpose: that one owns an archive format with a schema version and
 * a restore path, this one just puts text in a file the user chose. Sharing the [BackupFailure]
 * type is enough — the Settings screen already knows how to word「写不进去」.
 *
 * UTF-8 with no BOM. A BOM would show up as a stray character in editors that don't strip it, and
 * this file exists to be opened by whatever the user has in ten years.
 */
class TextDocumentStore(private val context: Context) {

    suspend fun write(target: Uri, text: String): Unit = withContext(Dispatchers.IO) {
        val output = context.contentResolver.openOutputStream(target)
            ?: throw BackupException(BackupFailure.CannotWrite)
        output.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }
}
