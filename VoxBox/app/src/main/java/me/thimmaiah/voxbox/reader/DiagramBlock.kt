package me.thimmaiah.voxbox.reader

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.thimmaiah.voxbox.ui.VbCard
import me.thimmaiah.voxbox.ui.theme.LocalVbStatus
import me.thimmaiah.voxbox.ui.theme.VbShape
import java.io.File

/** A Markdown image link and its alt text, as written into a note by the capture pipeline. */
data class DiagramLink(val alt: String, val path: String)

private val imageLink = Regex("!\\[([^]]*)]\\(([^)]+)\\)")

/** Finds every image link in a block so the reader can render pictures rather than link syntax. */
fun findDiagramLinks(markdown: String): List<DiagramLink> =
    imageLink.findAll(markdown).map { DiagramLink(it.groupValues[1], it.groupValues[2]) }.toList()

/** Strips image links from prose that is being rendered as text beside the pictures. */
fun withoutDiagramLinks(markdown: String): String =
    imageLink.replace(markdown, "").lines().filter { it.isNotBlank() }.joinToString("\n")

/**
 * A captured board crop, with a tap-to-open lightbox.
 *
 * Crops are the one part of a note that cannot be re-read as text, so they are worth the full
 * screen: a formula that was legible on the board is often unreadable at thumbnail size.
 */
@Composable
fun DiagramBlock(link: DiagramLink, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(link.path) { mutableStateOf<ImageBitmap?>(null) }
    var missing by remember(link.path) { mutableStateOf(false) }
    var open by remember { mutableStateOf(false) }

    LaunchedEffect(link.path) {
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val file = resolveAsset(context.filesDir, link.path)
                if (file?.isFile != true) return@runCatching null
                android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            }.getOrNull()
        }
        bitmap = loaded
        missing = loaded == null
    }

    VbCard(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = link.alt.ifBlank { "Captured board diagram" },
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(VbShape.media)
                    .clickable(onClickLabel = "Open diagram") { open = true },
            )
        } else {
            Text(
                text = if (missing) {
                    "This diagram is no longer on the device."
                } else {
                    "Loading diagram…"
                },
                style = MaterialTheme.typography.bodySmall,
                color = LocalVbStatus.current.fg2,
            )
        }
        if (link.alt.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = link.alt,
                style = MaterialTheme.typography.bodySmall,
                color = LocalVbStatus.current.fg2,
            )
        }
    }

    val image = bitmap
    if (open && image != null) {
        Dialog(onDismissRequest = { open = false }) {
            var scale by remember { mutableStateOf(1f) }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF00A0B0E))
                    .clickable(onClickLabel = "Close diagram") { open = false }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 6f)
                        }
                    },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = image,
                        contentDescription = link.alt.ifBlank { "Captured board diagram" },
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { scaleX = scale; scaleY = scale },
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = link.alt.ifBlank { "Captured from the board" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
        }
    }
}

/**
 * Resolves an in-note asset path against private storage.
 *
 * Rejects anything absolute or containing a parent segment: note text is model output, and a
 * path is the one part of it that could reach outside the app's own files.
 */
internal fun resolveAsset(filesDir: File, path: String): File? {
    val normalized = path.removePrefix("./").replace('\\', '/').trim()
    if (normalized.isBlank() || normalized.startsWith('/') || ':' in normalized) return null
    if (normalized.split('/').any { it == ".." }) return null
    return File(filesDir, normalized)
}
