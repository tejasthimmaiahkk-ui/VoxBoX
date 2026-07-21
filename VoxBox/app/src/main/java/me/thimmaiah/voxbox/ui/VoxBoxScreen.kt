package me.thimmaiah.voxbox.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import me.thimmaiah.voxbox.speech.VoiceCaptureUiState
import me.thimmaiah.voxbox.speech.VoiceCaptureViewModel
import me.thimmaiah.voxbox.notes.NoteLibraryUiState
import me.thimmaiah.voxbox.notes.NoteLibraryViewModel
import me.thimmaiah.voxbox.notes.NoteBlockEntity
import me.thimmaiah.voxbox.notes.ReadOnlyNoteBlock
import me.thimmaiah.voxbox.notes.toReadOnlyBlockOrNull
import me.thimmaiah.voxbox.voxscript.VoxScriptResult

private val VoxBlue = Color(0xFF2563EB)
private val VoxBlueSoft = Color(0xFFEAF2FF)

@Composable
fun VoxBoxScreen(viewModel: VoiceCaptureViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val noteLibraryViewModel: NoteLibraryViewModel = viewModel()
    val noteLibraryState by noteLibraryViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::onPermissionChanged,
    )
    val permissionGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(permissionGranted) {
        viewModel.onPermissionChanged(permissionGranted)
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(state)
            NoteLibraryCard(
                state = noteLibraryState,
                onCreateNote = noteLibraryViewModel::createNote,
                onOpenNote = noteLibraryViewModel::openNote,
            )
            PermissionAndRecognizerCard(state)
            CaptureCard(
                state = state,
                onPrimaryAction = {
                    when {
                        !state.permissionGranted -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        state.isListening -> viewModel.stopListening()
                        else -> viewModel.startListening()
                    }
                },
                onCancel = viewModel::cancelListening,
            )
            TranscriptCard(state)
            state.structuredResult?.let { result ->
                StructuredPreview(result)
                Button(
                    onClick = { noteLibraryViewModel.savePreview(result) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = result !is VoxScriptResult.InvalidCommand,
                ) {
                    Text("Save preview as a note block")
                }
            }
            OutlinedButton(
                onClick = viewModel::loadExample,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Load the 25% wheat chart example")
            }
            Text(
                text = "AI summarization is not enabled in this native baseline.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun NoteLibraryCard(
    state: NoteLibraryUiState,
    onCreateNote: () -> Unit,
    onOpenNote: (String) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Local note library", fontWeight = FontWeight.Bold)
                    Text(
                        "${state.notes.size} saved note${if (state.notes.size == 1) "" else "s"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = onCreateNote) {
                    Text("New note")
                }
            }
            Text(state.status, color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.notes.take(3).forEach { note ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(note.title, fontWeight = FontWeight.Medium)
                    OutlinedButton(onClick = { onOpenNote(note.id) }) {
                        Text("Open")
                    }
                }
            }
            val activeNote = state.notes.firstOrNull { it.id == state.activeNoteId }
            if (activeNote != null) {
                SavedNoteDetail(activeNote.title, state.activeBlocks)
            }
        }
    }
}

@Composable
private fun SavedNoteDetail(title: String, blocks: List<NoteBlockEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Saved note", color = VoxBlue, fontWeight = FontWeight.Bold)
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black)
        if (blocks.isEmpty()) {
            Text("No saved blocks yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            blocks.forEach { block ->
                ReadOnlyBlock(block.toReadOnlyBlockOrNull(), block.position)
            }
        }
        Text(
            "Read-only reopening is verified here; block editing is the next editor milestone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReadOnlyBlock(block: ReadOnlyNoteBlock?, position: Int) {
    when (block) {
        is ReadOnlyNoteBlock.Paragraph -> Text(block.text)
        is ReadOnlyNoteBlock.Heading -> Text(block.text, fontSize = 28.sp, fontWeight = FontWeight.Black)
        is ReadOnlyNoteBlock.BulletPoint -> Text("• ${block.text}", fontSize = 20.sp)
        is ReadOnlyNoteBlock.PieChart -> PieChartVisual(block.percentage, block.color, block.label)
        null -> Text(
            "Saved block ${position + 1} cannot be displayed because its data is incomplete.",
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun Header(state: VoiceCaptureUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("VoxBox", fontSize = 36.sp, fontWeight = FontWeight.Black)
            Text(
                "Speak structure. Keep control.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(color = VoxBlueSoft, shape = RoundedCornerShape(50)) {
            Text(
                text = if (state.usesOnDeviceRecognizer) "ON-DEVICE" else "NATIVE",
                color = VoxBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun PermissionAndRecognizerCard(state: VoiceCaptureUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Native speech status", fontWeight = FontWeight.Bold)
            Text(state.status, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                when {
                    !state.recognitionAvailable -> "No Android speech-recognition service was detected."
                    state.usesOnDeviceRecognizer -> "Android on-device recognizer selected."
                    else -> "Android system recognizer selected; recognition may use the network service."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CaptureCard(
    state: VoiceCaptureUiState,
    onPrimaryAction: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = VoxBlueSoft,
            contentColor = Color(0xFF111827),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Button(
                onClick = onPrimaryAction,
                enabled = state.recognitionAvailable || !state.permissionGranted,
                modifier = Modifier
                    .size(112.dp)
                    .semantics {
                        contentDescription = when {
                            !state.permissionGranted -> "Grant microphone permission"
                            state.isListening -> "Stop listening"
                            else -> "Start listening"
                        }
                    },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isListening) MaterialTheme.colorScheme.error else VoxBlue,
                    contentColor = Color.White,
                ),
            ) {
                Text(if (state.isListening) "STOP" else "MIC", fontWeight = FontWeight.Black)
            }
            Text(
                when {
                    !state.permissionGranted -> "Tap to allow microphone access"
                    state.isListening -> "Tap STOP when the command is complete"
                    else -> "Tap MIC, then speak dictation or a VoxScript command"
                },
                fontWeight = FontWeight.Medium,
            )
            if (state.isListening) {
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF374151)),
                ) {
                    Text("Cancel session")
                }
            }
        }
    }
}

@Composable
private fun TranscriptCard(state: VoiceCaptureUiState) {
    val visibleText = state.partialTranscript.ifBlank { state.finalTranscript }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Transcript", fontWeight = FontWeight.Bold)
            Text(
                text = visibleText.ifBlank { "Your recognized speech will appear here." },
                fontSize = 20.sp,
                color = if (visibleText.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StructuredPreview(result: VoxScriptResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("VoxScript preview", color = VoxBlue, fontWeight = FontWeight.Bold)
            when (result) {
                is VoxScriptResult.PlainDictation -> {
                    Text("Paragraph", fontWeight = FontWeight.Bold)
                    Text(result.sourceText)
                }
                is VoxScriptResult.Heading -> Text(
                    result.text,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                )
                is VoxScriptResult.BulletPoint -> Text("• ${result.text}", fontSize = 22.sp)
                is VoxScriptResult.PieChart -> PieChartPreview(result)
                is VoxScriptResult.InvalidCommand -> {
                    Text("Command needs correction", fontWeight = FontWeight.Bold)
                    Text(result.reason, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun PieChartPreview(result: VoxScriptResult.PieChart) {
    PieChartVisual(result.percentage, result.color, result.label)
}

@Composable
private fun PieChartVisual(percentage: Int, colorName: String, label: String) {
    val sliceColor = namedColor(colorName)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.White),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color.White)
                drawArc(
                    color = sliceColor,
                    startAngle = -90f,
                    sweepAngle = 360f * percentage / 100f,
                    useCenter = true,
                )
                drawCircle(Color(0xFF9CA3AF), style = Stroke(width = 2.dp.toPx()))
            }
        }
        Column {
            Text(label.replaceFirstChar { it.titlecase() }, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("${percentage}%", fontSize = 34.sp, fontWeight = FontWeight.Black, color = sliceColor)
            Text("${colorName} • remainder white", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun namedColor(name: String): Color = when (name.lowercase()) {
    "red" -> Color(0xFFE53935)
    "orange" -> Color(0xFFFB8C00)
    "yellow" -> Color(0xFFF6C945)
    "green" -> Color(0xFF43A047)
    "blue" -> Color(0xFF1E88E5)
    "purple" -> Color(0xFF8E24AA)
    "pink" -> Color(0xFFD81B60)
    "black" -> Color.Black
    else -> Color.White
}
