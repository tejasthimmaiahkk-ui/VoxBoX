package me.thimmaiah.voxbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import me.thimmaiah.voxbox.data.SettingsRepository
import me.thimmaiah.voxbox.data.VbSettings
import me.thimmaiah.voxbox.data.VbThemeMode
import me.thimmaiah.voxbox.ui.VoxBoxScreen
import me.thimmaiah.voxbox.ui.theme.VoxBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsRepository = SettingsRepository(applicationContext)
        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = VbSettings())
            val dark = when (settings.theme) {
                VbThemeMode.System -> isSystemInDarkTheme()
                VbThemeMode.Dark -> true
                VbThemeMode.Light -> false
            }
            // Still the pre-redesign screen. The new navigation graph lands with the screens it
            // routes to; wiring it before they exist would only break the app.
            VoxBoxTheme(darkTheme = dark, accent = settings.accent) {
                VoxBoxScreen()
            }
        }
    }
}
