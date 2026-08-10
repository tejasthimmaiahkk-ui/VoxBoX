package me.thimmaiah.voxbox.session

import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.thimmaiah.voxbox.camera.VbCameraController
import me.thimmaiah.voxbox.ui.VbPill
import me.thimmaiah.voxbox.ui.VbPrimaryButton
import me.thimmaiah.voxbox.ui.vbLoop
import me.thimmaiah.voxbox.ui.vbPulse
import me.thimmaiah.voxbox.ui.theme.VbLiveBg
import me.thimmaiah.voxbox.ui.theme.VbLiveFg
import me.thimmaiah.voxbox.ui.theme.VbLiveFg2
import me.thimmaiah.voxbox.ui.theme.VbLiveFgBody
import me.thimmaiah.voxbox.ui.theme.VbLiveLine
import me.thimmaiah.voxbox.ui.theme.VbLiveSf
import me.thimmaiah.voxbox.ui.theme.VbMono
import me.thimmaiah.voxbox.ui.theme.VbRed
import me.thimmaiah.voxbox.ui.theme.VbShape
import me.thimmaiah.voxbox.ui.theme.VbSpace
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The capture surface, always dark in both themes.
 *
 * A white page behind a photograph of a lecture hall is unreadable, and this screen is looked at
 * sideways for an hour while something else has your attention. It is also the one screen where
 * leaving destroys work, so back is intercepted rather than allowed through.
 */
@Composable
fun LiveSessionScreen(
    viewModel: CaptureSessionViewModel,
    onFinished: (String?) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmingExit by remember { mutableStateOf(false) }

    BackHandler(enabled = state.isActive) { confirmingExit = true }

    // Once the session has stopped and the note has been written, hand over to the reader.
    LaunchedEffect(state.stage) {
        if (state.stage == LiveCaptureStage.STOPPED) onFinished(state.activeNoteId)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(VbLiveBg),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = VbSpace.screenH),
        ) {
            LiveHeader(
                elapsedMs = state.startedAt?.let { System.currentTimeMillis() - it } ?: 0L,
                revision = state.revision,
                onCollapse = { confirmingExit = true },
            )
            Spacer(Modifier.height(14.dp))

            if (state.mode == CaptureMode.VIDEO) {
                BoardPreview(
                    accepted = state.acceptedFrames,
                    skipped = state.skippedFrames,
                    intervalMs = state.frameIntervalMs,
                    onFrame = viewModel::onFrameCaptured,
                )
            } else {
                VoiceMeter()
            }

            Spacer(Modifier.height(16.dp))
            LivingNotePanel(
                markdown = state.generatedMarkdown,
                transcript = state.transcript,
                queued = state.pendingEvents,
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.height(12.dp))
            VbPrimaryButton(
                text = if (state.stage == LiveCaptureStage.STOPPING) {
                    "Finishing note…"
                } else {
                    "Stop and finish note"
                },
                onClick = { viewModel.stopSession() },
                enabled = state.stage == LiveCaptureStage.RUNNING,
                container = VbRed,
                content = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            )
            Spacer(Modifier.height(10.dp))
        }
    }

    if (confirmingExit) {
        AlertDialog(
            onDismissRequest = { confirmingExit = false },
            title = { Text("Stop capturing?") },
            text = {
                Text(
                    "Leaving ends the session. Audio already captured is saved and the note is " +
                        "written before it closes.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingExit = false
                    viewModel.stopSession()
                }) { Text("Stop and finish") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingExit = false }) { Text("Keep capturing") }
            },
        )
    }
}

@Composable
private fun LiveHeader(elapsedMs: Long, revision: Long, onCollapse: () -> Unit) {
    var now by remember { mutableStateOf(elapsedMs) }
    LaunchedEffect(Unit) {
        while (true) {
            now = elapsedMs
            kotlinx.coroutines.delay(1_000)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Collapse",
            style = MaterialTheme.typography.labelLarge,
            color = VbLiveFg2,
            modifier = Modifier
                .clip(VbShape.pill)
                .clickable(onClickLabel = "Stop the session and leave", onClick = onCollapse)
                .padding(vertical = 8.dp),
        )
        Spacer(Modifier.width(12.dp))
        VbPill("Live", VbRed, blinking = true)
        Spacer(Modifier.weight(1f))
        Text(
            text = formatClock(elapsedMs),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = VbMono),
            color = VbLiveFg,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "rev $revision",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = VbMono),
            color = VbLiveFg2,
        )
    }
}

@Composable
private fun BoardPreview(
    accepted: Int,
    skipped: Int,
    intervalMs: Long,
    onFrame: (File, Long) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val zoomController = remember { VbCameraController() }
    val cameraController = remember(context) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            isTapToFocusEnabled = true
        }
    }
    var captureInFlight by remember { mutableStateOf(false) }
    val zoom by zoomController.zoom.collectAsStateWithLifecycle()
    var lastZoomAt by remember { mutableStateOf(0L) }
    var readoutFaded by remember { mutableStateOf(true) }

    DisposableEffect(cameraController, lifecycleOwner, intervalMs) {
        val handler = Handler(Looper.getMainLooper())
        var sampler: Runnable? = null
        runCatching {
            cameraController.bindToLifecycle(lifecycleOwner)
            zoomController.bind(cameraController)
            zoomController.refreshRange()
            sampler = object : Runnable {
                override fun run() {
                    if (!captureInFlight) {
                        captureInFlight = true
                        captureFrame(
                            context = context,
                            controller = cameraController,
                            onSaved = { file, at ->
                                captureInFlight = false
                                onFrame(file, at)
                            },
                            onError = { captureInFlight = false },
                        )
                    }
                    handler.postDelayed(this, intervalMs)
                }
            }.also { handler.postDelayed(it, 1_200) }
        }
        onDispose {
            sampler?.let(handler::removeCallbacks)
            zoomController.unbind()
            cameraController.unbind()
        }
    }
    LaunchedEffect(zoom) {
        lastZoomAt = System.currentTimeMillis()
        readoutFaded = false
        kotlinx.coroutines.delay(1_200)
        readoutFaded = true
    }
    val readoutAlpha by animateFloatAsState(
        targetValue = if (readoutFaded) 0.4f else 1f,
        label = "zoom-readout",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoomChange, _ -> zoomController.nudge(zoomChange) }
            },
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    controller = cameraController
                }
            },
            update = { it.controller = cameraController },
            modifier = Modifier.fillMaxSize(),
        )

        // Framing guide, deliberately faint: it suggests where the board should sit without
        // implying the app only reads inside the rectangle.
        Box(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(20.dp)),
        )

        ScanBand()

        ZoomSlider(
            fraction = zoomController.fraction(),
            onFraction = zoomController::setFraction,
            modifier = Modifier.align(Alignment.CenterEnd),
        )

        Text(
            text = String.format(Locale.US, "%.1f×", zoom),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = VbMono),
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .graphicsLayer { alpha = readoutAlpha }
                .clip(VbShape.pill)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )

        Text(
            text = "$accepted kept · $skipped skipped",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(VbShape.pill)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/** A slow sweep that reads as "watching" without implying continuous upload. */
@Composable
private fun BoxScope.ScanBand() {
    val progress by vbLoop(-0.08f, 1.08f, 3400, restingValue = -0.08f, label = "scan")
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .graphicsLayer { translationY = progress * size.height * 6f }
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.06f), Color.Transparent),
                ),
            ),
    )
}

/**
 * Vertical zoom slider on the preview edge.
 *
 * Hand-built rather than a rotated [androidx.compose.material3.Slider]: rotating a slider 270°
 * leaves its hit box and its accessibility semantics in the original orientation, so it reports
 * the wrong axis to a screen reader and is hard to hit. The visible track is 8.dp inside a 44.dp
 * touch column.
 */
@Composable
private fun ZoomSlider(
    fraction: Float,
    onFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var height by remember { mutableStateOf(1f) }
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier
            .padding(end = 6.dp)
            .width(44.dp)
            .fillMaxHeight(0.6f)
            .semantics { contentDescription = "Camera zoom" }
            .pointerInput(Unit) {
                height = size.height.toFloat()
                detectDragGestures { change, _ ->
                    onFraction(1f - (change.position.y / height).coerceIn(0f, 1f))
                }
            },
    ) {
        Box(
            Modifier
                .width(8.dp)
                .fillMaxHeight()
                .clip(VbShape.pill)
                .background(Color.White.copy(alpha = 0.22f)),
        )
        Box(
            Modifier
                .width(8.dp)
                .fillMaxHeight(fraction.coerceIn(0.02f, 1f))
                .clip(VbShape.pill)
                .background(Color.White.copy(alpha = 0.85f)),
        )
        Box(
            Modifier
                .padding(bottom = (fraction.coerceIn(0f, 1f) * 0.9f * 100).dp)
                .size(30.dp)
                .clip(VbShape.pill)
                .background(Color.White),
        )
    }
}

/** Seven bars, staggered. A placeholder for real amplitude, not a fake meter to keep. */
@Composable
private fun VoiceMeter() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(VbLiveSf),
    ) {
        Spacer(Modifier.weight(1f))
        repeat(7) { index ->
            val scale by vbPulse(
                from = 0.22f,
                to = 1f,
                durationMillis = 1100,
                restingValue = 0.6f,
                label = "voice-bar-$index",
            )
            Box(
                Modifier
                    .width(8.dp)
                    .height(64.dp)
                    .graphicsLayer { scaleY = scale }
                    .clip(VbShape.pill)
                    .background(Color.White.copy(alpha = 0.55f)),
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun LivingNotePanel(
    markdown: String,
    transcript: List<LiveTranscriptLine>,
    queued: Int,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(VbLiveSf)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Living note",
                style = MaterialTheme.typography.titleMedium,
                color = VbLiveFg,
                modifier = Modifier.weight(1f),
            )
            VbPill(
                text = if (queued == 0) "up to date" else "$queued queued",
                color = if (queued == 0) VbLiveFg2 else MaterialTheme.colorScheme.primary,
                blinking = queued > 0,
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            item {
                Text(
                    text = markdown.ifBlank { "The note starts filling in after the first chunk." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = VbLiveFgBody,
                )
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(VbLiveLine))
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "EVIDENCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = VbLiveFg2,
                )
                Spacer(Modifier.height(8.dp))
            }
            items(transcript.reversed(), key = { it.id }) { line ->
                Row(Modifier.padding(vertical = 5.dp)) {
                    Text(
                        text = formatClock(line.startMs),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = VbMono),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = line.speakerId?.let { "Speaker $it" } ?: "Speaker ?",
                            style = MaterialTheme.typography.labelSmall,
                            color = VbLiveFg2,
                            fontWeight = if (line.primary) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = VbLiveFgBody,
                        )
                    }
                }
            }
        }
    }
}

private fun formatClock(millis: Long): String {
    val seconds = (millis / 1_000).coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
}

/** One JPEG to the cache. The frame filter decides whether it is ever uploaded. */
private fun captureFrame(
    context: android.content.Context,
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
                override fun onImageSaved(result: ImageCapture.OutputFileResults) =
                    onSaved(target, capturedAt)

                override fun onError(exception: ImageCaptureException) {
                    target.delete()
                    onError(exception.message ?: "The frame could not be captured.")
                }
            },
        )
    } catch (error: Exception) {
        target.delete()
        onError(error.message ?: "The frame could not be captured.")
    }
}
