package me.thimmaiah.voxbox.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.thimmaiah.voxbox.data.SettingsRepository
import me.thimmaiah.voxbox.data.VbExportFormat
import me.thimmaiah.voxbox.data.VbSettings
import me.thimmaiah.voxbox.ui.VbCard
import me.thimmaiah.voxbox.ui.VbChoiceTile
import me.thimmaiah.voxbox.ui.VbEyebrow
import me.thimmaiah.voxbox.ui.VbSwitchRow
import me.thimmaiah.voxbox.ui.theme.VbSpace

@Composable
fun ExportScreen(
    settingsRepository: SettingsRepository,
    scope: CoroutineScope,
    onBack: () -> Unit,
) {
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = VbSettings())

    VbSubPage(title = "Export defaults", onBack = onBack) {
        VbEyebrow("Format")
        Spacer(Modifier.height(10.dp))
        VbChoiceTile(
            title = VbExportFormat.Zip.label,
            body = "One archive holding the refined note, the captured-evidence note, and every " +
                "diagram crop with working relative links.",
            selected = settings.exportFormat == VbExportFormat.Zip,
            onClick = { scope.launch { settingsRepository.setExportFormat(VbExportFormat.Zip) } },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(VbSpace.gap))
        VbChoiceTile(
            title = VbExportFormat.MarkdownOnly.label,
            body = "Text only. Diagram links are replaced with a labelled warning rather than " +
                "left pointing at files that are not in the export.",
            selected = settings.exportFormat == VbExportFormat.MarkdownOnly,
            onClick = {
                scope.launch { settingsRepository.setExportFormat(VbExportFormat.MarkdownOnly) }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(VbSpace.section))

        VbEyebrow("Contents")
        Spacer(Modifier.height(8.dp))
        VbCard(modifier = Modifier.fillMaxWidth()) {
            VbSwitchRow(
                title = "Include review flags",
                supporting = "Exported as a labelled section at the end, never inline. A flag is a " +
                    "disagreement between the AI and the evidence, so it must not read as note text.",
                checked = settings.exportIncludeFlags,
                onCheckedChange = { scope.launch { settingsRepository.setExportIncludeFlags(it) } },
            )
        }
        VbNote(
            "Every export contains two documents: the refined note, and a captured-evidence note " +
                "holding the verbatim transcript with timestamps and speaker labels. Keeping them " +
                "apart is what lets one be checked against the other.",
        )
        VbNote("Generated export files are deleted from the cache after 24 hours.")
    }
}
