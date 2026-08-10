package me.thimmaiah.voxbox.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.thimmaiah.voxbox.data.SettingsRepository
import me.thimmaiah.voxbox.data.VbSettings
import me.thimmaiah.voxbox.ui.VbOutlineButton
import me.thimmaiah.voxbox.ui.VbPrimaryButton
import me.thimmaiah.voxbox.ui.theme.LocalVbStatus
import me.thimmaiah.voxbox.ui.theme.VbShape
import me.thimmaiah.voxbox.ui.theme.VbSpace

/**
 * Three panes, shown once.
 *
 * The permission pane states the reason before the system dialog appears, and the last pane
 * carries the consent line: recording a class is not automatically the student's to do.
 */
@Composable
fun OnboardingScreen(
    settingsRepository: SettingsRepository,
    scope: CoroutineScope,
    onDone: () -> Unit,
) {
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = VbSettings())
    val pagerState = rememberPagerState(pageCount = { 3 })
    val pagerScope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

    val finish = {
        scope.launch {
            settingsRepository.setOnboarded(true)
            settingsRepository.setConsentAcknowledged(true)
        }
        onDone()
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = VbSpace.screenH),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Text(
                text = "Skip",
                style = MaterialTheme.typography.labelLarge,
                color = LocalVbStatus.current.fg2,
                modifier = Modifier
                    .clip(VbShape.pill)
                    .clickable(onClickLabel = "Skip") { finish() }
                    .padding(12.dp),
            )
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                when (page) {
                    0 -> Pane(
                        title = "A note, while the lecture happens",
                        body = "VoxBox listens and, in board mode, watches the board. It writes a " +
                            "structured Markdown note as the class runs, and keeps the raw " +
                            "transcript behind it so nothing is lost to a summary.\n\n" +
                            "Capture is foreground-only. It never records with the screen off.",
                    )
                    1 -> Pane(
                        title = "Microphone, and camera for board mode",
                        body = "The microphone records audio in 20-second chunks. The camera is " +
                            "only used in board mode, and frames that have not changed are " +
                            "deleted on this phone before anything is sent.",
                        action = {
                            VbPrimaryButton(
                                text = "Grant permissions",
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.RECORD_AUDIO,
                                            Manifest.permission.CAMERA,
                                        ),
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                    )
                    else -> Pane(
                        title = "Before you record a class",
                        body = "Recording a lecture may require permission from your institution " +
                            "or the speaker. Please check before you start.\n\n" +
                            "Notes, transcripts and diagram crops stay on this device. System " +
                            "backup is disabled, and nothing leaves the phone except through an " +
                            "export you start yourself.",
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        ) {
            repeat(3) { index ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index == pagerState.currentPage) 9.dp else 7.dp)
                        .clip(VbShape.pill)
                        .background(
                            if (index == pagerState.currentPage) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                LocalVbStatus.current.line
                            },
                        ),
                )
            }
        }

        if (pagerState.currentPage == 2) {
            VbPrimaryButton(
                text = "I understand — start",
                onClick = { finish() },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            )
        } else {
            VbOutlineButton(
                text = "Next",
                onClick = {
                    pagerScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                height = 52.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Pane(title: String, body: String, action: (@Composable () -> Unit)? = null) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = LocalVbStatus.current.fg2,
        )
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}
