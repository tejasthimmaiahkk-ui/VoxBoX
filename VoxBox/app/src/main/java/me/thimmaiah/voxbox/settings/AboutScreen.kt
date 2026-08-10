package me.thimmaiah.voxbox.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.thimmaiah.voxbox.BuildConfig
import me.thimmaiah.voxbox.ui.VbCard
import me.thimmaiah.voxbox.ui.VbEyebrow
import me.thimmaiah.voxbox.ui.theme.LocalVbStatus
import me.thimmaiah.voxbox.ui.theme.VbShape
import me.thimmaiah.voxbox.ui.theme.VbSpace

/**
 * A claims surface. Everything stated here has to survive being read by someone who then tests it.
 *
 * The three-level split is deliberate: "verified on a device", "verified against the live
 * service", and "not evaluated" are different kinds of confidence, and collapsing them into one
 * green tick would be the easiest lie in the app to tell.
 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val status = LocalVbStatus.current

    VbSubPage(title = "About & evidence", onBack = onBack) {
        VbEyebrow("What has been verified")
        Spacer(Modifier.height(8.dp))
        VbCard(modifier = Modifier.fillMaxWidth()) {
            ClaimRow(
                dot = status.saved,
                title = "Device-verified",
                body = "Capture, transcription, board extraction and note updates run end to end " +
                    "on a physical phone, under automated tests.",
            )
            Spacer(Modifier.height(14.dp))
            ClaimRow(
                dot = MaterialTheme.colorScheme.primary,
                title = "Live-contract verified",
                body = "Every endpoint is exercised against the real models, with the response " +
                    "shape enforced by a strict schema rather than trusted.",
            )
            Spacer(Modifier.height(14.dp))
            ClaimRow(
                dot = status.review,
                title = "Not yet evaluated",
                body = "Word error rate, diarization accuracy, OCR accuracy and diagram-crop " +
                    "overlap have no measured figure. No accuracy percentage is claimed anywhere " +
                    "in this app.",
            )
        }
        Spacer(Modifier.height(VbSpace.section))

        VbEyebrow("Speaker labels")
        Spacer(Modifier.height(8.dp))
        VbCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Labels are local to each 20-second chunk",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "The app finds the dominant speaker within a chunk and prioritises them when " +
                    "the note is written. It does not identify who anyone is, does not carry a " +
                    "speaker identity from one chunk to the next, and stores no voiceprint or " +
                    "biometric data. Speaker “A” in one chunk is not necessarily “A” in the next.",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalVbStatus.current.fg2,
            )
        }
        Spacer(Modifier.height(VbSpace.section))

        VbEyebrow("Evidence preservation")
        Spacer(Modifier.height(8.dp))
        VbCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "The AI never silently rewrites what was captured",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "The transcript and the board text are written first and kept as they were " +
                    "recorded. Where the model disagrees with the evidence, the disagreement is " +
                    "surfaced as a review flag for you to accept or reject — it does not replace " +
                    "the captured line.",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalVbStatus.current.fg2,
            )
        }
        Spacer(Modifier.height(VbSpace.section))

        VbEyebrow("Consent")
        Spacer(Modifier.height(8.dp))
        VbCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Recording a class may require permission from your institution or the " +
                    "speaker. Capture is foreground-only and visible on screen for that reason — " +
                    "this app will not record with the screen off or in the background.",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalVbStatus.current.fg2,
            )
        }
        VbNote("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
    }
}

@Composable
private fun ClaimRow(dot: Color, title: String, body: String) {
    Row {
        Box(
            Modifier
                .padding(top = 5.dp)
                .size(10.dp)
                .clip(VbShape.pill)
                .background(dot),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(3.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = LocalVbStatus.current.fg2)
        }
    }
}
