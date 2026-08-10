package me.thimmaiah.voxbox.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import me.thimmaiah.voxbox.notes.TranscriptSegmentEntity
import me.thimmaiah.voxbox.ui.VbEyebrow
import me.thimmaiah.voxbox.ui.VbOutlineButton
import me.thimmaiah.voxbox.ui.VbPrimaryButton
import me.thimmaiah.voxbox.ui.theme.LocalVbStatus
import me.thimmaiah.voxbox.ui.theme.VbMono
import me.thimmaiah.voxbox.ui.theme.VbShape
import java.util.Locale

/**
 * A disagreement, shown as a decision rather than as text.
 *
 * The captured line comes first and stays first. Neither action rewrites it: "Keep captured"
 * dismisses the suggestion, and "Add annotation" appends the suggestion *beside* the original as a
 * marked note. That is the whole evidence-preservation promise made visible — if the model is
 * wrong, the student can still see what was actually said.
 */
@Composable
fun ReviewFlagCard(
    flag: ReviewFlag,
    evidence: List<TranscriptSegmentEntity>,
    onKeepCaptured: () -> Unit,
    onAddAnnotation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tone = LocalVbStatus.current.review
    var evidenceOpen by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(VbShape.card)
            .background(tone.copy(alpha = 0.10f))
            .border(1.dp, tone.copy(alpha = 0.35f), VbShape.card)
            .padding(16.dp),
    ) {
        Row {
            VbEyebrow("Needs your review", color = tone)
            Spacer(Modifier.weight(1f))
            if (flag.severity.isNotBlank()) {
                Text(
                    text = flag.severity.uppercase(Locale.US),
                    style = MaterialTheme.typography.labelSmall,
                    color = tone,
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        VbEyebrow("Captured")
        Spacer(Modifier.height(4.dp))
        Text(
            text = flag.captured,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))

        VbEyebrow("Suggested", color = tone)
        Spacer(Modifier.height(4.dp))
        Text(
            text = flag.suggested,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (flag.reason.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = flag.reason,
                style = MaterialTheme.typography.bodySmall,
                color = LocalVbStatus.current.fg2,
            )
        }

        if (flag.evidenceIds.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (evidenceOpen) "Hide evidence" else "Evidence (${flag.evidenceIds.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(VbShape.pill)
                    .clickable { evidenceOpen = !evidenceOpen }
                    .padding(vertical = 6.dp),
            )
            AnimatedVisibility(
                visible = evidenceOpen,
                enter = expandVertically(tween(240)) + fadeIn(),
                exit = shrinkVertically(tween(240)) + fadeOut(),
            ) {
                EvidenceQuotes(flag.evidenceIds, evidence)
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VbOutlineButton(
                text = "Keep captured",
                onClick = onKeepCaptured,
                modifier = Modifier.weight(1f),
            )
            VbPrimaryButton(
                text = "Add annotation",
                onClick = onAddAnnotation,
                container = tone,
                content = MaterialTheme.colorScheme.onPrimary,
                height = 44.dp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** The transcript behind a flag, quoted with its timestamp and chunk-local speaker label. */
@Composable
private fun EvidenceQuotes(ids: List<String>, evidence: List<TranscriptSegmentEntity>) {
    val matched = evidence.filter { it.id in ids }
    Column(Modifier.padding(top = 8.dp)) {
        if (matched.isEmpty()) {
            Text(
                text = "The transcript for this flag is no longer on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalVbStatus.current.fg2,
            )
            return@Column
        }
        matched.forEach { segment ->
            Row(Modifier.padding(vertical = 4.dp)) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(38.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${clock(segment.startMs)} · speaker ${segment.speakerId ?: "?"}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = VbMono),
                        color = LocalVbStatus.current.fg3,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = segment.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

private fun clock(millis: Long): String {
    val seconds = (millis / 1_000).coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
}
