package me.thimmaiah.voxbox.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import java.io.File

private val IMAGE_LINE = Regex("^!\\[(.*)]\\(([^)]+)\\)$")
private val NUMBERED_LINE = Regex("^(\\d+)[.)]\\s+(.*)$")

@Composable
fun MarkdownNotePreview(
    markdown: String,
    modifier: Modifier = Modifier,
    emptyMessage: String = "Structured notes will appear here after the first processed chunk.",
) {
    val lines = markdown.lineSequence().toList()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (markdown.isBlank()) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            lines.forEachIndexed { index, line ->
                MarkdownLine(line = line, key = index)
            }
        }
    }
}

@Composable
private fun MarkdownLine(line: String, key: Int) {
    val trimmed = line.trim()
    when {
        trimmed.isBlank() -> Text(" ", style = MaterialTheme.typography.bodySmall)
        IMAGE_LINE.matches(trimmed) -> {
            val match = IMAGE_LINE.matchEntire(trimmed) ?: return
            MarkdownAssetImage(
                caption = match.groupValues[1].ifBlank { "Captured diagram" },
                relativePath = match.groupValues[2],
                key = key,
            )
        }
        trimmed.startsWith("### ") -> MarkdownText(trimmed.removePrefix("### "), MaterialTheme.typography.titleMedium)
        trimmed.startsWith("## ") -> MarkdownText(trimmed.removePrefix("## "), MaterialTheme.typography.titleLarge)
        trimmed.startsWith("# ") -> MarkdownText(trimmed.removePrefix("# "), MaterialTheme.typography.headlineSmall)
        trimmed.startsWith("- ") || trimmed.startsWith("* ") -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
            MarkdownText(trimmed.drop(2), MaterialTheme.typography.bodyLarge, Modifier.weight(1f))
        }
        NUMBERED_LINE.matches(trimmed) -> {
            val match = NUMBERED_LINE.matchEntire(trimmed) ?: return
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${match.groupValues[1]}.", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                MarkdownText(match.groupValues[2], MaterialTheme.typography.bodyLarge, Modifier.weight(1f))
            }
        }
        trimmed.startsWith("> ") -> Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(10.dp),
        ) {
            MarkdownText(
                text = trimmed.removePrefix("> "),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            )
        }
        trimmed.startsWith("$") && trimmed.endsWith("$") -> Text(
            text = trimmed.trim('$'),
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.tertiary,
        )
        else -> MarkdownText(trimmed, MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MarkdownText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val highlight = MaterialTheme.colorScheme.tertiaryContainer
    Text(
        text = remember(text, highlight) { inlineMarkdown(text, highlight) },
        modifier = modifier,
        style = style,
    )
}

@Composable
private fun MarkdownAssetImage(
    caption: String,
    relativePath: String,
    key: Int,
) {
    val context = LocalContext.current
    val bitmap = remember(relativePath, key) {
        val normalized = relativePath.replace('\\', '/')
        if (normalized.startsWith('/') || normalized.contains("../") || ':' in normalized) null
        else BitmapFactory.decodeFile(File(context.filesDir, normalized).absolutePath)?.asImageBitmap()
    }
    if (bitmap == null) {
        Text(
            text = "Diagram: $caption (asset unavailable)",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Image(
                bitmap = bitmap,
                contentDescription = caption,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio((bitmap.width.toFloat() / bitmap.height).coerceIn(0.6f, 2.2f)),
            )
            Text(caption, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun inlineMarkdown(text: String, highlight: Color): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    while (cursor < text.length) {
        val tokens = listOf(
            Triple("**", "**", SpanStyle(fontWeight = FontWeight.Bold)),
            Triple("==", "==", SpanStyle(background = highlight)),
            Triple("<u>", "</u>", SpanStyle(textDecoration = TextDecoration.Underline)),
        )
        val match = tokens.mapNotNull { token ->
            val start = text.indexOf(token.first, cursor)
            if (start < 0) null else Pair(start, token)
        }.minByOrNull { it.first }
        if (match == null) {
            append(text.substring(cursor))
            break
        }
        val (start, token) = match
        append(text.substring(cursor, start))
        val contentStart = start + token.first.length
        val end = text.indexOf(token.second, contentStart)
        if (end < 0) {
            append(text.substring(start))
            break
        }
        pushStyle(token.third)
        append(text.substring(contentStart, end))
        pop()
        cursor = end + token.second.length
    }
}
