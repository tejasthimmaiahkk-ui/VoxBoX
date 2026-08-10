package me.thimmaiah.voxbox.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.thimmaiah.voxbox.ui.VbCard
import me.thimmaiah.voxbox.ui.VbEyebrow
import me.thimmaiah.voxbox.ui.VbIconButton
import me.thimmaiah.voxbox.ui.VbIcons
import me.thimmaiah.voxbox.ui.theme.LocalVbStatus
import me.thimmaiah.voxbox.ui.theme.VbSpace

/**
 * Shared frame for every settings sub-page: a back row, a serif title, and a scrolling column.
 *
 * Sub-pages are full-screen rather than tabs, so the back affordance has to be part of the page
 * itself; putting it in the frame keeps the six of them from drifting apart.
 */
@Composable
fun VbSubPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = VbSpace.screenH),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            VbIconButton(
                icon = VbIcons.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
                modifier = Modifier.padding(start = (-12).dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(VbSpace.section))
        content()
        Spacer(Modifier.height(48.dp))
    }
}

/** A titled group of rows on one card, the unit every settings page is built from. */
@Composable
fun VbSettingsGroup(
    eyebrow: String,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(modifier) {
        VbEyebrow(eyebrow)
        Spacer(Modifier.height(8.dp))
        VbCard(modifier = Modifier.fillMaxWidth(), content = content)
        Spacer(Modifier.height(VbSpace.section))
    }
}

/** A page-level explanatory paragraph. Small, muted, never a card. */
@Composable
fun VbNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = LocalVbStatus.current.fg2,
        modifier = modifier.padding(top = 8.dp),
    )
}

/** The tab-level Settings frame, which keeps the bottom bar visible. */
@Composable
fun VbTabPage(
    title: String,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = VbSpace.screenH)
            .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(VbSpace.section))
        content()
    }
}
