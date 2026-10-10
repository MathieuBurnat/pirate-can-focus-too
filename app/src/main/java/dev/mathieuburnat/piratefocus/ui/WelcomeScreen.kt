package dev.mathieuburnat.piratefocus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mathieuburnat.piratefocus.account.AccountQuotes
import dev.mathieuburnat.piratefocus.account.AccountViewModel
import dev.mathieuburnat.piratefocus.account.PirateNames
import dev.mathieuburnat.piratefocus.focus.Phase

private enum class WelcomeStep { CHOICE, ANONYMOUS, FREE_ACCOUNT }

/** Premier lancement : matelot anonyme ou compte gratuit. Aucun compte n'est obligatoire. */
@Composable
fun WelcomeScreen(viewModel: AccountViewModel, onDone: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableStateOf(WelcomeStep.CHOICE) }
    var name by rememberSaveable { mutableStateOf(PirateNames.random()) }

    // Le code est validé : le compte gratuit est prêt, on lève l'ancre.
    LaunchedEffect(state.account.signedIn) {
        if (state.account.signedIn) onDone()
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (step != WelcomeStep.CHOICE) {
                Text(
                    "< RETOUR",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .clickable {
                            viewModel.clearError()
                            step = WelcomeStep.CHOICE
                        }
                        .padding(vertical = 4.dp),
                )
            }
            Text("=== BIENVENUE À BORD ===", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            PixelPirate(phase = Phase.IDLE)
            Spacer(Modifier.height(16.dp))

            when (step) {
                WelcomeStep.CHOICE -> {
                    Text(AccountQuotes.WELCOME, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    MenuItem("1. MATELOT ANONYME", onClick = { step = WelcomeStep.ANONYMOUS })
                    MenuItem("2. COMPTE GRATUIT", onClick = { step = WelcomeStep.FREE_ACCOUNT })
                }
                WelcomeStep.ANONYMOUS -> {
                    Text(AccountQuotes.ANONYMOUS, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    PirateField(name, { name = it }, "> ton pseudo de pirate")
                    Text(
                        "[dé] un autre pseudo",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .clickable { name = PirateNames.random() }
                            .padding(vertical = 6.dp),
                    )
                    NoAccountWarning()
                    MenuItem("LEVER L'ANCRE", onClick = { if (viewModel.chooseAnonymous(name)) onDone() })
                    BusyAndError(state)
                }
                WelcomeStep.FREE_ACCOUNT -> FreeAccountForm(
                    state = state,
                    onStartEmail = viewModel::startEmail,
                    onVerify = viewModel::verifyCode,
                    onChangeEmail = viewModel::changeEmail,
                )
            }
        }
    }
}
