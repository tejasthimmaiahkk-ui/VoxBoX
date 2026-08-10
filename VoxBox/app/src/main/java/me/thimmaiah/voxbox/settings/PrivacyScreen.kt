package me.thimmaiah.voxbox.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.thimmaiah.voxbox.data.SettingsRepository
import me.thimmaiah.voxbox.data.VbSettings
import me.thimmaiah.voxbox.ui.VbCard
import me.thimmaiah.voxbox.ui.VbEyebrow
import me.thimmaiah.voxbox.ui.VbOutlineButton
import me.thimmaiah.voxbox.ui.VbSwitchRow
import me.thimmaiah.voxbox.ui.theme.LocalVbStatus
import me.thimmaiah.voxbox.ui.theme.VbShape
import me.thimmaiah.voxbox.ui.theme.VbSpace
import java.io.File
import java.util.Locale

private data class VbStorage(val crops: Long, val notes: Long, val recovery: Long, val cache: Long) {
    val total: Long get() = crops + notes + recovery
}

@Composable
fun PrivacyScreen(
    settingsRepository: SettingsRepository,
    scope: CoroutineScope,
    onBack: () -> Unit,
) {
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = VbSettings())
    val context = LocalContext.current
    var storage by remember { mutableStateOf(VbStorage(0, 0, 0, 0)) }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(refresh) {
        storage = withContext(Dispatchers.IO) {
            VbStorage(
                crops = File(context.filesDir, "note-assets").directorySize(),
                notes = context.getDatabasePath("voxbox.db").parentFile.directorySize(),
                recovery = File(context.filesDir, "retained-audio").directorySize(),
                cache = File(context.cacheDir, "exports").directorySize(),
            )
        }
    }

    val status = LocalVbStatus.current

    VbSubPage(title = "Privacy & storage", onBack = onBack) {
        VbCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatSize(storage.total),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "on this device",
                style = MaterialTheme.typography.bodySmall,
                color = status.fg2,
            )
            Spacer(Modifier.height(14.dp))
            StorageBar(storage)
            Spacer(Modifier.height(12.dp))
            Legend(MaterialTheme.colorScheme.primary, "Diagram crops", storage.crops)
            Spacer(Modifier.height(6.dp))
            Legend(status.saved, "Notes and transcripts", storage.notes)
            Spacer(Modifier.height(6.dp))
            Legend(status.review, "Recovery audio", storage.recovery)
        }
        Spacer(Modifier.height(VbSpace.section))

        VbEyebrow("Capture")
        Spacer(Modifier.height(8.dp))
        VbCard(modifier = Modifier.fillMaxWidth()) {
            VbSwitchRow(
                title = "Keep raw frames",
                supporting = "Off by default. Frames are deleted once their note update and crops " +
                    "commit, so the board is kept as text and crops rather than photographs.",
                checked = settings.keepRawFrames,
                onCheckedChange = { scope.launch { settingsRepository.setKeepRawFrames(it) } },
            )
            VbSwitchRow(
                title = "Keep screen awake while capturing",
                supporting = "Capture is foreground-only, so the session stops if the screen does.",
                checked = settings.keepScreenAwake,
                onCheckedChange = { scope.launch { settingsRepository.setKeepScreenAwake(it) } },
            )
        }
        Spacer(Modifier.height(VbSpace.section))

        VbEyebrow("Export cache")
        Spacer(Modifier.height(8.dp))
        VbCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${formatSize(storage.cache)} of generated export files",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            VbOutlineButton(
                text = "Clear export cache",
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            File(context.cacheDir, "exports").deleteRecursively()
                        }
                        refresh++
                    }
                },
            )
        }
        VbNote(
            "Exports are cleaned up automatically after 24 hours. Recording is foreground-only " +
                "and system backup is disabled, so notes and audio never leave this device except " +
                "through an export you start yourself.",
        )
    }
}

@Composable
private fun StorageBar(storage: VbStorage) {
    val status = LocalVbStatus.current
    val total = storage.total.coerceAtLeast(1)
    Row(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(VbShape.pill)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Segment(MaterialTheme.colorScheme.primary, storage.crops.toFloat() / total)
        Segment(status.saved, storage.notes.toFloat() / total)
        Segment(status.review, storage.recovery.toFloat() / total)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Segment(color: Color, fraction: Float) {
    if (fraction <= 0f) return
    Box(
        Modifier
            .weight(fraction)
            .fillMaxHeight()
            .background(color),
    )
}

@Composable
private fun Legend(color: Color, label: String, bytes: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(VbShape.pill).background(color))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatSize(bytes),
            style = MaterialTheme.typography.bodySmall,
            color = LocalVbStatus.current.fg2,
        )
    }
}

private fun File?.directorySize(): Long {
    if (this == null || !exists()) return 0
    return walkTopDown().filter { it.isFile }.sumOf { it.length() }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> String.format(Locale.US, "%.1f GB", bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
    bytes >= 1_024 -> "${bytes / 1_024} KB"
    else -> "$bytes B"
}
