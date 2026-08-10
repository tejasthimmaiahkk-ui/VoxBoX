package me.thimmaiah.voxbox

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Walks the four destinations and every settings sub-page on a real device.
 *
 * Deliberately free of microphone, camera and network: this asserts that the navigation graph
 * and every screen compose without crashing, which is the failure the redesign is most likely to
 * introduce. Capture behaviour is covered by the unit tests and by manual runs, because a
 * meaningful capture test needs a lecture.
 */
@RunWith(AndroidJUnit4::class)
class RedesignNavigationDeviceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String, timeoutMillis: Long = 20_000) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule.onAllNodesWithText(text, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun skipOnboardingIfShown() {
        val onboarding = composeRule.onAllNodesWithText("Skip").fetchSemanticsNodes().isNotEmpty()
        if (!onboarding) return
        composeRule.onNodeWithText("Skip").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun everyDestinationComposes() {
        skipOnboardingIfShown()

        waitForText("Ready to capture")
        composeRule.onNodeWithText("Ready to capture").assertIsDisplayed()

        composeRule.onNodeWithText("Capture").performClick()
        waitForText("Session options")

        composeRule.onNodeWithText("Library").performClick()
        waitForText("Library")

        composeRule.onNodeWithText("Settings").performClick()
        waitForText("Unrecovered audio")
    }

    @Test
    fun everySettingsSubPageOpensAndReturns() {
        skipOnboardingIfShown()
        waitForText("Ready to capture")
        composeRule.onNodeWithText("Settings").performClick()
        waitForText("Unrecovered audio")

        val pages = listOf(
            "Unrecovered audio" to "Nothing waiting",
            "Connection & models" to "Model routing",
            "Appearance" to "Reading size",
            "Export defaults" to "Include review flags",
            "Privacy & storage" to "Keep raw frames",
            "About & evidence" to "Speaker labels",
        )
        pages.forEach { (row, marker) ->
            composeRule.onNodeWithText(row).performClick()
            waitForText(marker)
            composeRule.onNodeWithContentDescriptionBack().performClick()
            waitForText("Unrecovered audio")
        }
    }

    @Test
    fun captureSetupOffersTheFourDecisions() {
        skipOnboardingIfShown()
        waitForText("Ready to capture")
        composeRule.onNodeWithText("Capture").performClick()

        waitForText("Mode")
        composeRule.onNodeWithText("Where it goes").assertIsDisplayed()
        composeRule.onNodeWithText("How it is written").assertIsDisplayed()

        // Board mode reveals the sampling disclosure; voice mode must not show it.
        composeRule.onNodeWithText("Live board").performClick()
        waitForText("Sampling")
        composeRule.onNodeWithText("Voice").performClick()
        composeRule.waitForIdle()
    }
}

private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.onNodeWithContentDescriptionBack() =
    onNode(androidx.compose.ui.test.hasContentDescription("Back"))
