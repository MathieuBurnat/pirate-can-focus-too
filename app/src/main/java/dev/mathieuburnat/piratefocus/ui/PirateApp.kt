package dev.mathieuburnat.piratefocus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.mathieuburnat.piratefocus.focus.FocusViewModel
import dev.mathieuburnat.piratefocus.guard.GuardPermissions
import dev.mathieuburnat.piratefocus.journal.JournalViewModel

private enum class Screen { MENU, FOCUS, JOURNAL, SETTINGS }

/** Racine de l'app : le menu de pirate et la navigation entre les écrans. */
@Composable
fun PirateApp(viewModel: FocusViewModel = viewModel(), journalViewModel: JournalViewModel = viewModel()) {
    val context = LocalContext.current
    var screen by rememberSaveable { mutableStateOf(Screen.MENU) }
    // Le journal secret se déverrouille à chaque lancement (7 coups sur la porte).
    var journalUnlocked by rememberSaveable { mutableStateOf(false) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = state.timer.phase

    var guardReady by rememberSaveable { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        guardReady = GuardPermissions.isReady(context)
        onPauseOrDispose { }
    }

    BackHandler(enabled = screen != Screen.MENU) { screen = Screen.MENU }

    when (screen) {
        Screen.MENU -> MenuScreen(
            phase = phase,
            journalUnlocked = journalUnlocked,
            onFocus = { screen = Screen.FOCUS },
            onUnlockJournal = { journalUnlocked = true },
            onJournal = { screen = Screen.JOURNAL },
            onSettings = { screen = Screen.SETTINGS },
        )
        Screen.FOCUS -> FocusRoute(viewModel, guardReady = guardReady, onMenu = { screen = Screen.MENU })
        Screen.JOURNAL -> JournalScreen(journalViewModel, onBack = { screen = Screen.MENU })
        Screen.SETTINGS -> SettingsScreen(onBack = { screen = Screen.MENU })
    }
}
