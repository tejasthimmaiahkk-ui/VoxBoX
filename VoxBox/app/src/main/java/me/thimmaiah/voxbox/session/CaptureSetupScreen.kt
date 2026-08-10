package me.thimmaiah.voxbox.session

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.thimmaiah.voxbox.ui.VbCard
import me.thimmaiah.voxbox.ui.VbChoiceTile
import me.thimmaiah.voxbox.ui.VbExpandableCard
import me.thimmaiah.voxbox.ui.VbIcons
import me.thimmaiah.voxbox.ui.VbPrimaryButton
import me.thimmaiah.voxbox.ui.VbSectionHeader
import me.thimmaiah.voxbox.ui.VbSegmented
import me.thimmaiah.voxbox.ui.theme.LocalVbStatus
import me.thimmaiah.voxbox.ui.theme.VbShape
import me.thimmaiah.voxbox.ui.theme.VbSpace
import kotlin.math.roundToInt

private enum class Destination { NEW, CONTINUE }

/**
 * The four decisions made before a lecture: what to listen to, where it goes, how it is written,
 * and — in board mode only — how often to look.
 *
 * Everything that used to sit here and is not one of those four now lives in Settings. Recovery,
 * connection status, review flags and evidence copy are all things you deal with before or after
 * a lecture, and putting them in front of the start button made starting a lecture a form.
 */
@Composable
fun CaptureSetupScreen(
    viewModel: CaptureSessionViewModel,
    contentPadding: PaddingValues,
    onStarted: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val status = LocalVbStatus.current
    var destination by remember { mutableStateOf(Destination.NEW) }
    var cameraOptionsOpen by remember { mutableStateOf(false) }

    val needsCamera = state.mode == CaptureMode.VIDEO
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        micGranted = result[Manifest.permission.RECORD_AUDIO] ?: micGranted
        cameraGranted = result[Manifest.permission.CAMERA] ?: cameraGranted
    }
    val ready = micGranted && (!needsCamera || cameraGranted)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = VbSpace.screenH)
            .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Session options",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(VbSpace.section))

        // 1 — Mode
        VbSectionHeader("Mode")
        Row(horizontalArrangement = Arrangement.spacedBy(VbSpace.gap)) {
            VbChoiceTile(
                title = "Voice",
                body = "Audio only, in 20-second chunks.",
                icon = VbIcons.Mic,
                selected = state.mode == CaptureMode.VOICE,
                onClick = { viewModel.setMode(CaptureMode.VOICE) },
                modifier = Modifier.weight(1f),
            )
            VbChoiceTile(
                title = "Live board",
                body = "Audio, plus the board through the camera.",
                icon = VbIcons.Camera,
                selected = state.mode == CaptureMode.VIDEO,
                onClick = { viewModel.setMode(CaptureMode.VIDEO) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(VbSpace.section))

        // 2 — Where it goes
        VbSectionHeader("Where it goes")
        VbSegmented(
            options = listOf(Destination.NEW, Destination.CONTINUE),
            selected = destination,
            label = { if (it == Destination.NEW) "New note" else "Continue" },
            onSelect = { choice ->
                destination = choice
                if (choice == Destination.NEW) viewModel.selectExistingNote(null)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(VbSpace.gap))
        AnimatedContent(
            targetState = destination,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label = "destination",
        ) { choice ->
            when (choice) {
                Destination.NEW -> OutlinedTextField(
                    value = state.noteTitle,
                    onValueChange = viewModel::setNoteTitle,
                    placeholder = { Text("Note title") },
                    singleLine = true,
                    shape = VbShape.pill,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                Destination.CONTINUE -> Column {
                    if (state.notes.isEmpty()) {
                        Text(
                            text = "No notes yet. Start a new one and it will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = status.fg2,
                        )
                    }
                    state.notes.take(5).forEach { note ->
                        NoteRadioRow(
                            title = note.title.ifBlank { "Untitled note" },
                            selected = state.selectedNoteId == note.id,
                            onClick = { viewModel.selectExistingNote(note.id) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(VbSpace.section))

        // 3 — How it is written
        VbSectionHeader("How it is written")
        VbChoiceTile(
            title = "Runnable notes",
            body = "Structured study notes. Repetition is merged and tangents dropped, but nothing " +
                "the AI disagrees with is silently changed — it becomes a review flag.",
            selected = state.notePolicy == CaptureNotePolicy.RUNNABLE,
            onClick = { viewModel.setNotePolicy(CaptureNotePolicy.RUNNABLE) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(VbSpace.gap))
        VbChoiceTile(
            title = "Verbatim",
            body = "A faithful readable transcript in order, with nothing removed.",
            selected = state.notePolicy == CaptureNotePolicy.VERBATIM,
            onClick = { viewModel.setNotePolicy(CaptureNotePolicy.VERBATIM) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(VbSpace.gap))
        VbSegmented(
            options = listOf(NoteDetail.CONCISE, NoteDetail.STANDARD, NoteDetail.DETAILED),
            selected = state.noteDetail,
            label = {
                when (it) {
                    NoteDetail.CONCISE -> "Short"
                    NoteDetail.STANDARD -> "Balanced"
                    NoteDetail.DETAILED -> "Elaborate"
                }
            },
            onSelect = viewModel::setNoteDetail,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(VbSpace.gap))
        OutlinedTextField(
            value = state.customInstruction,
            onValueChange = viewModel::setCustomInstruction,
            placeholder = { Text("Custom instruction (optional)") },
            shape = VbShape.card,
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(VbSpace.section))

        // 4 — Camera options, board mode only
        AnimatedVisibility(visible = needsCamera) {
            Column {
                VbSectionHeader("Camera")
                VbExpandableCard(
                    title = "Sampling",
                    summary = "Every ${state.frameIntervalMs / 1000}s · " +
                        "${(state.changeThreshold * 100).roundToInt()}% change to keep a frame",
                    expanded = cameraOptionsOpen,
                    onExpandedChange = { cameraOptionsOpen = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SliderRow(
                        label = "Interval",
                        value = (state.frameIntervalMs / 1000).toFloat(),
                        range = 2f..30f,
                        suffix = "s",
                        onChange = { viewModel.setFrameIntervalMillis((it.roundToInt() * 1000).toLong()) },
                    )
                    Spacer(Modifier.height(10.dp))
                    SliderRow(
                        label = "Sensitivity",
                        value = (state.changeThreshold * 100).toFloat(),
                        range = 2f..30f,
                        suffix = "%",
                        onChange = { viewModel.setChangeThreshold(it.roundToInt() / 100.0) },
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "A change must still be there on the next sample before it is sent, " +
                            "so someone walking past the board never costs a call. Frames are " +
                            "deleted on the phone once their note update commits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = status.fg2,
                    )
                }
                Spacer(Modifier.height(VbSpace.section))
            }
        }

        // 5 — Start
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(VbShape.pill)
                    .background(if (ready) status.saved else status.review),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when {
                    ready && needsCamera -> "Microphone and camera ready"
                    ready -> "Microphone ready"
                    else -> "Permission needed before capture can start"
                },
                style = MaterialTheme.typography.bodySmall,
                color = status.fg2,
            )
        }
        Spacer(Modifier.height(12.dp))
        VbPrimaryButton(
            text = if (ready) "Start capture" else "Grant permission",
            onClick = {
                if (ready) {
                    viewModel.startSession()
                    onStarted()
                } else {
                    permissionLauncher.launch(
                        if (needsCamera) {
                            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                        } else {
                            arrayOf(Manifest.permission.RECORD_AUDIO)
                        },
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let { message ->
            Spacer(Modifier.height(10.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = status.danger)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun NoteRadioRow(title: String, selected: Boolean, onClick: () -> Unit) {
    VbCard(
        onClick = onClick,
        borderColor = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            LocalVbStatus.current.line
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(18.dp)
                    .clip(VbShape.pill)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
            ) {
                if (selected) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(VbShape.pill)
                            .background(MaterialTheme.colorScheme.onPrimary),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    suffix: String,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${value.roundToInt()}$suffix",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
