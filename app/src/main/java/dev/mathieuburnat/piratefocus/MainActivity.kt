package dev.mathieuburnat.piratefocus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.mathieuburnat.piratefocus.focus.ChestStore
import dev.mathieuburnat.piratefocus.focus.FocusViewModel
import dev.mathieuburnat.piratefocus.focus.Phase
import dev.mathieuburnat.piratefocus.guard.FocusGuardService
import dev.mathieuburnat.piratefocus.guard.GuardPermissions
import dev.mathieuburnat.piratefocus.ui.PirateApp
import dev.mathieuburnat.piratefocus.ui.theme.PirateFocusTheme
import dev.mathieuburnat.piratefocus.journal.JournalStore
import dev.mathieuburnat.piratefocus.journal.JournalViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Les deux ViewModels reçoivent leur coffre et leur journal rangés sur le téléphone.
    private val focusViewModel: FocusViewModel by viewModels {
        viewModelFactory { initializer { FocusViewModel(ChestStore(application)) } }
    }
    private val journalViewModel: JournalViewModel by viewModels {
        viewModelFactory { initializer { JournalViewModel(JournalStore(application)) } }
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

        // Chaque traversée terminée s'inscrit au journal de bord (drop(1) : on ignore l'état déjà connu).
        lifecycleScope.launch {
            focusViewModel.uiState.map { it.timer }.distinctUntilChangedBy { it.voyages }.drop(1).collect { timer ->
                journalViewModel.logVoyage(timer.focusMinutes)
            }
        }

        setContent {
            PirateFocusTheme {
                PirateApp(focusViewModel, journalViewModel)
            }
        }
    }
}
