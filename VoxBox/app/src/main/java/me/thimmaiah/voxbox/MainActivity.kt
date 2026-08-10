package me.thimmaiah.voxbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import me.thimmaiah.voxbox.data.SettingsRepository
import me.thimmaiah.voxbox.debug.VbDebugLog
import me.thimmaiah.voxbox.data.VbSettings
import me.thimmaiah.voxbox.data.VbThemeMode
import me.thimmaiah.voxbox.ui.VoxBoxApp
import me.thimmaiah.voxbox.ui.theme.VoxBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // TEMPORARY. Off in release so an accidental ship cannot accumulate diagnostics.
        // See docs/TEMPORARY_DEBUG_LOG.md.
        VbDebugLog.enabled = BuildConfig.DEBUG
        VbDebugLog.log("app", "started, debug=${BuildConfig.DEBUG}")
        val settingsRepository = SettingsRepository(applicationContext)
        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = VbSettings())
            val dark = when (settings.theme) {
                VbThemeMode.System -> isSystemInDarkTheme()
                VbThemeMode.Dark -> true
                VbThemeMode.Light -> false
            }
            VoxBoxTheme(darkTheme = dark, accent = settings.accent) {
                VoxBoxApp(
                    settingsRepository = settingsRepository,
                    onboarded = settings.onboarded,
                    scope = rememberCoroutineScope(),
                )
            }
        }
    }
}
