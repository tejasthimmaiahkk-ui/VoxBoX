package me.thimmaiah.voxbox.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.thimmaiah.voxbox.data.SettingsRepository
import me.thimmaiah.voxbox.data.VbSettings
import me.thimmaiah.voxbox.data.VbThemeMode
import me.thimmaiah.voxbox.ui.VbCard
import me.thimmaiah.voxbox.ui.VbEyebrow
import me.thimmaiah.voxbox.ui.VbIcons
import me.thimmaiah.voxbox.ui.VbPill
import me.thimmaiah.voxbox.ui.VbSegmented
import me.thimmaiah.voxbox.ui.theme.LocalVbStatus
import me.thimmaiah.voxbox.ui.theme.VbAccent
import me.thimmaiah.voxbox.ui.theme.VbBlue
import me.thimmaiah.voxbox.ui.theme.VbGreen
import me.thimmaiah.voxbox.ui.theme.VbOrange
import me.thimmaiah.voxbox.ui.theme.VbRed
import me.thimmaiah.voxbox.ui.theme.VbReadingSize
import me.thimmaiah.voxbox.ui.theme.VbShape
import me.thimmaiah.voxbox.ui.theme.VbSpace
import me.thimmaiah.voxbox.ui.theme.readerBodyStyle

@Composable
fun AppearanceScreen(
    settingsRepository: SettingsRepository,
    scope: CoroutineScope,
    onBack: () -> Unit,
) {
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = VbSettings())
    val status = LocalVbStatus.current

    VbSubPage(title = "Appearance", onBack = onBack) {
        VbEyebrow("Theme")
        Spacer(Modifier.height(8.dp))
        VbSegmented(
            options = listOf(VbThemeMode.System, VbThemeMode.Dark, VbThemeMode.Light),
            selected = settings.theme,
            label = { it.name },
            onSelect = { scope.launch { settingsRepository.setTheme(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(VbSpace.section))

        VbEyebrow("Accent")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            VbAccent.entries.forEach { accent ->
                AccentSwatch(
                    accent = accent,
                    selected = settings.accent == accent,
                    onClick = { scope.launch { settingsRepository.setAccent(accent) } },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        // The one thing a user could reasonably expect the accent to change, and must not.
        Row(verticalAlignment = Alignment.CenterVertically) {
            VbPill("Saved", status.saved)
            Spacer(Modifier.size(8.dp))
            VbPill("Review", status.review)
            Spacer(Modifier.size(8.dp))
            VbPill("Recording", status.danger)
        }
        VbNote(
            "Status colours never follow the accent. Green always means saved, orange always " +
                "means it needs your review, and red always means recording, deleting or failed.",
        )
        Spacer(Modifier.height(VbSpace.section))

        VbEyebrow("Reading size")
        Spacer(Modifier.height(8.dp))
        VbSegmented(
            options = VbReadingSize.entries.toList(),
            selected = VbReadingSize.fromOrdinal(settings.readingSize),
            label = { it.label },
            onSelect = { scope.launch { settingsRepository.setReadingSize(it.ordinal) } },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        VbSegmented(
            options = listOf(false, true),
            selected = settings.readingSerif,
            label = { if (it) "Serif" else "Sans" },
            onSelect = { scope.launch { settingsRepository.setReadingSerif(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        VbCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "A limit describes the value a function approaches as the input approaches " +
                    "some point. It does not require the function to be defined there.",
                style = readerBodyStyle(
                    VbReadingSize.fromOrdinal(settings.readingSize),
                    settings.readingSerif,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        VbNote("This is how note text will look in the reader.")
    }
}

@Composable
private fun AccentSwatch(accent: VbAccent, selected: Boolean, onClick: () -> Unit) {
    val hue = when (accent) {
        VbAccent.Blue -> VbBlue
        VbAccent.Green -> VbGreen
        VbAccent.Orange -> VbOrange
        VbAccent.Red -> VbRed
    }
    val ring by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
        animationSpec = tween(220),
        label = "swatch-ring",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .border(2.dp, ring, VbShape.pill)
            .clickable(role = Role.RadioButton, onClickLabel = "${accent.name} accent", onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(VbShape.pill)
                .background(hue),
        ) {
            if (selected) {
                Icon(
                    painter = painterResource(VbIcons.Check),
                    contentDescription = "Selected",
                    tint = if (accent == VbAccent.Orange) Color(0xFF1B1206) else Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
