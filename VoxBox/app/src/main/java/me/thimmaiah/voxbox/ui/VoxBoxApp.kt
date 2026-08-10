package me.thimmaiah.voxbox.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import me.thimmaiah.voxbox.data.SettingsRepository
import me.thimmaiah.voxbox.home.HomeScreen
import me.thimmaiah.voxbox.library.LibraryScreen
import me.thimmaiah.voxbox.nav.VbRoute
import me.thimmaiah.voxbox.notes.NoteLibraryViewModel
import me.thimmaiah.voxbox.onboarding.OnboardingScreen
import me.thimmaiah.voxbox.reader.NoteReaderScreen
import me.thimmaiah.voxbox.session.CaptureSessionViewModel
import me.thimmaiah.voxbox.session.CaptureSetupScreen
import me.thimmaiah.voxbox.session.LiveSessionScreen
import me.thimmaiah.voxbox.settings.AboutScreen
import me.thimmaiah.voxbox.settings.AppearanceScreen
import me.thimmaiah.voxbox.settings.AudioRecoveryScreen
import me.thimmaiah.voxbox.settings.ConnectionScreen
import me.thimmaiah.voxbox.settings.DebugLogScreen
import me.thimmaiah.voxbox.settings.ExportScreen
import me.thimmaiah.voxbox.settings.PrivacyScreen
import me.thimmaiah.voxbox.settings.SettingsScreen

val LocalVbSnackbar = staticCompositionLocalOf<SnackbarHostState> { error("No snackbar host") }

@Composable
fun VoxBoxApp(
    settingsRepository: SettingsRepository,
    onboarded: Boolean,
    scope: CoroutineScope,
) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route in VbRoute.tabs

    CompositionLocalProvider(LocalVbSnackbar provides snackbar) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                AnimatedVisibility(
                    visible = showBar,
                    enter = slideInVertically(tween(VbMotion.SWITCH)) { it } + fadeIn(),
                    exit = slideOutVertically(tween(VbMotion.SWITCH)) { it } + fadeOut(),
                ) {
                    VbBottomBar(
                        currentRoute = route,
                        onSelect = navController::navigateToTab,
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                VbNavGraph(
                    navController = navController,
                    settingsRepository = settingsRepository,
                    startDestination = if (onboarded) VbRoute.HOME else VbRoute.ONBOARDING,
                    contentPadding = padding,
                    scope = scope,
                )
            }
        }
    }
}

/** Tab switching restores the tab's own stack rather than piling a second copy on top. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(VbRoute.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun VbNavGraph(
    navController: NavHostController,
    settingsRepository: SettingsRepository,
    startDestination: String,
    contentPadding: PaddingValues,
    scope: CoroutineScope,
) {
    // One capture view model for the whole graph. Setup and the live session are two views of the
    // same recording, so rebuilding it on navigation would drop a lecture in progress.
    val captureViewModel: CaptureSessionViewModel = viewModel()
    val libraryViewModel: NoteLibraryViewModel = viewModel()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(VbRoute.ONBOARDING) {
            OnboardingScreen(settingsRepository, scope) {
                navController.navigate(VbRoute.HOME) {
                    popUpTo(VbRoute.ONBOARDING) { inclusive = true }
                }
            }
        }
        composable(VbRoute.HOME) {
            HomeScreen(
                captureViewModel = captureViewModel,
                libraryViewModel = libraryViewModel,
                settingsRepository = settingsRepository,
                scope = scope,
                contentPadding = contentPadding,
                onOpenCapture = { navController.navigate(VbRoute.CAPTURE) },
                onOpenNote = { id -> navController.navigate(VbRoute.note(id)) },
                onStarted = { navController.navigate(VbRoute.LIVE) },
            )
        }
        composable(VbRoute.CAPTURE) {
            CaptureSetupScreen(
                viewModel = captureViewModel,
                contentPadding = contentPadding,
                onStarted = { navController.navigate(VbRoute.LIVE) },
            )
        }
        composable(VbRoute.LIVE) {
            LiveSessionScreen(
                viewModel = captureViewModel,
                onFinished = { noteId ->
                    if (noteId != null) {
                        navController.navigate(VbRoute.note(noteId)) { popUpTo(VbRoute.HOME) }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }
        composable(VbRoute.LIBRARY) {
            LibraryScreen(
                viewModel = libraryViewModel,
                contentPadding = contentPadding,
                onOpenNote = { id -> navController.navigate(VbRoute.note(id)) },
            )
        }
        composable(VbRoute.NOTE) { backStackEntry ->
            NoteReaderScreen(
                noteId = backStackEntry.arguments?.getString("noteId").orEmpty(),
                viewModel = libraryViewModel,
                settingsRepository = settingsRepository,
                scope = scope,
                onBack = { navController.popBackStack() },
            )
        }
        composable(VbRoute.SETTINGS) {
            SettingsScreen(
                captureViewModel = captureViewModel,
                settingsRepository = settingsRepository,
                contentPadding = contentPadding,
                onOpen = { target -> navController.navigate(target) },
            )
        }
        composable(VbRoute.SET_APPEARANCE) {
            AppearanceScreen(settingsRepository, scope) { navController.popBackStack() }
        }
        composable(VbRoute.SET_AUDIO_RECOVERY) {
            AudioRecoveryScreen(captureViewModel) { navController.popBackStack() }
        }
        composable(VbRoute.SET_CONNECTION) {
            ConnectionScreen { navController.popBackStack() }
        }
        composable(VbRoute.SET_PRIVACY) {
            PrivacyScreen(settingsRepository, scope) { navController.popBackStack() }
        }
        composable(VbRoute.SET_EXPORT) {
            ExportScreen(settingsRepository, scope) { navController.popBackStack() }
        }
        composable(VbRoute.SET_ABOUT) {
            AboutScreen { navController.popBackStack() }
        }
        // TEMPORARY. Remove with the debug log; see docs/TEMPORARY_DEBUG_LOG.md.
        composable(VbRoute.SET_DEBUG) {
            DebugLogScreen { navController.popBackStack() }
        }
    }
}
