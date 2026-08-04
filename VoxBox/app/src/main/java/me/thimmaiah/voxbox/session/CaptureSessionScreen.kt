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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.util.Locale
import me.thimmaiah.voxbox.audio.SpeakerFocusStatus
import me.thimmaiah.voxbox.ui.MarkdownNotePreview
import me.thimmaiah.voxbox.ui.VoxBoxSectionCard
import me.thimmaiah.voxbox.ui.VoxBoxSpacing
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
) {
    var folderName by rememberSaveable { mutableStateOf("") }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item("promise") {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("One session, one living note", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Voice mode records continuous audio chunks. Video mode listens too, while changed board frames are filtered locally and useful diagrams are cropped into the note.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        if (state.stage == LiveCaptureStage.STOPPED) {
            item("finished") {
                VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                    VoxBoxStatusPill("SAVED", tone = VoxBoxStatusTone.Success)
                    Text(state.status, style = MaterialTheme.typography.bodyLarge)
                    MarkdownNotePreview(state.generatedMarkdown)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("New session") }
                        Button(
                            onClick = { state.activeNoteId?.let(onOpenNote) },
                            enabled = state.activeNoteId != null,
                            modifier = Modifier.weight(1f),
                        ) { Text("Open note") }
                    }
                }
            }
        }
        item("mode") {
            VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                Text("1 · Capture mode", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChoiceButton(
                        label = "Voice",
                        detail = "Audio only",
                        selected = state.mode == CaptureMode.VOICE,
                        onClick = { onMode(CaptureMode.VOICE) },
                        modifier = Modifier.weight(1f),
                    )
                    ChoiceButton(
                        label = "Live board",
                        detail = "Camera + audio",
                        selected = state.mode == CaptureMode.VIDEO,
                        onClick = { onMode(CaptureMode.VIDEO) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item("note") {
            VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                Text("2 · Note destination", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = state.noteTitle,
                    onValueChange = onTitle,
                    label = { Text("New note title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.notes.isNotEmpty()) {
                    Text("Or continue a recent note", style = MaterialTheme.typography.labelLarge)
                    state.notes.take(4).forEach { note ->
                        OutlinedButton(
                            onClick = { onSelectNote(note.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (state.selectedNoteId == note.id) "✓ ${note.title}" else note.title)
                        }
                    }
                    if (state.selectedNoteId != null) {
                        OutlinedButton(onClick = { onSelectNote(null) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Use a new note instead")
                        }
                    }
                }
            }
        }
        item("policy") {
            VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                Text("3 · Note policy", style = MaterialTheme.typography.titleLarge)
                ChoiceButton(
                    label = "Runnable notes",
                    detail = "Structured, deduplicated, teacher-focused; conflicts are flagged, never silently replaced.",
                    selected = state.notePolicy == CaptureNotePolicy.RUNNABLE,
                    onClick = { onPolicy(CaptureNotePolicy.RUNNABLE) },
                    modifier = Modifier.fillMaxWidth(),
                )
                ChoiceButton(
                    label = "Verbatim",
                    detail = "Keeps every diarized utterance in timestamp order without AI summarization.",
                    selected = state.notePolicy == CaptureNotePolicy.VERBATIM,
                    onClick = { onPolicy(CaptureNotePolicy.VERBATIM) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (state.mode == CaptureMode.VIDEO) {
            item("video-settings") {
                VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                    Text("4 · Camera efficiency", style = MaterialTheme.typography.titleLarge)
                    Text("Capture every ${state.frameIntervalMs / 1_000} seconds", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = state.frameIntervalMs.toFloat(),
                        onValueChange = { onFrameInterval((it / 1_000).toLong() * 1_000) },
                        valueRange = 2_000f..30_000f,
                        steps = 27,
                    )
                    Text(
                        "Change sensitivity: ${String.format(Locale.US, "%.0f", state.changeThreshold * 100)}%",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Slider(
                        value = state.changeThreshold.toFloat(),
                        onValueChange = { onThreshold(it.toDouble()) },
                        valueRange = 0.02f..0.30f,
                    )
                    Text(
                        "Similar frames are deleted locally immediately. Accepted raw frames are deleted only after note updates and diagram crops commit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item("organization") {
            VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                Text("${if (state.mode == CaptureMode.VIDEO) "5" else "4"} · Context & organization", style = MaterialTheme.typography.titleLarge)
                Text("Folder", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = { onSelectFolder(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.selectedFolderId == null) "✓ No folder" else "No folder")
                }
                state.folders.take(4).forEach { folder ->
                    OutlinedButton(onClick = { onSelectFolder(folder.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.selectedFolderId == folder.id) "✓ ${folder.name}" else folder.name)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
                HorizontalDivider()
                Text("Syllabus context", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = { onSelectSyllabus(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.selectedSyllabusId == null) "✓ No syllabus" else "No syllabus")
                }
                state.syllabi.take(4).forEach { syllabus ->
                    OutlinedButton(onClick = { onSelectSyllabus(syllabus.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.selectedSyllabusId == syllabus.id) "✓ ${syllabus.title}" else syllabus.title)
                    }
                }
                FilledTonalButton(onClick = onImportSyllabus, modifier = Modifier.fillMaxWidth()) {
                    Text("Import .md or .txt syllabus")
                }
                Text(
                    "Syllabus text stays local until selected for a session and is treated as context—not proof of what the speaker said.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item("start") {
            VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                VoxBoxStatusPill(
                    label = if (permissionsGranted) "PERMISSIONS READY" else "PERMISSION NEEDED",
                    tone = if (permissionsGranted) VoxBoxStatusTone.Success else VoxBoxStatusTone.Warning,
                )
                Button(
                    onClick = onStart,
                    enabled = state.stage != LiveCaptureStage.STARTING,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Start continuous ${state.mode.name.lowercase()} session" },
                ) {
                    Text(if (permissionsGranted) "Start live session" else "Allow & start")
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text(state.status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ChoiceButton(
    label: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = if (selected) {
        CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)
    } else {
        CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHighest)
    }
    Card(onClick = onClick, modifier = modifier, colors = colors, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (selected) "✓ $label" else label, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item("live-status") {
            Card(
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VoxBoxStatusPill(
                            if (state.stage == LiveCaptureStage.STOPPING) "FINISHING" else "LIVE",
                            tone = VoxBoxStatusTone.Warning,
                        )
                        Text("Revision ${state.revision}", style = MaterialTheme.typography.labelLarge)
                    }
                    Text(
                        if (state.mode == CaptureMode.VIDEO) "Camera + continuous audio" else "Continuous audio",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        "Foreground-only: keep Live open. Leaving this screen stops capture and drains saved audio before finishing the note.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(state.status, style = MaterialTheme.typography.bodyMedium)
                    if (state.pendingAudioChunks > 0 || state.pendingFrames > 0) {
                        Text(
                            "Queued independently: ${state.pendingAudioChunks} audio · ${state.pendingFrames} frame(s)",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    if (state.retainedAudioChunks > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                "${state.retainedAudioChunks} unrecovered WAV file(s) are retained privately on this phone.",
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Button(
                        onClick = onStop,
                        enabled = state.stage == LiveCaptureStage.RUNNING,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Stop and finish note") }
                }
            }
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
                    Text("Frame efficiency", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Counter("Accepted", state.acceptedFrames)
                        Counter("Skipped", state.skippedFrames)
                        Counter("Done", state.processedFrames)
                    }
                    state.lastFrameChangeScore?.let {
                        Text(
                            "Last change score: ${String.format(Locale.US, "%.1f", it * 100)}%",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        item("speaker") {
            VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Speaker focus", style = MaterialTheme.typography.titleLarge)
                    VoxBoxStatusPill(
                        state.speakerFocus.status.name,
                        tone = when (state.speakerFocus.status) {
                            SpeakerFocusStatus.FOCUSED, SpeakerFocusStatus.MANUAL -> VoxBoxStatusTone.Success
                            SpeakerFocusStatus.AMBIGUOUS, SpeakerFocusStatus.UNAVAILABLE -> VoxBoxStatusTone.Warning
                            SpeakerFocusStatus.LEARNING -> VoxBoxStatusTone.Accent
                        },
                    )
                }
                Text(state.speakerFocus.reason, style = MaterialTheme.typography.bodyMedium)
                state.speakerFocus.selectedSpeakerId?.let {
                    Text("Dominant label in latest chunk: $it", style = MaterialTheme.typography.labelLarge)
                }
                val speakers = state.latestChunkSpeakerIds.take(4)
                if (speakers.isNotEmpty()) {
                    Text("Manual mark for latest chunk", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Labels such as A/B are local to one transcription request and are never assumed to identify the same person later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    speakers.forEach { speaker ->
                        OutlinedButton(
                            onClick = { onSelectPrimarySpeaker(speaker) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (state.speakerFocus.selectedSpeakerId == speaker) "✓ Speaker $speaker" else "Use speaker $speaker")
                        }
                    }
                    OutlinedButton(onClick = { onSelectPrimarySpeaker(null) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Use automatic choice for this chunk")
                    }
                }
            }
        }
        item("note-preview") {
            VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Structured note", style = MaterialTheme.typography.titleLarge)
                    if (state.pendingEvents > 0) VoxBoxStatusPill("${state.pendingEvents} QUEUED")
                }
                MarkdownNotePreview(state.generatedMarkdown.ifBlank { state.existingNoteMarkdown })
            }
        }
        item("transcript") {
            VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                Text("Evidence transcript", style = MaterialTheme.typography.titleLarge)
                if (state.transcript.isEmpty()) {
                    Text("Completed diarized segments will appear after each audio chunk.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    state.transcript.takeLast(12).forEach { line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(formatClock(line.startMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text(line.speakerId?.let { "Speaker $it" }.orEmpty(), style = MaterialTheme.typography.labelMedium)
                                Text(line.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
        if (state.corrections.isNotEmpty() || state.warnings.isNotEmpty() || state.error != null) {
            item("review") {
                VoxBoxSectionCard(Modifier.fillMaxWidth()) {
                    Text("Review flags", style = MaterialTheme.typography.titleLarge)
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    state.corrections.forEach { correction ->
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Captured: ${correction.captured}", style = MaterialTheme.typography.bodyMedium)
                                Text("Suggested annotation: ${correction.suggested}", style = MaterialTheme.typography.bodyMedium)
                                Text(correction.reason, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    state.warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
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
    Card(shape = MaterialTheme.shapes.extraLarge) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(MaterialTheme.shapes.extraLarge)
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
                    modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        "Auto frame every ${intervalMs / 1_000}s · audio remains on",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            cameraError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
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

@Composable
private fun Counter(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
