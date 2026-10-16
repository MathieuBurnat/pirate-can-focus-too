package dev.mathieuburnat.piratefocus.ui

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import dev.mathieuburnat.piratefocus.account.AccountQuotes
import dev.mathieuburnat.piratefocus.focus.Phase

@Composable
fun MenuScreen(
    phase: Phase,
    signedIn: Boolean,
    onFocus: () -> Unit,
    onJournal: () -> Unit,
    onSettings: () -> Unit,
    onAccount: () -> Unit,
) {
    var showNoAccount by remember { mutableStateOf(false) }

    if (showNoAccount) {
        AlertDialog(
            onDismissRequest = { showNoAccount = false },
            shape = RectangleShape,
            title = { Text("⚓ BON À SAVOIR ⚓") },
            text = { Text(AccountQuotes.NO_ACCOUNT_INFO, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    showNoAccount = false
                    onAccount()
                }) { Text("COMPTE GRATUIT") }
            },
            dismissButton = { TextButton(onClick = { showNoAccount = false }) { Text("COMPRIS") } },
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
            MenuItem("2. MON JOURNAL", onClick = onJournal)
            MenuItem("3. PARAMÈTRES", onClick = onSettings)
            if (signedIn) {
                MenuItem("4. MA FICHE DE MATELOT", onClick = onAccount)
            } else {
                // Sans compte, le butin ne vit que sur ce téléphone : le capitaine le rappelle, gentiment.
                Text(
                    "[i] sans compte : butin rangé sur ce téléphone",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.clickable { showNoAccount = true }.padding(vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
internal fun MenuItem(label: String, onClick: () -> Unit, dimmed: Boolean = false, modifier: Modifier = Modifier) {
    Text(
        text = "> $label",
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .alpha(if (dimmed) 0.4f else 1f)
            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground), RectangleShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
    )
}
