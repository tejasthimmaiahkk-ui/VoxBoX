package me.thimmaiah.voxbox.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.thimmaiah.voxbox.BuildConfig
import me.thimmaiah.voxbox.network.VbHealthProbe
import me.thimmaiah.voxbox.network.probeVoxBoxHealth
import me.thimmaiah.voxbox.ui.VbCard
import me.thimmaiah.voxbox.ui.VbEyebrow
import me.thimmaiah.voxbox.ui.VbOutlineButton
import me.thimmaiah.voxbox.ui.VbPill
import me.thimmaiah.voxbox.ui.theme.LocalVbStatus
import me.thimmaiah.voxbox.ui.theme.VbMono
import me.thimmaiah.voxbox.ui.theme.VbSpace

@Composable
fun ConnectionScreen(onBack: () -> Unit) {
    val status = LocalVbStatus.current
    var probe by remember { mutableStateOf<VbHealthProbe?>(null) }
    var probing by remember { mutableStateOf(true) }
    var attempt by remember { mutableStateOf(0) }

    LaunchedEffect(attempt) {
        probing = true
        probe = withContext(Dispatchers.IO) { probeVoxBoxHealth() }
        probing = false
    }

    VbSubPage(title = "Connection", onBack = onBack) {
        VbCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Proxy",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                val result = probe
                when {
                    probing -> VbPill("Checking", status.fg2)
                    result == null -> VbPill("Unreachable", status.danger)
                    else -> VbPill("${result.latencyMs} ms", status.saved)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = BuildConfig.VOXBOX_API_BASE_URL.ifBlank { "not configured" },
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = VbMono),
                color = LocalVbStatus.current.fg2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            VbOutlineButton("Check again", onClick = { attempt++ })
        }
        VbNote(
            "The provider key never lives in this app. The phone authenticates to the proxy with " +
                "a client token, and the proxy holds the AI credential in its own secret store.",
        )
        Spacer(Modifier.height(VbSpace.section))

        VbEyebrow("Model routing")
        Spacer(Modifier.height(8.dp))
        VbCard(modifier = Modifier.fillMaxWidth()) {
            val models = probe?.models
            ModelRow("Transcription + speakers", models?.transcription)
            Spacer(Modifier.height(10.dp))
            ModelRow("Board extraction", models?.vision)
            Spacer(Modifier.height(10.dp))
            ModelRow("Note synthesis", models?.notes)
        }
        VbNote(
            "Models are chosen per job on the server and can be swapped without rebuilding the " +
                "app. If these read as unknown, the proxy could not be reached.",
        )
        Spacer(Modifier.height(VbSpace.section))

        VbEyebrow("Budget")
        Spacer(Modifier.height(8.dp))
        VbCard(modifier = Modifier.fillMaxWidth()) {
            val budget = probe?.budget
            Text(
                text = if (budget == null) {
                    "Unknown"
                } else {
                    "${budget.used} of ${budget.limit} requests used today"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "A hard daily cap on the server, so a runaway session cannot run up a bill.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalVbStatus.current.fg2,
            )
        }
    }
}

@Composable
private fun ModelRow(label: String, model: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(150.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = model ?: "unknown",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = VbMono),
            color = LocalVbStatus.current.fg2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
