package me.thimmaiah.voxbox.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.thimmaiah.voxbox.data.SettingsRepository
import me.thimmaiah.voxbox.data.VbSettings
import me.thimmaiah.voxbox.data.VbThemeMode
import me.thimmaiah.voxbox.notes.NoteEntity
import me.thimmaiah.voxbox.notes.NoteLibraryViewModel
import me.thimmaiah.voxbox.session.CaptureMode
import me.thimmaiah.voxbox.session.CaptureSessionViewModel
import me.thimmaiah.voxbox.ui.VbCard
import me.thimmaiah.voxbox.ui.VbEyebrow
import me.thimmaiah.voxbox.ui.VbIconButton
import me.thimmaiah.voxbox.ui.VbIcons
import me.thimmaiah.voxbox.ui.VbInitial
import me.thimmaiah.voxbox.ui.VbNotice
import me.thimmaiah.voxbox.ui.VbSegmented
import me.thimmaiah.voxbox.ui.vbBlockEnter
import me.thimmaiah.voxbox.ui.vbLoop
import me.thimmaiah.voxbox.ui.theme.LocalVbStatus
import me.thimmaiah.voxbox.ui.theme.VbShape
import me.thimmaiah.voxbox.ui.theme.VbSpace
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    captureViewModel: CaptureSessionViewModel,
    libraryViewModel: NoteLibraryViewModel,
    settingsRepository: SettingsRepository,
    scope: CoroutineScope,
    contentPadding: PaddingValues,
    onOpenCapture: () -> Unit,
    onOpenNote: (String) -> Unit,
    onStarted: () -> Unit,
) {
    val capture by captureViewModel.uiState.collectAsStateWithLifecycle()
    val library by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = VbSettings())
    val scroll = rememberScrollState()
    val visible = remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .statusBarsPadding()
            .padding(horizontal = VbSpace.screenH)
            .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        AppRow(settings.theme) { next ->
            scope.launch { settingsRepository.setTheme(next) }
        }

        Block(visible, 0) {
            Spacer(Modifier.height(18.dp))
            VbEyebrow(todayLabel())
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Ready to capture",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Block(visible, 1) {
            Spacer(Modifier.height(20.dp))
            StartCard(
                mode = capture.mode,
                onModeChange = captureViewModel::setMode,
                onStart = {
                    captureViewModel.startSession()
                    onStarted()
                },
                onOptions = onOpenCapture,
            )
        }

        // A flag is a decision the student still owes the note. Surfacing the count on Home is
        // the only place it competes with nothing else for attention.
        val flagged = library.activeBlocks.count { it.content.contains("voxbox-review:start") }
        if (flagged > 0) {
            Block(visible, 2) {
                Spacer(Modifier.height(20.dp))
                VbNotice(
                    title = "$flagged note section needs your review",
                    body = "The AI disagreed with something that was captured. Nothing was changed.",
                    tone = LocalVbStatus.current.review,
                )
            }
        }

        val recent = library.allNotes.take(2)
        if (recent.isNotEmpty()) {
            Block(visible, 2) {
                Spacer(Modifier.height(24.dp))
                VbEyebrow("Continue a note")
                Spacer(Modifier.height(10.dp))
                recent.forEach { note ->
                    RecentNoteRow(note) { onOpenNote(note.id) }
                    Spacer(Modifier.height(VbSpace.gap))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Block(visible: MutableState<Boolean>, index: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(visible = visible.value, enter = vbBlockEnter(index)) {
        Column { content() }
    }
}

@Composable
private fun AppRow(theme: VbThemeMode, onToggleTheme: (VbThemeMode) -> Unit) {
    val dark = theme == VbThemeMode.Dark
    val rotation by animateFloatAsState(
        targetValue = if (dark) 35f else 0f,
        animationSpec = spring(),
        label = "theme-icon",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "VoxBox",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Box(Modifier.graphicsLayer { rotationZ = rotation }) {
            VbIconButton(
                icon = if (dark) VbIcons.Sun else VbIcons.Moon,
                contentDescription = if (dark) "Switch to light theme" else "Switch to dark theme",
                onClick = { onToggleTheme(if (dark) VbThemeMode.Light else VbThemeMode.Dark) },
                tint = LocalVbStatus.current.fg2,
            )
        }
    }
}

@Composable
private fun StartCard(
    mode: CaptureMode,
    onModeChange: (CaptureMode) -> Unit,
    onStart: () -> Unit,
    onOptions: () -> Unit,
) {
    VbCard(
        shape = VbShape.cardL,
        contentPadding = PaddingValues(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        VbSegmented(
            options = listOf(CaptureMode.VOICE, CaptureMode.VIDEO),
            selected = mode,
            label = { if (it == CaptureMode.VOICE) "Voice" else "Board" },
            onSelect = onModeChange,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            RecordButton(onStart)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = if (mode == CaptureMode.VOICE) {
                "Continuous audio in 20-second chunks, transcribed with per-chunk speaker labels."
            } else {
                "Audio, plus the board through the camera. Frames that have not changed never leave the phone."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = LocalVbStatus.current.fg2,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Session options",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(VbShape.pill)
                .clickable(onClickLabel = "Session options", onClick = onOptions)
                .padding(vertical = 6.dp),
        )
    }
}

/** 92.dp circle with two rings breathing out behind it. */
@Composable
private fun RecordButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "record-press",
    )
    Box(contentAlignment = Alignment.Center) {
        Ring(delayMillis = 0)
        Ring(delayMillis = 900)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(92.dp)
                .graphicsLayer { scaleX = press; scaleY = press }
                .clip(VbShape.pill)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClickLabel = "Start capture",
                    onClick = onClick,
                ),
        ) {
            Icon(
                painter = painterResource(VbIcons.Mic),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@Composable
private fun Ring(delayMillis: Int) {
    val progress by vbLoop(0f, 1f, 2600, delayMillis, restingValue = 0f, label = "ring")
    val primary = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .size(92.dp)
            .graphicsLayer {
                val scale = 1f + progress * 0.85f
                scaleX = scale
                scaleY = scale
                alpha = 0.55f * (1f - progress)
            }
            .clip(VbShape.pill)
            .background(primary),
    )
}

@Composable
private fun RecentNoteRow(note: NoteEntity, onClick: () -> Unit) {
    VbCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VbInitial(note.title, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { "Untitled note" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Tap to open",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalVbStatus.current.fg2,
                )
            }
            Icon(
                painter = painterResource(VbIcons.ChevronRight),
                contentDescription = null,
                tint = LocalVbStatus.current.fg3,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun todayLabel(): String =
    SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(Date())
