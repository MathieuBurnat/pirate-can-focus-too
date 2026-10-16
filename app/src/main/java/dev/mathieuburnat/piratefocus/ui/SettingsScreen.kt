package dev.mathieuburnat.piratefocus.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.mathieuburnat.piratefocus.focus.Phase
import kotlin.math.abs

/** Coups à donner sur le titre des paramètres pour faire apparaître le mode dev. */
private const val TAPS_TO_REVEAL = 5

/** Les paramètres : applications bloquées, déconnexion, et le mode dev caché (5 taps sur le titre). */
@Composable
fun SettingsScreen(
    devMode: Boolean,
    devRevealed: Boolean,
    signedIn: Boolean,
    onBack: () -> Unit,
    onBlockedApps: () -> Unit,
    onRevealDev: () -> Unit,
    onDevModeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
) {
    var taps by remember { mutableIntStateOf(0) }
    var reveals by remember { mutableIntStateOf(0) }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                "< MENU",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable(onClick = onBack).padding(vertical = 4.dp),
            )
            Text(
                "=== PARAMÈTRES ===",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                // Pas d'effet de pression : le passage secret ne doit pas se voir.
                modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    if (devRevealed) return@clickable
                    if (++taps >= TAPS_TO_REVEAL) {
                        taps = 0
                        onRevealDev()
                        reveals++
                    }
                },
            )
            if (!devRevealed && taps >= 2) {
                val left = TAPS_TO_REVEAL - taps
                Text(
                    "(encore $left coup${if (left > 1) "s" else ""} de sabre...)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(Modifier.height(16.dp))

            var number = 1
            MenuItem("${number++}. APPLICATIONS BLOQUÉES", onClick = onBlockedApps)
            if (devRevealed) DevModeItem("${number++}.", devMode, reveals, onDevModeChange)
            if (signedIn) MenuItem("${number}. SE DÉCONNECTER", onClick = onLogout)
        }
    }
}

/**
 * Le bouton « Mode dev » : il se trémousse et lâche des paillettes en apparaissant.
 * Un tap ouvre son menu : activer ou désactiver le mode dev, et dire bonjour à papa.
 */
@Composable
private fun DevModeItem(number: String, devMode: Boolean, reveals: Int, onDevModeChange: (Boolean) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var hello by remember { mutableStateOf(false) }
    val wiggle = remember { Animatable(0f) }

    LaunchedEffect(reveals) {
        if (reveals == 0) return@LaunchedEffect
        for (angle in listOf(-6f, 6f, -5f, 5f, -3f, 3f, -1f, 0f)) wiggle.animateTo(angle, tween(70))
    }

    Box {
        MenuItem(
            "$number MODE DEV  ${if (devMode) "[x]" else "[ ]"}  ${if (open) "▲" else "▼"}",
            onClick = { open = true },
            modifier = Modifier.graphicsLayer {
                rotationZ = wiggle.value
                scaleX = 1f + abs(wiggle.value) / 60f
                scaleY = scaleX
            },
        )
        SparkleBurst(reveals, Modifier.matchParentSize(), originX = 0.3f)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = RectangleShape) {
            DropdownMenuItem(
                text = { Text(if (devMode) "DÉSACTIVER LE MODE DEV" else "ACTIVER LE MODE DEV") },
                onClick = {
                    onDevModeChange(!devMode)
                    open = false
                },
            )
            DropdownMenuItem(
                text = { Text("SALUT PAPA") },
                onClick = {
                    hello = true
                    open = false
                },
            )
        }
    }
    Text(
        if (devMode) {
            "Les statistiques de l'équipage montrent des matelots inventés, codés en dur dans l'app."
        } else {
            "Les statistiques de l'équipage montrent les vrais pirates publics. Aucune fausse donnée en base."
        },
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    if (hello) HelloDadDialog(onDismiss = { hello = false })
}

/** « SALUT PAPA » : le capitaine sourit, et les paillettes pleuvent sans fin. */
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
                        "SALUT PAPA !",
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
