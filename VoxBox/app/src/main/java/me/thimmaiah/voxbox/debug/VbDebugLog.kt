package me.thimmaiah.voxbox.debug

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/**
 * TEMPORARY diagnostic log. Remove before any real release — see docs/TEMPORARY_DEBUG_LOG.md.
 *
 * Exists because the failures that matter in this app only appear during a live lecture, on a
 * phone, away from a laptop: a transcript that drops a sentence at a chunk boundary, a note delta
 * the model returned empty, a frame the filter accepted for no visible reason. Reproducing those
 * from a screenshot after the fact has cost more time than the log will.
 *
 * Deliberately conservative about what it records, because a lecture is other people talking:
 *
 *  - Transcribed speech is stored as a character count and a short prefix, never in full.
 *  - No audio, no images, no note bodies.
 *  - Nothing leaves the device unless the student taps share.
 *
 * In-memory ring buffer plus a single file, so a long session cannot fill the disk.
 */
object VbDebugLog {

    private const val MAX_ENTRIES = 2_000
    private const val PREVIEW_CHARS = 60
    private const val FILE_NAME = "voxbox-debug.log"

    private val entries = ArrayDeque<String>(MAX_ENTRIES)
    private val stamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    @Volatile
    var enabled: Boolean = true

    @Synchronized
    fun log(tag: String, message: String) {
        if (!enabled) return
        if (entries.size >= MAX_ENTRIES) entries.removeFirst()
        entries.addLast("${stamp.format(Date())}  [$tag] $message")
    }

    /** Records text volume and a short prefix, never the whole utterance. */
    @Synchronized
    fun logText(tag: String, label: String, text: String) {
        if (!enabled) return
        val preview = text.take(PREVIEW_CHARS).replace('\n', ' ')
        val suffix = if (text.length > PREVIEW_CHARS) "…" else ""
        log(tag, "$label chars=${text.length} preview=\"$preview$suffix\"")
    }

    @Synchronized
    fun snapshot(): String = entries.joinToString("\n")

    @Synchronized
    fun clear() = entries.clear()

    /**
     * Writes the buffer to private storage and returns the file, for the share sheet.
     *
     * Its own subdirectory on purpose: the FileProvider grants access to a directory, not a file,
     * so putting the log beside the note assets would have made every captured diagram crop
     * shareable by anyone holding a content URI.
     */
    @Synchronized
    fun writeTo(context: Context): File {
        val directory = File(context.filesDir, "debug").apply { mkdirs() }
        val file = File(directory, FILE_NAME)
        file.writeText(
            buildString {
                appendLine("VoxBox debug log")
                appendLine("Written ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                appendLine("Entries: ${entries.size}${if (entries.size >= MAX_ENTRIES) " (oldest discarded)" else ""}")
                appendLine("Transcribed speech is summarised, never recorded in full.")
                appendLine("-".repeat(72))
                appendLine(entries.joinToString("\n"))
            },
        )
        return file
    }
}
