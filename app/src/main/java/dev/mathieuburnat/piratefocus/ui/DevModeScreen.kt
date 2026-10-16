package dev.mathieuburnat.piratefocus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.mathieuburnat.piratefocus.focus.Phase

/**
 * La page du mode dev (dénichée dans les paramètres) : l'activer ou non, et, mode dev allumé,
 * choisir les fausses données de l'équipage et dire bonjour à papa.
 */
@Composable
fun DevModeScreen(
    devMode: Boolean,
    fakeData: Boolean,
    onBack: () -> Unit,
    onDevModeChange: (Boolean) -> Unit,
    onFakeDataChange: (Boolean) -> Unit,
) {
    var hello by remember { mutableStateOf(false) }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                "< PARAMÈTRES",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable(onClick = onBack).padding(vertical = 4.dp),
            )
            Text("=== MODE DEV ===", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))

            MenuItem("1. MODE DEV  ${if (devMode) "[x]" else "[ ]"}", onClick = { onDevModeChange(!devMode) })
            if (!devMode) return@Column
            MenuItem("2. FAKE DATA  ${if (fakeData) "[x]" else "[ ]"}", onClick = { onFakeDataChange(!fakeData) })
            Text(
                if (fakeData) {
                    "Les statistiques de l'équipage montrent des matelots inventés, codés en dur dans l'app."
                } else {
                    "Les statistiques de l'équipage montrent les vrais pirates publics. Aucune fausse donnée en base."
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            MenuItem("3. HI DAAD", onClick = { hello = true })
        }
    }

    if (hello) HelloDadDialog(onDismiss = { hello = false })
}

/** « HI DAAD » : le capitaine sourit, et les paillettes pleuvent sans fin. */
@Composable
private fun HelloDadDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box {
            Surface(
                shape = RectangleShape,
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RectangleShape),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp),
                ) {
                    PixelPirate(phase = Phase.IDLE, happy = true, lookingAtYou = true, size = 128.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "HI DAAD !",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tout l'équipage te salue bien bas !",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    MenuItem("À L'ABORDAGE !", onClick = onDismiss)
                }
            }
            SparkleBurst(trigger = 1, modifier = Modifier.matchParentSize(), count = 70, spread = 1.6f, forever = true)
        }
    }
}
