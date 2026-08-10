package me.thimmaiah.voxbox.notes

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class NoteExport(
    val markdownFile: File,
    /** Verbatim capture, written only when the note has transcript evidence behind it. */
    val capturedFile: File?,
    val zipFile: File,
    val assetCount: Int,
    val capturedSegmentCount: Int,
)

class MarkdownExporter(
    private val context: Context,
) {
    suspend fun export(
        note: NoteEntity,
        blocks: List<NoteBlockEntity>,
        assets: List<NoteAssetEntity>,
        transcript: List<TranscriptSegmentEntity> = emptyList(),
    ): NoteExport = withContext(Dispatchers.IO) {
        cleanupOldExports()
        val slug = safeExportName(note.title)
        val exportDirectory = File(context.cacheDir, "exports/${slug}-${System.currentTimeMillis()}").apply {
            if (!mkdirs()) error("The export folder could not be created.")
        }
        val assetDirectory = File(exportDirectory, "assets").apply {
            if (!mkdirs()) error("The export asset folder could not be created.")
        }
        val copiedAssets = mutableListOf<Pair<NoteAssetEntity, File>>()
        assets.forEachIndexed { index, asset ->
            val source = resolvePrivateAsset(asset.localRelativePath)
            if (!source.isFile) return@forEachIndexed
            val extension = source.extension.lowercase(Locale.ROOT).takeIf { it in setOf("jpg", "jpeg", "png", "webp") } ?: "jpg"
            val target = File(assetDirectory, "diagram-${index + 1}.$extension")
            source.inputStream().use { input -> target.outputStream().use(input::copyTo) }
            copiedAssets += asset to target
        }
        val replacements = copiedAssets.associate { (asset, target) ->
            asset.localRelativePath.replace('\\', '/') to "assets/${target.name}"
        }
        val markdown = renderNoteMarkdown(note, blocks, assets, replacements)
        val markdownFile = File(exportDirectory, "$slug.md").apply {
            writeText(markdown, Charsets.UTF_8)
        }
        // The refined note and the raw record ship as separate documents. Keeping them apart is
        // the point: the refined file is what the AI wrote, the captured file is what was
        // actually said and shown, and one can always be checked against the other.
        val capturedFile = if (transcript.isEmpty()) null else {
            File(exportDirectory, "$slug-captured.md").apply {
                writeText(renderCapturedMarkdown(note, transcript, copiedAssets.map { it.second.name }), Charsets.UTF_8)
            }
        }
        val zipFile = File(context.cacheDir, "exports/$slug-${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            addZipFile(zip, markdownFile, markdownFile.name)
            capturedFile?.let { addZipFile(zip, it, it.name) }
            copiedAssets.forEach { (_, file) -> addZipFile(zip, file, "assets/${file.name}") }
        }
        NoteExport(
            markdownFile = markdownFile,
            capturedFile = capturedFile,
            zipFile = zipFile,
            assetCount = copiedAssets.size,
            capturedSegmentCount = transcript.size,
        )
    }

    private fun resolvePrivateAsset(relativePath: String): File {
        val normalized = relativePath.replace('\\', '/').trim()
        require(normalized.isNotBlank() && !normalized.startsWith('/') && ':' !in normalized) {
            "Note assets must use private relative paths."
        }
        require(normalized.split('/').none { it == ".." }) { "Note assets cannot leave private storage." }
        return File(context.filesDir, normalized)
    }

    private fun cleanupOldExports() {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60_000L
        File(context.cacheDir, "exports").listFiles()?.filter { it.lastModified() < cutoff }?.forEach { target ->
            if (target.isDirectory) target.deleteRecursively() else target.delete()
        }
    }
}

internal fun renderNoteMarkdown(
    note: NoteEntity,
    blocks: List<NoteBlockEntity>,
    assets: List<NoteAssetEntity>,
    assetPathReplacements: Map<String, String> = emptyMap(),
): String {
    val sections = mutableListOf("# ${note.title.trim().ifBlank { "Untitled note" }}")
    blocks.sortedBy(NoteBlockEntity::position).forEach { block ->
        val rewritten = assetPathReplacements.entries.fold(block.content.trim()) { value, replacement ->
            value.replace(replacement.key, replacement.value)
        }
        val content = replaceUnavailablePrivateAssetLinks(rewritten)
        if (content.isBlank()) return@forEach
        sections += when (block.type) {
            NoteBlockType.HEADING.name -> "## $content"
            NoteBlockType.BULLET_POINT.name -> "- $content"
            NoteBlockType.PIE_CHART.name -> {
                val label = block.label?.trim().orEmpty().ifBlank { "Value" }
                val value = block.chartValue ?: 0
                "**$label:** $value%${block.accentColor?.let { " ($it)" }.orEmpty()}"
            }
            NoteBlockType.MARKDOWN.name -> content
            else -> content
        }
    }
    val current = sections.joinToString("\n\n")
    val missingAssets = assets.mapNotNull { asset ->
        val path = assetPathReplacements[asset.localRelativePath.replace('\\', '/')] ?: return@mapNotNull null
        if (current.contains(path)) null
        else "![${escapeAltText(asset.caption.ifBlank { "Captured diagram" })}]($path)"
    }
    if (missingAssets.isNotEmpty()) {
        sections += "## Captured diagrams\n\n${missingAssets.joinToString("\n\n")}"
    }
    val unavailableAssets = assets.filter { asset ->
        assetPathReplacements[asset.localRelativePath.replace('\\', '/')] == null
    }
    if (unavailableAssets.isNotEmpty()) {
        sections += "## Export warnings\n\n" + unavailableAssets.joinToString("\n") { asset ->
            "- Captured diagram unavailable: ${asset.caption.trim().ifBlank { "unnamed diagram" }}"
        }
    }
    return sections.joinToString("\n\n").trim() + "\n"
}

/**
 * The captured record: what was said and shown, with nothing written by a model.
 *
 * This is the other half of the evidence-preservation principle. The refined note can be
 * argued with; this file is the thing it is argued against, so it carries timestamps and
 * speaker labels and is never edited, summarised or reordered.
 */
internal fun renderCapturedMarkdown(
    note: NoteEntity,
    transcript: List<TranscriptSegmentEntity>,
    assetFileNames: List<String> = emptyList(),
): String {
    val title = note.title.trim().ifBlank { "Untitled note" }
    val sections = mutableListOf(
        "# $title — captured evidence",
        "> [!info] Verbatim record\n" +
            "> Transcribed speech and captured board images exactly as recorded, with no AI rewriting.\n" +
            "> Speaker labels are local to each 20-second chunk and are not identity claims.\n" +
            "> The refined note is in `${safeExportName(note.title)}.md`.",
    )

    if (transcript.isNotEmpty()) {
        val lines = transcript.map { segment ->
            val speaker = segment.speakerId?.trim().orEmpty().ifBlank { "?" }
            "- `${formatClock(segment.startMs)}` **$speaker** — ${segment.text.trim()}"
        }
        sections += "## Transcript\n\n" + lines.joinToString("\n")
    }

    if (assetFileNames.isNotEmpty()) {
        sections += "## Captured board images\n\n" + assetFileNames.joinToString("\n\n") { name ->
            "![Captured board](assets/$name)"
        }
    }
    return sections.joinToString("\n\n").trim() + "\n"
}

internal fun formatClock(millis: Long): String {
    val totalSeconds = (millis / 1_000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private val privateImageLink = Regex("!\\[([^]]*)]\\((?:\\./)?note-assets/[^)]+\\)")

private fun replaceUnavailablePrivateAssetLinks(value: String): String = privateImageLink.replace(value) { match ->
    val alt = match.groupValues[1].trim().ifBlank { "captured diagram" }
    "> [!warning] Diagram unavailable in this export: $alt"
}

private fun safeExportName(value: String): String {
    val slug = value.trim().lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .take(64)
    return slug.ifBlank { "voxbox-note" }
}

private fun escapeAltText(value: String): String = value.replace("[", "\\[").replace("]", "\\]")

private fun addZipFile(zip: ZipOutputStream, file: File, entryName: String) {
    zip.putNextEntry(ZipEntry(entryName))
    FileInputStream(file).use { it.copyTo(zip) }
    zip.closeEntry()
}
