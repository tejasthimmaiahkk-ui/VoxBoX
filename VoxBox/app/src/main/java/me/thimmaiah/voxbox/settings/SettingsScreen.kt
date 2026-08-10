package me.thimmaiah.voxbox.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.thimmaiah.voxbox.data.SettingsRepository
import me.thimmaiah.voxbox.data.VbSettings
import me.thimmaiah.voxbox.data.VbThemeMode
import me.thimmaiah.voxbox.nav.VbRoute
import me.thimmaiah.voxbox.session.CaptureSessionViewModel
import me.thimmaiah.voxbox.ui.VbNavRow
import me.thimmaiah.voxbox.ui.theme.LocalVbStatus
import me.thimmaiah.voxbox.ui.theme.VbShape

/**
 * Three groups, six destinations.
 *
 * Everything here used to live in the capture screen. It is all durable configuration — decided
 * once and rarely revisited — and mixing it with the four decisions you make before each lecture
 * is what made that screen a five-step form.
 */
@Composable
fun SettingsScreen(
    captureViewModel: CaptureSessionViewModel,
    settingsRepository: SettingsRepository,
    contentPadding: PaddingValues,
    onOpen: (String) -> Unit,
) {
    val capture by captureViewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = VbSettings())
    val status = LocalVbStatus.current
    val unrecovered = capture.retainedAudio.size

    VbTabPage(title = "Settings", contentPadding = contentPadding) {
        VbSettingsGroup("Capture") {
            VbNavRow(
                title = "Unrecovered audio",
                supporting = if (unrecovered == 0) {
                    "Nothing waiting"
                } else {
                    "$unrecovered file${if (unrecovered == 1) "" else "s"} kept on this device"
                },
                badge = if (unrecovered > 0) unrecovered.toString() else null,
                badgeColor = if (unrecovered > 0) status.review else null,
                onClick = { onOpen(VbRoute.SET_AUDIO_RECOVERY) },
            )
            VbNavRow(
                title = "Connection & models",
                supporting = "Proxy reachability and which model does which job",
                onClick = { onOpen(VbRoute.SET_CONNECTION) },
            )
        }

        VbSettingsGroup("App") {
            VbNavRow(
                title = "Appearance",
                supporting = "${themeLabel(settings.theme)} · ${settings.accent.name} accent",
                trailing = {
                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(VbShape.pill)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Box(Modifier.size(8.dp))
                },
                onClick = { onOpen(VbRoute.SET_APPEARANCE) },
            )
            VbNavRow(
                title = "Export defaults",
                supporting = settings.exportFormat.label,
                onClick = { onOpen(VbRoute.SET_EXPORT) },
            )
        }

        VbSettingsGroup("Data") {
            VbNavRow(
                title = "Privacy & storage",
                supporting = "What is kept on this device, and what is never sent",
                onClick = { onOpen(VbRoute.SET_PRIVACY) },
            )
            VbNavRow(
                title = "About & evidence",
                supporting = "What this app claims, and what it does not",
                onClick = { onOpen(VbRoute.SET_ABOUT) },
            )
        }
    }
}

private fun themeLabel(mode: VbThemeMode) = when (mode) {
    VbThemeMode.System -> "System"
    VbThemeMode.Dark -> "Dark"
    VbThemeMode.Light -> "Light"
}
