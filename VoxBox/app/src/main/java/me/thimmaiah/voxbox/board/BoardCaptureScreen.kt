package me.thimmaiah.voxbox.board

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.util.Locale
import me.thimmaiah.voxbox.ui.VoxBoxBanner
import me.thimmaiah.voxbox.ui.VoxBoxIcons
import me.thimmaiah.voxbox.ui.VoxBoxLayout
import me.thimmaiah.voxbox.ui.VoxBoxSectionCard
import me.thimmaiah.voxbox.ui.VoxBoxSectionHeader
import me.thimmaiah.voxbox.ui.VoxBoxSpacing
import me.thimmaiah.voxbox.ui.VoxBoxStatusPill
import me.thimmaiah.voxbox.ui.VoxBoxStatusTone

private const val PREVIEW_DESCRIPTION = "Live board camera preview"
private const val CAPTURE_LABEL = "Capture board frame"
private const val RETAKE_LABEL = "Retake frame"
private const val SAVE_LABEL = "Save board capture as note"

@Composable
fun BoardCaptureScreen(
    onSaveExtraction: (BoardExtraction) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BoardCaptureViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var permissionGranted by remember {
        mutableStateOf(context.hasCameraPermission())
    }
    var requestedPermission by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionGranted = granted
        viewModel.onPermissionChanged(granted)
    }
    val cameraController = remember(context) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }

    LaunchedEffect(permissionGranted) {
        viewModel.onPermissionChanged(permissionGranted)
    }
    LaunchedEffect(uiState.stage) {
        scrollState.scrollTo(0)
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = context.hasCameraPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(Unit) {
        if (!permissionGranted && !requestedPermission) {
            requestedPermission = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val cameraShouldBeBound = permissionGranted &&
        uiState.stage in setOf(BoardCaptureStage.LIVE_PREVIEW, BoardCaptureStage.CAPTURING)
    DisposableEffect(cameraController, lifecycleOwner, cameraShouldBeBound) {
        if (cameraShouldBeBound) {
            try {
                cameraController.bindToLifecycle(lifecycleOwner)
                viewModel.onCameraReady()
            } catch (error: Exception) {
                viewModel.onCameraError(
                    error.message?.takeIf(String::isNotBlank)
                        ?: "The rear camera could not be started on this device.",
                )
            }
        }
        onDispose {
            cameraController.unbind()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                horizontal = VoxBoxLayout.compactScreenPadding,
                vertical = VoxBoxSpacing.small,
            ),
        verticalArrangement = Arrangement.spacedBy(VoxBoxLayout.sectionSpacing),
    ) {
        StatusHeader(uiState)

        when {
            !permissionGranted -> PermissionCard(
                onRequestPermission = {
                    requestedPermission = true
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
            )

            uiState.stage == BoardCaptureStage.REVIEW ||
                uiState.stage == BoardCaptureStage.SAVED -> ReviewCard(
                state = uiState,
                viewModel = viewModel,
                onRetake = viewModel::retake,
                onSave = {
                    viewModel.saveReviewedExtraction()?.let(onSaveExtraction)
                },
            )

            uiState.stage == BoardCaptureStage.PROCESSING -> ProcessingCard()

            else -> {
                CameraPreview(
                    cameraController = cameraController,
                    modifier = Modifier.fillMaxWidth(),
                )
                uiState.errorMessage?.let { errorMessage ->
                    VoxBoxBanner(
                        title = "The last frame failed",
                        message = errorMessage,
                        tone = VoxBoxStatusTone.Error,
                    )
                }
                if (uiState.stage == BoardCaptureStage.ERROR) {
                    OutlinedButton(
                        onClick = viewModel::retake,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = RETAKE_LABEL },
                    ) {
                        Text(RETAKE_LABEL)
                    }
                } else {
                    ShutterButton(
                        enabled = uiState.canCapture,
                        onClick = {
                            captureBoardFrame(
                                context = context,
                                controller = cameraController,
                                viewModel = viewModel,
                            )
                        },
                    )
                }
                CaptureGuidance()
            }
        }
        Spacer(Modifier.height(VoxBoxSpacing.small))
    }
}

/**
 * Large circular shutter under the preview.
 *
 * The capture label stays on the accessibility node so assistive technology and the device test
 * keep addressing this control by name even though the button itself is now iconographic.
 */
@Composable
private fun ShutterButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .size(76.dp)
                .semantics { contentDescription = CAPTURE_LABEL },
        ) {
            Icon(
                imageVector = VoxBoxIcons.Camera,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun StatusHeader(state: BoardCaptureUiState) {
    Column(
        modifier = Modifier.padding(
            start = VoxBoxSpacing.xSmall,
            end = VoxBoxSpacing.xSmall,
            top = VoxBoxSpacing.xSmall,
        ),
        verticalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small),
    ) {
        VoxBoxStatusPill(
            label = when (state.stage) {
                BoardCaptureStage.ERROR -> "NEEDS ATTENTION"
                BoardCaptureStage.REVIEW -> "REVIEW BEFORE SAVING"
                BoardCaptureStage.SAVED -> "SAVED"
                BoardCaptureStage.PROCESSING -> "READING FRAME"
                BoardCaptureStage.CAPTURING -> "CAPTURING"
                else -> "READY"
            },
            tone = when (state.stage) {
                BoardCaptureStage.ERROR -> VoxBoxStatusTone.Error
                BoardCaptureStage.REVIEW -> VoxBoxStatusTone.Warning
                BoardCaptureStage.SAVED -> VoxBoxStatusTone.Success
                else -> VoxBoxStatusTone.Accent
            },
            pulsing = state.stage == BoardCaptureStage.PROCESSING ||
                state.stage == BoardCaptureStage.CAPTURING,
        )
        Text(state.status, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "Only the still frame you capture is analyzed. A configured AI service is tried " +
                "first; bundled on-device OCR is the fallback.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CameraPreview(
    cameraController: LifecycleCameraController,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
    Box(
        modifier = modifier
            .aspectRatio(4f / 3f)
            .clip(shape)
            .semantics { contentDescription = PREVIEW_DESCRIPTION },
    ) {
        AndroidView(
            factory = { previewContext ->
                PreviewView(previewContext).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    controller = cameraController
                }
            },
            update = { preview -> preview.controller = cameraController },
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(16.dp),
                ),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 30.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        ) {
            Text(
                text = "Keep all writing inside the frame",
                modifier = Modifier.padding(horizontal = VoxBoxSpacing.medium, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun PermissionCard(onRequestPermission: () -> Unit) {
    VoxBoxSectionCard(Modifier.fillMaxWidth(), tone = VoxBoxStatusTone.Accent) {
        VoxBoxSectionHeader(
            title = "Camera access is needed",
            supportingText = "Nothing is streamed or recorded",
        )
        Text(
            text = "Only the frame you capture may be sent to the configured AI service. " +
                "The temporary phone file is deleted after extraction.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
            Text("Allow camera access")
        }
    }
}

@Composable
private fun ProcessingCard() {
    VoxBoxSectionCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text("Reading the captured frame…", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "The still is sent to the configured AI service when available; " +
                        "bundled on-device OCR is the fallback.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(
    state: BoardCaptureUiState,
    viewModel: BoardCaptureViewModel,
    onRetake: () -> Unit,
    onSave: () -> Unit,
) {
    val draft = state.draft ?: return
    VoxBoxSectionCard(Modifier.fillMaxWidth()) {
        VoxBoxSectionHeader(
            title = "Review extraction",
            supportingText = "Nothing is saved until you confirm",
            trailing = {
                VoxBoxStatusPill(
                    label = when (draft.source) {
                        BoardExtractionSource.REMOTE_VISION -> "VISION"
                        BoardExtractionSource.MOCK_PROXY -> "MOCK"
                        BoardExtractionSource.OFFLINE_OCR -> "OFFLINE OCR"
                    },
                    tone = when (draft.source) {
                        BoardExtractionSource.REMOTE_VISION -> VoxBoxStatusTone.Success
                        else -> VoxBoxStatusTone.Warning
                    },
                )
            },
        )
        Column(verticalArrangement = Arrangement.spacedBy(VoxBoxSpacing.xSmall)) {
            Text(
                text = when (draft.source) {
                    BoardExtractionSource.REMOTE_VISION -> "Vision extraction"
                    BoardExtractionSource.MOCK_PROXY -> "Mock response — image not analyzed"
                    BoardExtractionSource.OFFLINE_OCR -> "Offline OCR fallback"
                },
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = if (draft.source != BoardExtractionSource.REMOTE_VISION) {
                    "Confidence not reported"
                } else {
                    "${String.format(Locale.US, "%.0f", draft.confidence * 100)}% confidence"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        OutlinedTextField(
            value = draft.title,
            onValueChange = viewModel::updateTitle,
            label = { Text("Note title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.summary,
            onValueChange = viewModel::updateSummary,
            label = { Text("Summary") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.conceptsText,
            onValueChange = viewModel::updateConcepts,
            label = { Text("Concepts (one per line)") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.visibleText,
            onValueChange = viewModel::updateVisibleText,
            label = { Text("Visible board text") },
            minLines = 5,
            modifier = Modifier.fillMaxWidth(),
        )
        if (draft.warnings.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Check before saving", style = MaterialTheme.typography.titleSmall)
            draft.warnings.forEach { warning ->
                Row(horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small)) {
                    Icon(
                        imageVector = VoxBoxIcons.Info,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = warning,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.medium),
        ) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = RETAKE_LABEL },
            ) {
                Text(RETAKE_LABEL)
            }
            Button(
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = SAVE_LABEL },
            ) {
                Text("Save note")
            }
        }
    }
}

@Composable
private fun CaptureGuidance() {
    VoxBoxSectionCard(Modifier.fillMaxWidth()) {
        VoxBoxSectionHeader(title = "For a clearer result")
        listOf(
            "Hold the phone steady and fill the frame.",
            "Avoid glare and blocked writing.",
            "Review equations and names before saving.",
        ).forEach { tip ->
            Row(horizontalArrangement = Arrangement.spacedBy(VoxBoxSpacing.small)) {
                Icon(
                    imageVector = VoxBoxIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun captureBoardFrame(
    context: Context,
    controller: LifecycleCameraController,
    viewModel: BoardCaptureViewModel,
) {
    if (!viewModel.onCaptureStarted()) return
    val target = try {
        File.createTempFile("voxbox-board-", ".jpg", context.cacheDir)
    } catch (error: Exception) {
        viewModel.onCaptureFailed("A temporary capture file could not be created.")
        return
    }
    val outputOptions = ImageCapture.OutputFileOptions.Builder(target).build()
    try {
        controller.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    viewModel.processCapturedFile(target)
                }

                override fun onError(exception: ImageCaptureException) {
                    target.delete()
                    viewModel.onCaptureFailed(
                        exception.message?.takeIf(String::isNotBlank)
                            ?: "The camera could not capture this frame.",
                    )
                }
            },
        )
    } catch (error: Exception) {
        target.delete()
        viewModel.onCaptureFailed(
            error.message?.takeIf(String::isNotBlank)
                ?: "The camera could not capture this frame.",
        )
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
