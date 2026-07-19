package me.thimmaiah.voxbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import me.thimmaiah.voxbox.ui.VoxBoxScreen
import me.thimmaiah.voxbox.ui.theme.VoxBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoxBoxTheme(dynamicColor = false) {
                VoxBoxScreen()
            }
        }
    }
}
