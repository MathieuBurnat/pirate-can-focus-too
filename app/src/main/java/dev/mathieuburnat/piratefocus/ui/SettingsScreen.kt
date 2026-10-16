package dev.mathieuburnat.piratefocus.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
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
    onDevMode: () -> Unit,
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
            if (devRevealed) DevModeItem("${number++}.", devMode, reveals, onDevMode)
            if (signedIn) MenuItem("${number}. SE DÉCONNECTER", onClick = onLogout)
        }
    }
}

/** Le bouton « Mode dev » : il se trémousse et lâche des paillettes en apparaissant, puis ouvre sa page. */
@Composable
private fun DevModeItem(number: String, devMode: Boolean, reveals: Int, onClick: () -> Unit) {
    val wiggle = remember { Animatable(0f) }

    LaunchedEffect(reveals) {
        if (reveals == 0) return@LaunchedEffect
        for (angle in listOf(-6f, 6f, -5f, 5f, -3f, 3f, -1f, 0f)) wiggle.animateTo(angle, tween(70))
    }

    Box {
        MenuItem(
            "$number MODE DEV  ${if (devMode) "[x]" else "[ ]"}  >",
            onClick = onClick,
            modifier = Modifier.graphicsLayer {
                rotationZ = wiggle.value
                scaleX = 1f + abs(wiggle.value) / 60f
                scaleY = scaleX
            },
        )
        SparkleBurst(reveals, Modifier.matchParentSize(), originX = 0.3f)
    }
}
