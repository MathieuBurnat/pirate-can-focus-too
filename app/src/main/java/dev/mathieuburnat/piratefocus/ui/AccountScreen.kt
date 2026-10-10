package dev.mathieuburnat.piratefocus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mathieuburnat.piratefocus.account.AccountQuotes
import dev.mathieuburnat.piratefocus.account.AccountViewModel

/** « Mon compte » : pseudo, visibilité publique, déconnexion, suppression. Sans compte : de quoi en créer un. */
@Composable
fun AccountScreen(viewModel: AccountViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val account = state.account
    var name by rememberSaveable(account.pirateName) { mutableStateOf(account.pirateName) }
    var confirmDelete by remember { mutableStateOf(false) }

    // Le Worker a peut-être changé d'avis (pseudo, visibilité) depuis un autre téléphone.
    LaunchedEffect(Unit) { viewModel.refresh() }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = RectangleShape,
            title = { Text("☠ SUPPRIMER LE COMPTE ? ☠") },
            text = { Text(AccountQuotes.DELETE_WARNING, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteAccount()
                }) { Text("PAR-DESSUS BORD", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("NON, JE RESTE") } },
        )
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                "< MENU",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable(onClick = onBack).padding(vertical = 4.dp),
            )
            Text("=== MON COMPTE ===", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))

            Text("-- Pseudo --", style = MaterialTheme.typography.titleSmall)
            PirateField(name, { name = it }, "> ton pseudo de pirate")
            if (name.trim() != account.pirateName) MenuItem("RENOMMER", onClick = { viewModel.rename(name) })

            Spacer(Modifier.height(12.dp))
            if (account.signedIn) {
                Text("-- Compte gratuit --", style = MaterialTheme.typography.titleSmall)
                val via = if (account.kind == "google") "Google" else "email"
                Text("Connecté par $via${account.email?.let { " : $it" }.orEmpty()}", style = MaterialTheme.typography.bodyMedium)

                Spacer(Modifier.height(12.dp))
                Text("-- Mes données publiques --", style = MaterialTheme.typography.titleSmall)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !state.busy) { viewModel.setPublic(!account.public) }
                        .padding(vertical = 8.dp),
                ) {
                    Text(
                        if (account.public) "[x] " else "[ ] ",
                        color = if (account.public) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text("Afficher mes données publiquement", style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    if (account.public) AccountQuotes.PUBLIC_EXPLAINED else AccountQuotes.PRIVATE_EXPLAINED,
                    style = MaterialTheme.typography.bodySmall,
                )

                Spacer(Modifier.height(16.dp))
                MenuItem("SE DÉCONNECTER", onClick = viewModel::logout)
                MenuItem("SUPPRIMER MON COMPTE", onClick = { confirmDelete = true })
                BusyAndError(state)
            } else {
                NoAccountInfo()
                Text("-- Créer un compte gratuit --", style = MaterialTheme.typography.titleSmall)
                FreeAccountForm(
                    state = state,
                    onStartEmail = viewModel::startEmail,
                    onVerify = viewModel::verifyCode,
                    onChangeEmail = viewModel::changeEmail,
                )
            }
        }
    }
}
