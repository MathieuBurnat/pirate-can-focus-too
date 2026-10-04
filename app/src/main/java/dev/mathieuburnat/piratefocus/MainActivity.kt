package dev.mathieuburnat.piratefocus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import dev.mathieuburnat.piratefocus.focus.FocusViewModel
import dev.mathieuburnat.piratefocus.focus.Phase
import dev.mathieuburnat.piratefocus.guard.FocusGuardService
import dev.mathieuburnat.piratefocus.guard.GuardPermissions
import dev.mathieuburnat.piratefocus.ui.PirateApp
import dev.mathieuburnat.piratefocus.ui.theme.PirateFocusTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val focusViewModel: FocusViewModel by viewModels()

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

        setContent {
            PirateFocusTheme {
                PirateApp(focusViewModel)
            }
        }
    }
}
