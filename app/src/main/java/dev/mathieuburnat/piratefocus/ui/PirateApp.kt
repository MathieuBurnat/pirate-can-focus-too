package dev.mathieuburnat.piratefocus.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.mathieuburnat.piratefocus.account.AccountViewModel
import dev.mathieuburnat.piratefocus.account.ApiException
import dev.mathieuburnat.piratefocus.focus.FocusViewModel
import dev.mathieuburnat.piratefocus.guard.GuardPermissions
import dev.mathieuburnat.piratefocus.journal.CrewMate
import dev.mathieuburnat.piratefocus.journal.JournalViewModel
import dev.mathieuburnat.piratefocus.settings.DevModeStore
import dev.mathieuburnat.piratefocus.settings.FactoryReset

private enum class Screen { WELCOME, MENU, FOCUS, JOURNAL, CREW, SETTINGS, DEV_MODE, BLOCKED_APPS, ACCOUNT }

/** Racine de l'app : le menu de pirate et la navigation entre les écrans. */
@Composable
fun PirateApp(
    viewModel: FocusViewModel = viewModel(),
    journalViewModel: JournalViewModel = viewModel(),
    accountViewModel: AccountViewModel = viewModel(),
) {
    val context = LocalContext.current
    val accountState by accountViewModel.uiState.collectAsStateWithLifecycle()
    // Au tout premier lancement, l'écran de bienvenue : anonyme ou compte gratuit.
    var screen by rememberSaveable { mutableStateOf(if (accountState.account.onboarded) Screen.MENU else Screen.WELCOME) }
    val devModeStore = remember { DevModeStore(context) }
    var devMode by remember { mutableStateOf(devModeStore.isOn()) }
    var devRevealed by remember { mutableStateOf(devModeStore.isRevealed()) }
    var fakeData by remember { mutableStateOf(devModeStore.isFakeData()) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val phase = state.timer.phase

    var guardReady by rememberSaveable { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        guardReady = GuardPermissions.isReady(context)
        onPauseOrDispose { }
    }

    BackHandler(enabled = screen != Screen.MENU && screen != Screen.WELCOME) {
        screen = when (screen) {
            Screen.CREW -> Screen.JOURNAL
            Screen.BLOCKED_APPS, Screen.DEV_MODE -> Screen.SETTINGS
            else -> Screen.MENU
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Les écrans ne gèrent plus la barre de navigation : le pied de page s'en charge.
        Box(Modifier.weight(1f).consumeWindowInsets(WindowInsets.navigationBars)) {
            when (screen) {
                Screen.WELCOME -> WelcomeScreen(accountViewModel, onDone = { screen = Screen.MENU })
                Screen.MENU -> MenuScreen(
                    phase = phase,
                    signedIn = accountState.account.signedIn,
                    onFocus = { screen = Screen.FOCUS },
                    onJournal = { screen = Screen.JOURNAL },
                    onSettings = { screen = Screen.SETTINGS },
                    onAccount = { screen = Screen.ACCOUNT },
                )
                Screen.FOCUS -> FocusRoute(viewModel, guardReady = guardReady, onMenu = { screen = Screen.MENU })
                Screen.JOURNAL -> JournalScreen(
                    journalViewModel,
                    onBack = { screen = Screen.MENU },
                    onCrew = { screen = Screen.CREW },
                )
                Screen.CREW -> {
                    val journal by journalViewModel.uiState.collectAsStateWithLifecycle()
                    // Sans « fake data » : les vrais pirates publics, rechargés à chaque visite.
                    var status by remember { mutableStateOf<String?>(null) }
                    val realCrew by produceState(initialValue = emptyList<CrewMate>(), fakeData) {
                        if (fakeData) return@produceState
                        status = "~ la longue-vue cherche l'équipage... ~"
                        status = try {
                            value = accountViewModel.crew()
                            if (value.isEmpty()) "Aucun pirate public à l'horizon pour l'instant." else null
                        } catch (e: ApiException) {
                            e.message
                        }
                    }
                    CrewScreen(
                        crew = if (fakeData) journalViewModel.crew else realCrew,
                        myLogs = journal.myLogs,
                        imaginary = fakeData,
                        status = if (fakeData) null else status,
                        onBack = { screen = Screen.JOURNAL },
                    )
                }
                Screen.SETTINGS -> SettingsScreen(
                    devMode = devMode,
                    devRevealed = devRevealed,
                    signedIn = accountState.account.signedIn,
                    onBack = { screen = Screen.MENU },
                    onBlockedApps = { screen = Screen.BLOCKED_APPS },
                    onRevealDev = {
                        devModeStore.reveal()
                        devRevealed = true
                    },
                    onDevMode = { screen = Screen.DEV_MODE },
                    onLogout = accountViewModel::logout,
                )
                Screen.DEV_MODE -> DevModeScreen(
                    devMode = devMode,
                    fakeData = fakeData,
                    onBack = { screen = Screen.SETTINGS },
                    onFactoryReset = {
                        // D'abord l'état en mémoire (rien de périmé à l'écran), puis le téléphone.
                        accountViewModel.reset()
                        journalViewModel.reset()
                        viewModel.reset()
                        FactoryReset.wipe(context)
                        devMode = false
                        devRevealed = false
                        fakeData = false
                        screen = Screen.WELCOME
                        Toast.makeText(context.applicationContext, "Merci pour tout. Bon voyage !", Toast.LENGTH_LONG).show()
                    },
                    anonymous = !accountState.account.signedIn,
                    onLinkAccount = { screen = Screen.ACCOUNT },
                    onFakeDataChange = {
                        devModeStore.setFakeData(it)
                        fakeData = it
                    },
                    onDevModeChange = {
                        devModeStore.set(it)
                        devMode = it
                        fakeData = devModeStore.isFakeData()
                        // Mode dev éteint : le bouton se recache, retour aux paramètres.
                        if (!it) {
                            devModeStore.hide()
                            devRevealed = false
                            screen = Screen.SETTINGS
                        }
                    },
                )
                Screen.BLOCKED_APPS -> BlockedAppsScreen(onBack = { screen = Screen.SETTINGS })
                Screen.ACCOUNT -> AccountScreen(accountViewModel, onBack = { screen = Screen.MENU })
            }
        }
        VersionFooter()
    }
}
