package me.thimmaiah.voxbox.nav

import me.thimmaiah.voxbox.ui.VbIcons

/**
 * Every destination in the app.
 *
 * Four of them are tabs. Everything else is full screen, because it is either a task you are in
 * the middle of (Live) or a document you are reading (Note) — both are worse with a tab bar
 * stealing the bottom of the screen.
 */
object VbRoute {
    const val HOME = "home"
    const val CAPTURE = "capture"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"

    const val LIVE = "live"
    const val NOTE = "note/{noteId}"

    const val SET_APPEARANCE = "settings/appearance"
    const val SET_AUDIO_RECOVERY = "settings/audio-recovery"
    const val SET_CONNECTION = "settings/connection"
    const val SET_PRIVACY = "settings/privacy"
    const val SET_EXPORT = "settings/export"
    const val SET_ABOUT = "settings/about"

    const val ONBOARDING = "onboarding"

    fun note(noteId: String) = "note/$noteId"

    /** Routes that show the bottom bar. Anything else is full screen. */
    val tabs = listOf(HOME, CAPTURE, LIBRARY, SETTINGS)
}

data class VbTab(
    val route: String,
    val label: String,
    val icon: Int,
)

val VB_TABS = listOf(
    VbTab(VbRoute.HOME, "Home", VbIcons.Home),
    VbTab(VbRoute.CAPTURE, "Capture", VbIcons.Mic),
    VbTab(VbRoute.LIBRARY, "Library", VbIcons.Library),
    VbTab(VbRoute.SETTINGS, "Settings", VbIcons.Settings),
)
