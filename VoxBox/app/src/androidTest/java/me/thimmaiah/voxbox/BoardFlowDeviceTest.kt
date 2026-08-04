package me.thimmaiah.voxbox

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.Ignore
import org.junit.runner.RunWith

@Ignore("Manual device test: requires rear-camera hardware and the mock proxy through adb reverse.")
@RunWith(AndroidJUnit4::class)
class BoardFlowDeviceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun captureMockBoardFrameReviewAndSave() {
        tryGrantCameraPermission()
        composeRule.onNodeWithContentDescription("Board").performClick()
        allowCameraDialogIfShown()

        composeRule.waitUntil(timeoutMillis = 30_000) {
            composeRule.onAllNodesWithContentDescription("Capture board frame")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Live board camera preview").assertIsDisplayed()
        saveScreenshot("board-live-preview.png")

        composeRule.onNodeWithContentDescription("Capture board frame").performClick()
        composeRule.waitUntil(timeoutMillis = 60_000) {
            composeRule.onAllNodesWithText("Mock response — image not analyzed")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Confidence not reported").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Mock mode is enabled; the submitted image was not analyzed.",
        ).performScrollTo().assertIsDisplayed()
        saveScreenshot("board-mock-review.png")

        composeRule.onNodeWithContentDescription("Save board capture as note")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithText("Mock board capture", substring = false)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Mock board capture", substring = false).assertIsDisplayed()
        saveScreenshot("board-saved-note.png")
    }

    private fun tryGrantCameraPermission() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) return
        runCatching {
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.CAMERA,
            )
        }
    }

    private fun allowCameraDialogIfShown() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) return
        val deadline = System.currentTimeMillis() + 10_000
        while (
            context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
            System.currentTimeMillis() < deadline
        ) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            root?.findPermissionButton()?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Thread.sleep(250)
        }
        check(context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            "Camera permission could not be granted through the device permission dialog."
        }
    }

    private fun AccessibilityNodeInfo.findPermissionButton(): AccessibilityNodeInfo? {
        val normalized = text?.toString()?.trim()?.lowercase().orEmpty()
        if (
            isClickable &&
            (
                normalized.contains("while using") ||
                    normalized.contains("only this time") ||
                    normalized == "allow"
                )
        ) {
            return this
        }
        for (index in 0 until childCount) {
            getChild(index)?.findPermissionButton()?.let { return it }
        }
        return null
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
}
