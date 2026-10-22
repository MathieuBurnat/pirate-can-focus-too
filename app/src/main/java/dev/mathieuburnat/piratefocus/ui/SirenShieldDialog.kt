package dev.mathieuburnat.piratefocus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.mathieuburnat.piratefocus.focus.FocusTimer
import dev.mathieuburnat.piratefocus.guard.SirenShield
import kotlinx.coroutines.delay

/**
 * La fenêtre du bouclier anti-sirènes (mode dev) : l'allumer ou l'éteindre, choisir la durée du blocage,
 * voir le compte à rebours d'Instagram bloqué et le lever d'un geste. Rien de compliqué pour le matelot.
 */
@Composable
fun SirenShieldDialog(
    on: Boolean,
    minutes: Int,
    blockedUntil: Long,
    accessibilityReady: Boolean,
    onToggle: (Boolean) -> Unit,
    onMinutes: (Int) -> Unit,
    onUnblock: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onDismiss: () -> Unit,
) {
    // L'horloge du compte à rebours, rafraîchie chaque seconde.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val secondsLeft = ((blockedUntil - now) / 1_000).toInt()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RectangleShape,
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RectangleShape),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "⚓ BOUCLIER ANTI-SIRÈNES ⚓",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                MenuItem(if (on) "SIRÈNES MUSELÉES  [x]" else "MUSELER LES SIRÈNES  [ ]", onClick = { onToggle(!on) })
                Text(
                    "Les stories d'Instagram se referment, et au ${SirenShield.SCROLL_LIMIT}e défilement le capitaine te chasse " +
                        "puis bloque Instagram.",
                    style = MaterialTheme.typography.bodySmall,
                )

                if (on) {
                    Spacer(Modifier.height(12.dp))
                    Text("-- Durée du blocage --", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        SirenShield.BLOCK_CHOICES.forEach { choice ->
                            val selected = choice == minutes
                            val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            Text(
                                "$choice min",
                                style = MaterialTheme.typography.labelMedium,
                                color = color,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .border(BorderStroke(if (selected) 2.dp else 1.dp, color), RectangleShape)
                                    .clickable { onMinutes(choice) }
                                    .padding(vertical = 10.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    if (secondsLeft > 0) {
                        Text(
                            "Instagram bloqué encore ${FocusTimer.format(secondsLeft)}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        MenuItem("LEVER LE BLOCAGE", onClick = onUnblock)
                    } else {
                        Text("Instagram est libre... pour l'instant.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }

                    // Le bouclier a besoin d'un service d'accessibilité, à activer à la main dans les réglages du téléphone.
                    if (!accessibilityReady) {
                        Text(
                            "> ACTIVER « Pirate Focus : bouclier anti-sirènes » DANS L'ACCESSIBILITÉ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable(onClick = onOpenAccessibility).padding(vertical = 8.dp),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "fermer",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally).clickable(onClick = onDismiss).padding(8.dp),
                )
            }
        }
    }
}
