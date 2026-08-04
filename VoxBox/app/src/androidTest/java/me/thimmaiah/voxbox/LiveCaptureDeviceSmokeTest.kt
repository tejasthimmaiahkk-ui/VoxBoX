package me.thimmaiah.voxbox

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opt-in physical-device smoke test for the local mock proxy.
 *
 * Run with `voxboxLiveSmoke=true` after forwarding device port 8787 to a
 * `MOCK_AI=1` development proxy. Normal connected-test runs skip this test so
 * they never require a microphone, camera, or local server.
 */
@RunWith(AndroidJUnit4::class)
class LiveCaptureDeviceSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun voiceDrainsFinalPartialChunk() {
        prepareSmoke()
        enterTitle("Device smoke - voice drain")
        scrollToMatcher(hasContentDescription("Start continuous voice session"))
        composeRule.onNodeWithContentDescription("Start continuous voice session")
            .performClick()
        composeRule.waitUntil(timeoutMillis = 30_000) { isTextComposed("Continuous audio") }
        composeRule.onNodeWithText("Continuous audio").assertIsDisplayed()

        // Stop before the normal 20-second boundary: the final partial WAV must
        // still be transcribed, refined, persisted, and shown on the saved note.
        Thread.sleep(4_000)
        composeRule.onNodeWithText("Stop and finish note").performClick()
        waitForTextByScrolling("SAVED", timeoutMillis = 75_000)
        composeRule.onNodeWithText("SAVED").assertIsDisplayed()
        waitForTextByScrolling(MOCK_TRANSCRIPT, timeoutMillis = 15_000)
        saveScreenshot("live-voice-drain-saved.png")
    }

    @Test
    fun videoCapturesBoardAndAudio() {
        prepareSmoke()
        scrollToMatcher(hasText("Live board", substring = true))
        composeRule.onNodeWithText("Live board", substring = true)
            .performClick()
        enterTitle("Device smoke - video and audio")
        scrollToMatcher(hasContentDescription("Start continuous video session"))
        composeRule.onNodeWithContentDescription("Start continuous video session")
            .performClick()
        composeRule.waitUntil(timeoutMillis = 30_000) { isTextComposed("Camera + continuous audio") }
        composeRule.onNodeWithText("Camera + continuous audio").assertIsDisplayed()

        // The first accepted frame exercises CameraX -> local change gate ->
        // board extraction -> note refinement. Waiting for the 20-second audio
        // chunk at the same time proves Video mode keeps listening continuously.
        Thread.sleep(25_000)
        waitForTextByScrolling(MOCK_BOARD_TEXT, timeoutMillis = 45_000)
        waitForTextByScrolling(MOCK_TRANSCRIPT, timeoutMillis = 45_000)
        saveScreenshot("live-video-board-and-audio.png")

        scrollToMatcher(hasText("Stop and finish note"))
        composeRule.onNodeWithText("Stop and finish note")
            .performClick()
        waitForTextByScrolling("SAVED", timeoutMillis = 75_000)
        composeRule.onNodeWithText("SAVED").assertIsDisplayed()
        waitForTextByScrolling(MOCK_BOARD_TEXT, timeoutMillis = 15_000)
        waitForTextByScrolling(MOCK_TRANSCRIPT, timeoutMillis = 15_000)
        saveScreenshot("live-video-board-and-audio-saved.png")
    }

    private fun prepareSmoke() {
        assumeTrue(
            "Pass -e voxboxLiveSmoke true and run the local mock proxy to enable this test.",
            InstrumentationRegistry.getArguments().getString("voxboxLiveSmoke") == "true",
        )
        grantRuntimePermission(Manifest.permission.RECORD_AUDIO)
        grantRuntimePermission(Manifest.permission.CAMERA)
        composeRule.onNodeWithContentDescription("Live").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            isTextComposed("One session, one living note")
        }
    }

    private fun enterTitle(title: String) {
        scrollToMatcher(hasText("New note title"))
        val titleField = composeRule.onNodeWithText("New note title")
        runCatching { titleField.performTextClearance() }
        titleField.performTextInput(title)
        closeSoftKeyboard()
    }

    private fun grantRuntimePermission(permission: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) return
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, permission)
        check(context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            "$permission was not granted for the live device smoke test."
        }
    }

    private fun isTextComposed(text: String): Boolean =
        composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    private fun scrollToMatcher(matcher: SemanticsMatcher) {
        val list = composeRule.onNode(hasScrollAction())
        runCatching { list.performScrollToIndex(0) }
        list.performScrollToNode(matcher)
    }

    private fun waitForTextByScrolling(text: String, timeoutMillis: Long) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        var lastFailure: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            val attempt = runCatching { scrollToMatcher(hasText(text, substring = true)) }
            if (attempt.isSuccess && isTextComposed(text)) return
            lastFailure = attempt.exceptionOrNull()
            Thread.sleep(400)
        }
        throw AssertionError("Timed out waiting for text: $text", lastFailure)
    }

    private fun saveScreenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(
            instrumentation.targetContext.getExternalFilesDir(null),
            "evidence",
        ).apply { mkdirs() }
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        FileOutputStream(File(directory, name)).use { output ->
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        screenshot.recycle()
    }

    private companion object {
        const val MOCK_TRANSCRIPT =
            "Mock teacher segment: structured notes preserve evidence and key concepts."
        const val MOCK_BOARD_TEXT = "VoxBox mock vision frame"
    }
}
