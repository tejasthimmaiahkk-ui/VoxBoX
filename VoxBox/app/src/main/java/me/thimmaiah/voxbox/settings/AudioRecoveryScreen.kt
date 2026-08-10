package me.thimmaiah.voxbox.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.thimmaiah.voxbox.session.CaptureSessionViewModel
import me.thimmaiah.voxbox.session.RetainedAudioChunk
import me.thimmaiah.voxbox.ui.VbCard
import me.thimmaiah.voxbox.ui.VbEmptyState
import me.thimmaiah.voxbox.ui.VbEyebrow
import me.thimmaiah.voxbox.ui.VbNotice
import me.thimmaiah.voxbox.ui.VbOutlineButton
import me.thimmaiah.voxbox.ui.VbPrimaryButton
import me.thimmaiah.voxbox.ui.theme.LocalVbStatus
import me.thimmaiah.voxbox.ui.theme.VbMono
import me.thimmaiah.voxbox.ui.theme.VbSpace
import java.util.Locale

/**
 * Audio that was captured but whose note revision never committed.
 *
 * This is the safety net behind the whole capture path: speech cannot be re-recorded, so a chunk
 * whose upload or note update failed is written to disk rather than dropped. It lives in settings
 * because it is a recovery task, not something to think about mid-lecture.
 */
@Composable
fun AudioRecoveryScreen(viewModel: CaptureSessionViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val status = LocalVbStatus.current

    VbSubPage(title = "Unrecovered audio", onBack = onBack) {
        if (state.retainedAudio.isEmpty()) {
            VbEmptyState(
                title = "Nothing waiting",
                body = "Audio only lands here when a chunk was captured but its note revision " +
                    "did not commit. An empty list is the normal state.",
            )
        } else {
            VbNotice(
                title = "${state.retainedAudio.size} file${if (state.retainedAudio.size == 1) "" else "s"} kept",
                body = "Recovering appends a labelled \"Recovered audio\" section to the original " +
                    "note. It does not re-run refinement, so nothing already written is rewritten.",
                tone = status.review,
            )
            Spacer(Modifier.height(VbSpace.section))
            state.retainedAudio.forEach { chunk ->
                RetainedRow(
                    chunk = chunk,
                    onRecover = { viewModel.retryRetainedAudio(chunk.id) },
                    onDelete = { viewModel.deleteRetainedAudio(chunk.id) },
                )
                Spacer(Modifier.height(VbSpace.gap))
            }
        }

        Spacer(Modifier.height(VbSpace.section))
        VbEyebrow("Refinement failures")
        Spacer(Modifier.height(8.dp))
        val failure = state.serviceFailure
        if (failure == null) {
            VbCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No refinement failure recorded in this session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalVbStatus.current.fg2,
                )
            }
        } else {
            VbCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = failure.code,
                    style = MaterialTheme.typography.titleMedium,
                    color = status.danger,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = failure.describe(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (failure.retryable) {
                        "Retryable. The captured evidence was appended without silent correction."
                    } else {
                        "Not retryable. The captured evidence was appended without silent correction."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalVbStatus.current.fg2,
                )
            }
        }
        VbNote(
            "Captured speech is never discarded because the AI step failed. The transcript is " +
                "written first; refinement is an addition on top of it.",
        )
    }
}

@Composable
private fun RetainedRow(chunk: RetainedAudioChunk, onRecover: () -> Unit, onDelete: () -> Unit) {
    VbCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                text = formatClock(chunk.offsetMs),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VbMono),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${formatDuration(chunk.durationMs)} · ${formatSize(chunk.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = LocalVbStatus.current.fg2,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = chunk.reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VbPrimaryButton(
                text = if (chunk.retrying) "Recovering…" else "Recover",
                onClick = onRecover,
                enabled = !chunk.retrying,
                height = 44.dp,
                modifier = Modifier.weight(1f),
            )
            VbOutlineButton(
                text = "Delete",
                onClick = onDelete,
                contentColor = LocalVbStatus.current.danger,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun formatClock(millis: Long): String {
    val seconds = (millis / 1_000).coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
}

private fun formatDuration(millis: Long): String =
    "${(millis / 1_000).coerceAtLeast(0)} s"

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
    bytes >= 1_024 -> "${bytes / 1_024} KB"
    else -> "$bytes B"
}
