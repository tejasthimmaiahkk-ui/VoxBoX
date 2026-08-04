package me.thimmaiah.voxbox.session

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.util.Locale
import me.thimmaiah.voxbox.audio.SpeakerFocusStatus
import me.thimmaiah.voxbox.network.VoxBoxFailureKind
import me.thimmaiah.voxbox.network.VoxBoxServiceFailure
import me.thimmaiah.voxbox.ui.MarkdownNotePreview
import me.thimmaiah.voxbox.ui.VoxBoxBanner
import me.thimmaiah.voxbox.ui.VoxBoxChip
import me.thimmaiah.voxbox.ui.VoxBoxChipGroup
import me.thimmaiah.voxbox.ui.VoxBoxChoiceRow
import me.thimmaiah.voxbox.ui.VoxBoxIcons
import me.thimmaiah.voxbox.ui.VoxBoxLayout
import me.thimmaiah.voxbox.ui.VoxBoxSectionCard
import me.thimmaiah.voxbox.ui.VoxBoxSectionHeader
import me.thimmaiah.voxbox.ui.VoxBoxSpacing
import me.thimmaiah.voxbox.ui.VoxBoxStat
import me.thimmaiah.voxbox.ui.VoxBoxStatusPill
import me.thimmaiah.voxbox.ui.VoxBoxStatusTone

@Composable
fun CaptureSessionScreen(
    onOpenNote: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CaptureSessionViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    var permissionRevision by remember { mutableIntStateOf(0) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionRevision += 1
        if (hasRequiredPermissions(context, state.mode)) viewModel.startSession()
    }
    val syllabusLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importSyllabus)
    }
    val permissionsGranted = remember(permissionRevision, state.mode) {
        hasRequiredPermissions(context, state.mode)
    }

    BackHandler(enabled = state.isActive) {
        viewModel.stopSession()
    }
    DisposableEffect(viewModel) {
        onDispose { viewModel.onLiveScreenLeaving() }
    }
    DisposableEffect(view, state.isActive) {
        val previousKeepScreenOn = view.keepScreenOn
        if (state.isActive) view.keepScreenOn = true
        onDispose { view.keepScreenOn = previousKeepScreenOn }
    }

    if (state.stage in setOf(LiveCaptureStage.RUNNING, LiveCaptureStage.STOPPING)) {
        LiveSessionContent(
            state = state,
            modifier = modifier,
            onStop = viewModel::stopSession,
            onFrameCaptured = viewModel::onFrameCaptured,
            onSelectPrimarySpeaker = viewModel::selectPrimarySpeaker,
        )
    } else {
        SessionSetupContent(
            state = state,
            modifier = modifier,
            permissionsGranted = permissionsGranted,
            onMode = viewModel::setMode,
            onPolicy = viewModel::setNotePolicy,
            onNoteDetail = viewModel::setNoteDetail,
            onCustomInstruction = viewModel::setCustomInstruction,
            onTitle = viewModel::setNoteTitle,
            onSelectNote = viewModel::selectExistingNote,
            onSelectFolder = viewModel::selectFolder,
            onSelectSyllabus = viewModel::selectSyllabus,
            onFrameInterval = viewModel::setFrameIntervalMillis,
            onThreshold = viewModel::setChangeThreshold,
            onCreateFolder = viewModel::createFolder,
            onImportSyllabus = { syllabusLauncher.launch(arrayOf("text/markdown", "text/plain", "text/*")) },
            onStart = {
                if (permissionsGranted) viewModel.startSession()
                else permissionLauncher.launch(requiredPermissions(state.mode))
            },
            onReset = viewModel::resetAfterStop,
            onOpenNote = onOpenNote,
            onRetryRetainedAudio = viewModel::retryRetainedAudio,
            onDeleteRetainedAudio = viewModel::deleteRetainedAudio,
        )
    }
}

@Composable
private fun SessionSetupContent(
    state: CaptureSessionUiState,
    modifier: Modifier,
    permissionsGranted: Boolean,
    onMode: (CaptureMode) -> Unit,
    onPolicy: (CaptureNotePolicy) -> Unit,
    onNoteDetail: (NoteDetail) -> Unit,
    onCustomInstruction: (String) -> Unit,
    onTitle: (String) -> Unit,
    onSelectNote: (String?) -> Unit,
    onSelectFolder: (String?) -> Unit,
    onSelectSyllabus: (String?) -> Unit,
    onFrameInterval: (Long) -> Unit,
    onThreshold: (Double) -> Unit,
    onCreateFolder: (String) -> Unit,
    onImportSyllabus: () -> Unit,
    onStart: () -> Unit,
    onReset: () -> Unit,
    onOpenNote: (String) -> Unit,
    onRetryRetainedAudio: (String) -> Unit,
    onDeleteRetainedAudio: (String) -> Unit,
) {
    var folderName by rememberSaveable { mutableStateOf("") }
    val videoMode = state.mode == CaptureMode.VIDEO
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = VoxBoxLayout.compactScreenPadding,
            top = VoxBoxSpacing.small,
            end = VoxBoxLayout.compactScreenPadding,
            bottom = VoxBoxLayout.listBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(VoxBoxLayout.sectionSpacing),
    ) {
        item("intro") {
            Column(
                modifier = Modifier.padding(
                    start = VoxBoxSpacing.xSmall,
                    end = VoxBoxSpacing.xSmall,
                    top = VoxBoxSpacing.xSmall,
                ),
                verticalArrangement = Arrangement.spacedBy(VoxBoxSpacing.xSmall),
            ) {
                Text("One session, one living note", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Capture runs in the foreground while this screen stays open. " +
                        "Leaving it stops capture and finishes the note.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.stage == LiveCaptureStage.STOPPED) {
            item("finished") {
                VoxBoxSectionCard(Modifier.fillMaxWidth(), tone = VoxBoxStatusTone.Success) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VoxBoxStatusPill("SAVED", tone = VoxBoxStatusTone.Success)
                        if (state.verifying) {
                            VoxBoxStatusPill(
                                label = "CHECKING",
                                tone = VoxBoxStatusTone.Accent,
                                pulsing = true,
                            )
                        }
                    }
                    Text(state.status, style = MaterialTheme.typography.bodyMedium)
                    MarkdownNotePreview(state.generatedMarkdown)
                    Row(horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small)) {
                        OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                            Text("New session")
                        }
                        Button(
                            onClick = { state.activeNoteId?.let(onOpenNote) },
                            enabled = state.activeNoteId != null,
                            modifier = Modifier.weight(1f),
                        ) { Text("Open note") }
                    }
                }
            }
        }

        state.serviceFailure?.let { failure ->
            item("service-failure") {
                ServiceFailureBanner(failure)
            }
        }

        state.verification?.takeIf { it.findings.isNotEmpty() }?.let { verification ->
            item("verification") {
                VerificationFindingsSection(verification)
            }
        }

        if (state.retainedAudio.isNotEmpty()) {
            item("retained-audio") {
                RetainedAudioSection(
                    retained = state.retainedAudio,
                    onRetry = onRetryRetainedAudio,
                    onDelete = onDeleteRetainedAudio,
                )
            }
        }

        item("mode") {
            VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                VoxBoxSectionHeader(
                    step = 1,
                    title = "Capture mode",
                    // These must not contain "Continuous audio" or "Camera + continuous audio" as a
                    // substring: the device smoke test waits for those exact live-screen headings
                    // with a substring matcher while the setup screen is still on screen.
                    supportingText = if (videoMode) {
                        "Board frames plus uninterrupted audio"
                    } else {
                        "Uninterrupted audio only"
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small)) {
                    ModeTile(
                        label = "Voice",
                        detail = "Audio only",
                        icon = VoxBoxIcons.Microphone,
                        selected = !videoMode,
                        onClick = { onMode(CaptureMode.VOICE) },
                        modifier = Modifier.weight(1f),
                    )
                    ModeTile(
                        label = "Live board",
                        detail = "Camera + audio",
                        icon = VoxBoxIcons.Camera,
                        selected = videoMode,
                        onClick = { onMode(CaptureMode.VIDEO) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item("note") {
            VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                VoxBoxSectionHeader(
                    step = 2,
                    title = "Note destination",
                    supportingText = "Start a new note or keep adding to a recent one",
                )
                OutlinedTextField(
                    value = state.noteTitle,
                    onValueChange = onTitle,
                    label = { Text("New note title") },
                    singleLine = true,
                    enabled = state.selectedNoteId == null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.notes.isNotEmpty()) {
                    VoxBoxChoiceRow(
                        label = "Create a new note",
                        supportingText = "Uses the title above",
                        selected = state.selectedNoteId == null,
                        onClick = { onSelectNote(null) },
                    )
                    state.notes.take(3).forEach { note ->
                        VoxBoxChoiceRow(
                            label = note.title,
                            supportingText = "Continue this note",
                            selected = state.selectedNoteId == note.id,
                            onClick = { onSelectNote(note.id) },
                        )
                    }
                }
            }
        }

        item("policy") {
            VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                VoxBoxSectionHeader(
                    step = 3,
                    title = "Note style",
                    supportingText = "How captured evidence becomes note content",
                )
                VoxBoxChoiceRow(
                    label = "Runnable notes",
                    supportingText = "Structured and deduplicated. Conflicts are flagged for review, never silently replaced.",
                    selected = state.notePolicy == CaptureNotePolicy.RUNNABLE,
                    onClick = { onPolicy(CaptureNotePolicy.RUNNABLE) },
                )
                VoxBoxChoiceRow(
                    label = "Verbatim",
                    supportingText = "Every diarized utterance in timestamp order, with no AI summarization.",
                    selected = state.notePolicy == CaptureNotePolicy.VERBATIM,
                    onClick = { onPolicy(CaptureNotePolicy.VERBATIM) },
                )
                if (state.notePolicy == CaptureNotePolicy.RUNNABLE) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("How much detail", style = MaterialTheme.typography.labelLarge)
                    VoxBoxChipGroup {
                        NoteDetail.entries.forEach { detail ->
                            VoxBoxChip(
                                label = when (detail) {
                                    NoteDetail.CONCISE -> "Short and precise"
                                    NoteDetail.STANDARD -> "Balanced"
                                    NoteDetail.DETAILED -> "Elaborate"
                                },
                                selected = state.noteDetail == detail,
                                onClick = { onNoteDetail(detail) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = state.customInstruction,
                        onValueChange = onCustomInstruction,
                        label = { Text("Extra instruction (optional)") },
                        placeholder = { Text("e.g. prefer worked examples over prose") },
                        supportingText = {
                            Text("Captured evidence always wins if this conflicts with it.")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                    )
                }
            }
        }

        if (videoMode) {
            item("video-settings") {
                VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                    VoxBoxSectionHeader(
                        step = 4,
                        title = "Camera efficiency",
                        supportingText = "Similar frames are dropped on this phone before any API call",
                    )
                    SliderRow(
                        label = "Capture interval",
                        value = "${state.frameIntervalMs / 1_000}s",
                        sliderValue = state.frameIntervalMs.toFloat(),
                        valueRange = 2_000f..30_000f,
                        steps = 27,
                        onValueChange = { onFrameInterval((it / 1_000).toLong() * 1_000) },
                    )
                    SliderRow(
                        label = "Change sensitivity",
                        value = String.format(Locale.US, "%.0f%%", state.changeThreshold * 100),
                        sliderValue = state.changeThreshold.toFloat(),
                        valueRange = 0.02f..0.30f,
                        steps = 0,
                        onValueChange = { onThreshold(it.toDouble()) },
                    )
                    Text(
                        text = "An accepted raw frame is deleted only after its note update and diagram crops commit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item("organization") {
            VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                VoxBoxSectionHeader(
                    step = if (videoMode) 5 else 4,
                    title = "Context and organization",
                    supportingText = "Optional folder and syllabus for this session",
                )
                Text("Folder", style = MaterialTheme.typography.labelLarge)
                VoxBoxChipGroup {
                    VoxBoxChip(
                        label = "No folder",
                        selected = state.selectedFolderId == null,
                        onClick = { onSelectFolder(null) },
                    )
                    state.folders.take(6).forEach { folder ->
                        VoxBoxChip(
                            label = folder.name,
                            selected = state.selectedFolderId == folder.id,
                            onClick = { onSelectFolder(folder.id) },
                            icon = VoxBoxIcons.Folder,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it.take(60) },
                        label = { Text("New folder") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalButton(
                        onClick = {
                            onCreateFolder(folderName)
                            folderName = ""
                        },
                        enabled = folderName.isNotBlank(),
                    ) { Text("Add") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Syllabus context", style = MaterialTheme.typography.labelLarge)
                VoxBoxChipGroup {
                    VoxBoxChip(
                        label = "No syllabus",
                        selected = state.selectedSyllabusId == null,
                        onClick = { onSelectSyllabus(null) },
                    )
                    state.syllabi.take(6).forEach { syllabus ->
                        VoxBoxChip(
                            label = syllabus.title,
                            selected = state.selectedSyllabusId == syllabus.id,
                            onClick = { onSelectSyllabus(syllabus.id) },
                            icon = VoxBoxIcons.Notes,
                        )
                    }
                }
                TextButton(onClick = onImportSyllabus) {
                    Icon(VoxBoxIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Import .md or .txt syllabus",
                        modifier = Modifier.padding(start = VoxBoxSpacing.small),
                    )
                }
                Text(
                    text = "Syllabus text stays local and is treated as context, not as proof of what was said.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item("start") {
            VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                VoxBoxSectionHeader(
                    title = "Ready to record",
                    supportingText = if (videoMode) {
                        "Microphone and camera are used only while this session runs"
                    } else {
                        "The microphone is used only while this session runs"
                    },
                    trailing = {
                        VoxBoxStatusPill(
                            label = if (permissionsGranted) "PERMISSIONS READY" else "PERMISSION NEEDED",
                            tone = if (permissionsGranted) {
                                VoxBoxStatusTone.Success
                            } else {
                                VoxBoxStatusTone.Warning
                            },
                        )
                    },
                )
                Button(
                    onClick = onStart,
                    enabled = state.stage != LiveCaptureStage.STARTING,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Start continuous ${state.mode.name.lowercase()} session"
                        },
                ) {
                    Icon(VoxBoxIcons.Microphone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = if (permissionsGranted) "Start live session" else "Allow and start",
                        modifier = Modifier.padding(start = VoxBoxSpacing.small),
                    )
                }
                state.error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Text(
                    text = state.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ModeTile(
    label: String,
    detail: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.primary else colors.surfaceContainerHigh,
        contentColor = if (selected) colors.onPrimary else colors.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: String,
    sliderValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(VoxBoxSpacing.xSmall)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}

@Composable
private fun ServiceFailureBanner(failure: VoxBoxServiceFailure) {
    val (title, tone) = when (failure.kind) {
        VoxBoxFailureKind.QUOTA_EXHAUSTED -> "AI quota exhausted" to VoxBoxStatusTone.Error
        VoxBoxFailureKind.RATE_LIMITED -> "AI service rate limited" to VoxBoxStatusTone.Warning
        VoxBoxFailureKind.AUTH -> "AI credential rejected" to VoxBoxStatusTone.Error
        VoxBoxFailureKind.REJECTED -> "Request rejected" to VoxBoxStatusTone.Error
        VoxBoxFailureKind.UNAVAILABLE -> "AI service unavailable" to VoxBoxStatusTone.Warning
    }
    val guidance = when (failure.kind) {
        VoxBoxFailureKind.QUOTA_EXHAUSTED ->
            "Captured audio is retained below instead of being retried. Add provider credits, then retry each file."
        VoxBoxFailureKind.AUTH ->
            "The proxy's provider key was refused. Configure a valid key in the proxy environment, never in the app."
        else -> "Captured evidence is preserved locally; nothing was discarded."
    }
    VoxBoxBanner(
        title = title,
        message = "${failure.message}\n$guidance",
        tone = tone,
    )
}

/**
 * End-of-session review findings.
 *
 * Presented as suggestions to confirm, never as applied edits: the same findings are already
 * appended to the note as a labelled review section, and the note text itself was not changed.
 */
@Composable
private fun VerificationFindingsSection(verification: NoteVerification) {
    VoxBoxSectionCard(Modifier.fillMaxWidth()) {
        VoxBoxSectionHeader(
            title = "End-of-session check",
            supportingText = "Formulas, units and concepts reviewed after the session",
            trailing = {
                VoxBoxStatusPill(
                    label = "${verification.findings.size} TO REVIEW",
                    tone = if (verification.warningCount > 0) {
                        VoxBoxStatusTone.Warning
                    } else {
                        VoxBoxStatusTone.Accent
                    },
                )
            },
        )
        Text(
            text = "These are suggestions. Nothing in the saved note was changed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        verification.findings.take(8).forEach { finding ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (finding.severity == "warning") {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = if (finding.severity == "warning") {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            ) {
                Column(
                    modifier = Modifier.padding(VoxBoxSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(VoxBoxSpacing.xSmall),
                ) {
                    Text(
                        text = "${finding.kind.name.lowercase().replaceFirstChar { it.titlecase() }} · " +
                            "${finding.severity} · ${(finding.confidence * 100).toInt()}% confident",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(finding.claim, style = MaterialTheme.typography.bodyMedium)
                    Text(finding.issue, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Suggested: ${finding.suggestion}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        if (verification.checkedFormulas.isNotEmpty() || verification.checkedConcepts.isNotEmpty()) {
            Text(
                text = "Checked ${verification.checkedFormulas.size} formula(s) and " +
                    "${verification.checkedConcepts.size} concept(s).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RetainedAudioSection(
    retained: List<RetainedAudioChunk>,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    VoxBoxSectionCard(Modifier.fillMaxWidth()) {
        VoxBoxSectionHeader(
            title = "Unrecovered audio",
            supportingText = "Kept privately on this phone until you recover or delete it",
            trailing = {
                VoxBoxStatusPill(
                    label = "${retained.size} FILE(S)",
                    tone = VoxBoxStatusTone.Warning,
                )
            },
        )
        retained.take(8).forEach { chunk ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier.padding(VoxBoxSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = VoxBoxIcons.Waveform,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "${formatClock(chunk.offsetMs)} · " +
                                "${(chunk.durationMs / 1_000).coerceAtLeast(0)}s · " +
                                "${chunk.sizeBytes / 1_024} KB",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f),
                        )
                        if (chunk.retrying) {
                            VoxBoxStatusPill(
                                label = "RETRYING",
                                tone = VoxBoxStatusTone.Accent,
                                pulsing = true,
                            )
                        }
                    }
                    Text(
                        text = chunk.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small)) {
                        FilledTonalButton(
                            onClick = { onRetry(chunk.id) },
                            enabled = !chunk.retrying,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(VoxBoxIcons.Retry, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Recover", modifier = Modifier.padding(start = VoxBoxSpacing.small))
                        }
                        OutlinedButton(
                            onClick = { onDelete(chunk.id) },
                            enabled = !chunk.retrying,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(VoxBoxIcons.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Delete", modifier = Modifier.padding(start = VoxBoxSpacing.small))
                        }
                    }
                }
            }
        }
        Text(
            text = "Recovery appends a labelled verbatim section to the original note. " +
                "Deleting discards that audio evidence permanently.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LiveSessionContent(
    state: CaptureSessionUiState,
    modifier: Modifier,
    onStop: () -> Unit,
    onFrameCaptured: (File, Long) -> Unit,
    onSelectPrimarySpeaker: (String?) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = VoxBoxLayout.compactScreenPadding,
            top = VoxBoxSpacing.small,
            end = VoxBoxLayout.compactScreenPadding,
            bottom = VoxBoxLayout.listBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(VoxBoxLayout.sectionSpacing),
    ) {
        item("live-status") {
            LiveHeaderCard(state = state, onStop = onStop)
        }

        if (state.mode == CaptureMode.VIDEO) {
            item("camera") {
                LiveCameraPanel(
                    enabled = state.stage == LiveCaptureStage.RUNNING,
                    intervalMs = state.frameIntervalMs,
                    onFrameCaptured = onFrameCaptured,
                )
            }
            item("frame-counters") {
                VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                    VoxBoxSectionHeader(
                        title = "Frame efficiency",
                        supportingText = state.lastFrameChangeScore?.let {
                            "Last change score ${String.format(Locale.US, "%.1f", it * 100)}%"
                        } ?: "Similar frames never reach the API",
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small)) {
                        VoxBoxStat("${state.acceptedFrames}", "Accepted", Modifier.weight(1f))
                        VoxBoxStat("${state.skippedFrames}", "Skipped", Modifier.weight(1f))
                        VoxBoxStat("${state.processedFrames}", "Done", Modifier.weight(1f))
                    }
                }
            }
        }

        item("speaker") {
            VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                VoxBoxSectionHeader(
                    title = "Speaker focus",
                    supportingText = state.speakerFocus.reason,
                    trailing = {
                        VoxBoxStatusPill(
                            label = state.speakerFocus.status.name,
                            tone = when (state.speakerFocus.status) {
                                SpeakerFocusStatus.FOCUSED, SpeakerFocusStatus.MANUAL -> VoxBoxStatusTone.Success
                                SpeakerFocusStatus.AMBIGUOUS, SpeakerFocusStatus.UNAVAILABLE -> VoxBoxStatusTone.Warning
                                SpeakerFocusStatus.LEARNING -> VoxBoxStatusTone.Accent
                            },
                        )
                    },
                )
                val speakers = state.latestChunkSpeakerIds.take(4)
                if (speakers.isNotEmpty()) {
                    Text(
                        text = "A/B labels belong to one transcription request and are never assumed " +
                            "to be the same person later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    VoxBoxChipGroup {
                        VoxBoxChip(
                            label = "Automatic",
                            selected = state.speakerFocus.selectedSpeakerId == null,
                            onClick = { onSelectPrimarySpeaker(null) },
                        )
                        speakers.forEach { speaker ->
                            VoxBoxChip(
                                label = "Speaker $speaker",
                                selected = state.speakerFocus.selectedSpeakerId == speaker,
                                onClick = { onSelectPrimarySpeaker(speaker) },
                            )
                        }
                    }
                }
            }
        }

        item("note-preview") {
            VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                VoxBoxSectionHeader(
                    title = "Structured note",
                    supportingText = "Revision ${state.revision}",
                    trailing = {
                        if (state.pendingEvents > 0) {
                            VoxBoxStatusPill(
                                label = "${state.pendingEvents} QUEUED",
                                tone = VoxBoxStatusTone.Accent,
                                pulsing = true,
                            )
                        }
                    },
                )
                MarkdownNotePreview(state.generatedMarkdown.ifBlank { state.existingNoteMarkdown })
            }
        }

        item("transcript") {
            VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                VoxBoxSectionHeader(
                    title = "Evidence transcript",
                    supportingText = "Stored before it contributes to the note",
                )
                if (state.transcript.isEmpty()) {
                    Text(
                        text = "Completed diarized segments appear after each audio chunk.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.transcript.takeLast(12).forEach { line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.medium)) {
                            Text(
                                text = formatClock(line.startMs),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Column(Modifier.weight(1f)) {
                                line.speakerId?.let { speaker ->
                                    Text(
                                        text = if (line.primary) "Speaker $speaker · focus" else "Speaker $speaker",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(line.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        state.serviceFailure?.let { failure ->
            item("live-service-failure") {
                ServiceFailureBanner(failure)
            }
        }

        if (state.corrections.isNotEmpty() || state.warnings.isNotEmpty() || state.error != null) {
            item("review") {
                VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                    VoxBoxSectionHeader(
                        title = "Review flags",
                        supportingText = "Suggestions and warnings; nothing was silently rewritten",
                    )
                    state.error?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                    state.corrections.forEach { correction ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ) {
                            Column(
                                modifier = Modifier.padding(VoxBoxSpacing.medium),
                                verticalArrangement = Arrangement.spacedBy(VoxBoxSpacing.xSmall),
                            ) {
                                Text("Captured: ${correction.captured}", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "Suggested annotation: ${correction.suggested}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(correction.reason, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    state.warnings.forEach { warning ->
                        Row(horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small)) {
                            Icon(
                                imageVector = VoxBoxIcons.Info,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveHeaderCard(
    state: CaptureSessionUiState,
    onStop: () -> Unit,
) {
    val stopping = state.stage == LiveCaptureStage.STOPPING
    VoxBoxSectionCard(Modifier.fillMaxWidth(), tone = VoxBoxStatusTone.Accent) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VoxBoxStatusPill(
                label = if (stopping) "FINISHING" else "LIVE",
                tone = if (stopping) VoxBoxStatusTone.Warning else VoxBoxStatusTone.Error,
                pulsing = !stopping,
            )
            Text("Revision ${state.revision}", style = MaterialTheme.typography.labelLarge)
        }
        Text(
            text = if (state.mode == CaptureMode.VIDEO) "Camera + continuous audio" else "Continuous audio",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(state.status, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small)) {
            VoxBoxStat("${state.pendingAudioChunks}", "Audio queue", Modifier.weight(1f))
            VoxBoxStat("${state.pendingFrames}", "Frame queue", Modifier.weight(1f))
            VoxBoxStat(
                value = "${state.retainedAudioChunks}",
                label = "Retained",
                modifier = Modifier.weight(1f),
                tone = if (state.retainedAudioChunks > 0) {
                    VoxBoxStatusTone.Error
                } else {
                    VoxBoxStatusTone.Neutral
                },
            )
        }
        Text(
            text = "Foreground only: keep this screen open. Leaving stops capture and drains saved audio " +
                "before finishing the note.",
            style = MaterialTheme.typography.bodySmall,
        )
        Button(
            onClick = onStop,
            enabled = state.stage == LiveCaptureStage.RUNNING,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Icon(VoxBoxIcons.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Stop and finish note", modifier = Modifier.padding(start = VoxBoxSpacing.small))
        }
    }
}

@Composable
private fun LiveCameraPanel(
    enabled: Boolean,
    intervalMs: Long,
    onFrameCaptured: (File, Long) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember(context) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }
    var captureInFlight by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    DisposableEffect(controller, lifecycleOwner, enabled, intervalMs) {
        val handler = Handler(Looper.getMainLooper())
        var sampler: Runnable? = null
        if (enabled) {
            try {
                controller.bindToLifecycle(lifecycleOwner)
                cameraError = null
                sampler = object : Runnable {
                    override fun run() {
                        if (!captureInFlight) {
                            captureInFlight = true
                            captureAutomaticFrame(
                                context = context,
                                controller = controller,
                                onSaved = { file, capturedAt ->
                                    captureInFlight = false
                                    onFrameCaptured(file, capturedAt)
                                },
                                onError = { message ->
                                    captureInFlight = false
                                    cameraError = message
                                },
                            )
                        }
                        handler.postDelayed(this, intervalMs)
                    }
                }.also { handler.postDelayed(it, 1_200) }
            } catch (error: Exception) {
                cameraError = error.message ?: "The rear camera could not start."
            }
        }
        onDispose {
            sampler?.let(handler::removeCallbacks)
            controller.unbind()
        }
    }
    Card(shape = MaterialTheme.shapes.large) {
        Column(verticalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                AndroidView(
                    factory = { previewContext ->
                        PreviewView(previewContext).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            this.controller = controller
                        }
                    },
                    update = { it.controller = controller },
                    modifier = Modifier.fillMaxSize(),
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(VoxBoxSpacing.medium),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = "Auto frame every ${intervalMs / 1_000}s · audio stays on",
                        modifier = Modifier.padding(horizontal = VoxBoxSpacing.medium, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            cameraError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = VoxBoxSpacing.small),
                )
            }
        }
    }
}

private fun captureAutomaticFrame(
    context: Context,
    controller: LifecycleCameraController,
    onSaved: (File, Long) -> Unit,
    onError: (String) -> Unit,
) {
    val target = try {
        File.createTempFile("voxbox-live-", ".jpg", context.cacheDir)
    } catch (_: Exception) {
        onError("A temporary frame file could not be created.")
        return
    }
    val capturedAt = System.currentTimeMillis()
    try {
        controller.takePicture(
            ImageCapture.OutputFileOptions.Builder(target).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) = onSaved(target, capturedAt)
                override fun onError(exception: ImageCaptureException) {
                    target.delete()
                    onError(exception.message ?: "The automatic frame could not be captured.")
                }
            },
        )
    } catch (error: Exception) {
        target.delete()
        onError(error.message ?: "The automatic frame could not be captured.")
    }
}

private fun requiredPermissions(mode: CaptureMode): Array<String> = if (mode == CaptureMode.VIDEO) {
    arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
} else {
    arrayOf(Manifest.permission.RECORD_AUDIO)
}

private fun hasRequiredPermissions(context: Context, mode: CaptureMode): Boolean =
    requiredPermissions(mode).all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

private fun formatClock(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0) / 1_000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
