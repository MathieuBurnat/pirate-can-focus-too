package dev.mathieuburnat.piratefocus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.mathieuburnat.piratefocus.account.AccountStore
import dev.mathieuburnat.piratefocus.account.AccountViewModel
import dev.mathieuburnat.piratefocus.focus.ChestStore
import dev.mathieuburnat.piratefocus.focus.FocusViewModel
import dev.mathieuburnat.piratefocus.focus.Phase
import dev.mathieuburnat.piratefocus.guard.FocusGuardService
import dev.mathieuburnat.piratefocus.guard.GuardPermissions
import dev.mathieuburnat.piratefocus.ui.PirateApp
import dev.mathieuburnat.piratefocus.ui.theme.PirateFocusTheme
import dev.mathieuburnat.piratefocus.journal.JournalStore
import dev.mathieuburnat.piratefocus.journal.JournalViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Les ViewModels reçoivent leur coffre, leur journal et leur compte rangés sur le téléphone.
    private val focusViewModel: FocusViewModel by viewModels {
        viewModelFactory { initializer { FocusViewModel(ChestStore(application)) } }
    }
    private val journalViewModel: JournalViewModel by viewModels {
        viewModelFactory { initializer { JournalViewModel(JournalStore(application)) } }
    }
    private val accountViewModel: AccountViewModel by viewModels {
        viewModelFactory { initializer { AccountViewModel(AccountStore(application)) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Le gardien ne monte la garde que pendant une traversée, même si l'app est en arrière-plan.
        lifecycleScope.launch {
            focusViewModel.uiState.map { it.timer.phase }.distinctUntilChanged().collect { phase ->
                if (phase == Phase.FOCUS && GuardPermissions.isReady(this@MainActivity)) {
                    FocusGuardService.start(this@MainActivity)
                } else {
                    FocusGuardService.stop(this@MainActivity)
                }
            }
        }

        // Chaque traversée terminée s'inscrit au journal de bord.
        lifecycleScope.launch {
            focusViewModel.voyageDone.collect { minutes -> journalViewModel.logVoyage(minutes) }
        }

        // Avec un compte gratuit, journal et coffre partent chez le Worker peu après chaque changement
        // (et à la connexion) ; ce qu'il connaît d'autre revient sur le téléphone.
        lifecycleScope.launch { syncWithWorker() }

        setContent {
            PirateFocusTheme {
                PirateApp(focusViewModel, journalViewModel, accountViewModel)
            }
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun syncWithWorker() {
        combine(
            accountViewModel.uiState.map { it.account.token }.distinctUntilChanged(),
            journalViewModel.uiState.map { it.myLogs }.distinctUntilChanged(),
            focusViewModel.uiState.map { it.timer.doubloons to it.timer.voyages }.distinctUntilChanged(),
        ) { token, logs, chest -> Triple(token, logs, chest) }
            .debounce(SYNC_DELAY_MS)
            .collect { (token, logs, chest) ->
                if (token == null) return@collect
                val result = accountViewModel.sync(logs, chest.first, chest.second) ?: return@collect
                journalViewModel.mergeRemote(result.logs)
                focusViewModel.mergeChest(result.doubloons, result.voyages)
            }
    }

    private companion object {
        const val SYNC_DELAY_MS = 2_000L
    }
}
