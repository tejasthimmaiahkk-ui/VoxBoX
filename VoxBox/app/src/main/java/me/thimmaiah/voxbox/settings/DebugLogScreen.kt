package me.thimmaiah.voxbox.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.thimmaiah.voxbox.debug.VbDebugLog
import me.thimmaiah.voxbox.ui.VbCard
import me.thimmaiah.voxbox.ui.VbNotice
import me.thimmaiah.voxbox.ui.VbOutlineButton
import me.thimmaiah.voxbox.ui.VbPrimaryButton
import me.thimmaiah.voxbox.ui.VbSwitchRow
import me.thimmaiah.voxbox.ui.shareFile
import me.thimmaiah.voxbox.ui.theme.LocalVbStatus
import me.thimmaiah.voxbox.ui.theme.VbMono
import me.thimmaiah.voxbox.ui.theme.VbSpace

/**
 * TEMPORARY. Remove with the rest of the debug log — see docs/TEMPORARY_DEBUG_LOG.md.
 */
@Composable
fun DebugLogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val status = LocalVbStatus.current
    var enabled by remember { mutableStateOf(VbDebugLog.enabled) }
    var refresh by remember { mutableIntStateOf(0) }
    val preview = remember(refresh) { VbDebugLog.snapshot().takeLast(1_500) }

    VbSubPage(title = "Diagnostics", onBack = onBack) {
        VbNotice(
            title = "Temporary feature",
            body = "This log exists to diagnose problems that only appear during a real lecture. " +
                "It is scheduled for removal and is not part of the app.",
            tone = status.review,
        )
        Spacer(Modifier.height(VbSpace.section))

        VbCard(modifier = Modifier.fillMaxWidth()) {
            VbSwitchRow(
                title = "Record diagnostics",
                supporting = "Speech is stored as a length and a short prefix, never in full. " +
                    "No audio, images or note text are recorded, and nothing is uploaded.",
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    VbDebugLog.enabled = it
                },
            )
        }
        Spacer(Modifier.height(VbSpace.section))

        VbPrimaryButton(
            text = "Share log",
            onClick = {
                val file = VbDebugLog.writeTo(context)
                shareFile(context, file, "text/plain", "VoxBox debug log")
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        VbOutlineButton(
            text = "Clear log",
            onClick = {
                VbDebugLog.clear()
                refresh++
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(VbSpace.section))

        VbCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = preview.ifBlank { "Nothing recorded yet. Run a capture session." },
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = VbMono),
                color = LocalVbStatus.current.fg2,
            )
        }
        VbNote("Showing the most recent entries. The full log is in the shared file.")
    }
}
