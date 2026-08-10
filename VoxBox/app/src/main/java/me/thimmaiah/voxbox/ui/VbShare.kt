package me.thimmaiah.voxbox.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Hands a file to the system share sheet.
 *
 * The export path built the zip correctly and recorded where it was, but nothing ever turned that
 * into an intent — so "export" looked like it did nothing at all. A produced file the user cannot
 * reach is the same as no feature.
 */
fun shareFile(context: Context, file: File, mimeType: String, title: String): Boolean {
    if (!file.isFile) return false
    val uri = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull() ?: return false

    val send = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, title).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching { context.startActivity(chooser) }.isSuccess
}
