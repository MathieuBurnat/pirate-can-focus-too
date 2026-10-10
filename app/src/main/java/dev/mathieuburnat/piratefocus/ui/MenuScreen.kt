package dev.mathieuburnat.piratefocus.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.mathieuburnat.piratefocus.account.AccountQuotes
import dev.mathieuburnat.piratefocus.focus.Phase
import dev.mathieuburnat.piratefocus.journal.JournalQuotes

/** Nombre de coups à frapper sur le journal verrouillé pour que le capitaine cède. */
private const val KNOCKS_TO_UNLOCK = 7

@Composable
fun MenuScreen(
    phase: Phase,
    journalUnlocked: Boolean,
    signedIn: Boolean,
    onFocus: () -> Unit,
    onUnlockJournal: () -> Unit,
    onJournal: () -> Unit,
    onSettings: () -> Unit,
    onAccount: () -> Unit,
) {
    val context = LocalContext.current
    var knocks by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<Toast?>(null) }
    var showUnlock by remember { mutableStateOf(false) }
    var showNoAccount by remember { mutableStateOf(false) }

    if (showNoAccount) {
        AlertDialog(
            onDismissRequest = { showNoAccount = false },
            shape = RectangleShape,
            title = { Text("☠ PAS DE COMPTE ☠") },
            text = { Text(AccountQuotes.NO_ACCOUNT_WARNING, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    showNoAccount = false
                    onAccount()
                }) { Text("COMPTE GRATUIT") }
            },
            dismissButton = { TextButton(onClick = { showNoAccount = false }) { Text("COMPRIS") } },
        )
    }

    if (showUnlock) {
        AlertDialog(
            onDismissRequest = { showUnlock = false },
            shape = RectangleShape,
            title = { Text("☠ LE CAPITAINE ☠") },
            text = { TypewriterText(JournalQuotes.UNLOCKED, style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = {
                    showUnlock = false
                    onUnlockJournal()
                    onJournal()
                }) { Text("ENTRER") }
            },
        )
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("=== PIRATE FOCUS ===", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text("~ un pirate aussi peut se concentrer ~", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(24.dp))
            PixelPirate(phase = Phase.IDLE)
            Spacer(Modifier.height(32.dp))

            val focusLabel = if (phase == Phase.FOCUS) "1. FOCUS  (traversée en cours)" else "1. FOCUS"
            MenuItem(focusLabel, onClick = onFocus)
            if (journalUnlocked) {
                MenuItem("2. MON JOURNAL  (en travaux)", onClick = onJournal)
            } else {
                // Grisé... mais pas tout à fait fermé.
                MenuItem("2. MON JOURNAL  (bientôt)", dimmed = true, onClick = {
                    knocks++
                    if (knocks >= KNOCKS_TO_UNLOCK) {
                        toast?.cancel()
                        showUnlock = true
                    } else {
                        JournalQuotes.knock(KNOCKS_TO_UNLOCK - knocks)?.let { message ->
                            toast?.cancel()
                            toast = Toast.makeText(context, message, Toast.LENGTH_SHORT).also { it.show() }
                        }
                    }
                })
            }
            MenuItem("3. PARAMÈTRES", onClick = onSettings)
            if (signedIn) {
                MenuItem("4. MON COMPTE", onClick = onAccount)
            } else {
                // Sans compte, le butin ne vit que sur ce téléphone : le capitaine le rappelle.
                Text(
                    "[!] pas de compte : butin non sauvegardé",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable { showNoAccount = true }.padding(vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
internal fun MenuItem(label: String, onClick: () -> Unit, dimmed: Boolean = false) {
    Text(
        text = "> $label",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .alpha(if (dimmed) 0.4f else 1f)
            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground), RectangleShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
    )
}
