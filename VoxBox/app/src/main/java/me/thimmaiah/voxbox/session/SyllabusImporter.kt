package me.thimmaiah.voxbox.session

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImportedSyllabus(
    val title: String,
    val localRelativePath: String,
    val text: String,
    val sha256: String,
)

class SyllabusImporter(
    private val context: Context,
    private val maxBytes: Int = 750_000,
) {
    suspend fun import(uri: Uri): ImportedSyllabus = withContext(Dispatchers.IO) {
        val displayName = queryDisplayName(uri).ifBlank { "Imported syllabus.md" }
        val extension = displayName.substringAfterLast('.', "").lowercase()
        require(extension in setOf("md", "markdown", "txt")) {
            "Import a UTF-8 Markdown or text syllabus (.md or .txt)."
        }
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ArrayList<Byte>()
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                require(total <= maxBytes) { "The syllabus exceeds ${maxBytes / 1_000} KB." }
                repeat(count) { index -> output.add(buffer[index]) }
            }
            ByteArray(output.size) { output[it] }
        } ?: error("The selected syllabus could not be opened.")
        val text = bytes.toString(Charsets.UTF_8).trim()
        require(text.isNotBlank()) { "The selected syllabus is empty." }
        require('\u0000' !in text) { "The selected file does not look like UTF-8 text." }
        val hash = sha256(bytes)
        val directory = File(context.filesDir, "syllabi").apply {
            if (!exists() && !mkdirs()) error("The local syllabus folder could not be created.")
        }
        val target = File(directory, "$hash.md")
        if (!target.exists()) {
            val temporary = File(directory, ".$hash.tmp")
            try {
                FileOutputStream(temporary).use { output ->
                    output.write(text.toByteArray(Charsets.UTF_8))
                    output.fd.sync()
                }
                if (!temporary.renameTo(target)) error("The local syllabus copy could not be committed.")
            } catch (error: Exception) {
                temporary.delete()
                throw error
            }
        }
        ImportedSyllabus(
            title = displayName.substringBeforeLast('.').trim().ifBlank { "Imported syllabus" },
            localRelativePath = "syllabi/$hash.md",
            text = text,
            sha256 = hash,
        )
    }

    private fun queryDisplayName(uri: Uri): String = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
            }.orEmpty()
    }.getOrDefault("")
}

internal fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes)
    .joinToString("") { byte -> "%02x".format(byte) }
