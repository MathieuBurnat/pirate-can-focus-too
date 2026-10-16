package dev.mathieuburnat.piratefocus.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

/** Coups à donner sur « Mode dev » pour l'activer. */
private const val TAPS_TO_DEV = 5

/** Les paramètres : applications bloquées, mode dev et déconnexion. */
@Composable
fun SettingsScreen(
    devMode: Boolean,
    signedIn: Boolean,
    onBack: () -> Unit,
    onBlockedApps: () -> Unit,
    onDevModeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                "< MENU",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable(onClick = onBack).padding(vertical = 4.dp),
            )
            Text("=== PARAMÈTRES ===", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))

            MenuItem("1. APPLICATIONS BLOQUÉES", onClick = onBlockedApps)
            DevModeItem(devMode, onDevModeChange)
            Text(
                if (devMode) {
                    "Les statistiques de l'équipage montrent des matelots inventés, codés en dur dans l'app."
                } else {
                    "Les statistiques de l'équipage montrent les vrais pirates publics. Aucune fausse donnée en base."
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (signedIn) MenuItem("3. SE DÉCONNECTER", onClick = onLogout)
        }
    }
}

/**
 * « Mode dev » : il faut taper [TAPS_TO_DEV] fois pour l'activer. Le bouton se trémousse alors
 * et lâche une gerbe de paillettes. Un seul tap suffit pour l'éteindre.
 */
@Composable
private fun DevModeItem(devMode: Boolean, onDevModeChange: (Boolean) -> Unit) {
    var taps by remember { mutableIntStateOf(0) }
    var bursts by remember { mutableIntStateOf(0) }
    val wiggle = remember { Animatable(0f) }
    val sparkle = remember { Animatable(1f) }
    val sparkles = remember(bursts) { List(SPARKLE_COUNT) { Sparkle.random() } }

    LaunchedEffect(bursts) {
        if (bursts == 0) return@LaunchedEffect
        launch {
            sparkle.snapTo(0f)
            sparkle.animateTo(1f, tween(1400, easing = LinearOutSlowInEasing))
        }
        for (angle in listOf(-6f, 6f, -5f, 5f, -3f, 3f, -1f, 0f)) wiggle.animateTo(angle, tween(70))
    }

    Box {
        MenuItem(
            "2. MODE DEV  ${if (devMode) "[x]" else "[ ]"}",
            onClick = {
                if (devMode) {
                    taps = 0
                    onDevModeChange(false)
                } else if (++taps >= TAPS_TO_DEV) {
                    taps = 0
                    onDevModeChange(true)
                    bursts++
                }
            },
            modifier = Modifier.graphicsLayer {
                rotationZ = wiggle.value
                scaleX = 1f + abs(wiggle.value) / 60f
                scaleY = scaleX
            },
        )
        // Les paillettes jaillissent du bouton puis retombent en scintillant.
        Canvas(Modifier.matchParentSize()) {
            val t = sparkle.value
            if (t >= 1f) return@Canvas
            val px = 3.dp.toPx()
            val origin = Offset(size.width * 0.3f, size.height / 2)
            sparkles.forEach { s ->
                val x = origin.x + cos(s.angle) * s.speed * t * px
                val y = origin.y + sin(s.angle) * s.speed * t * px + 40f * t * t * px
                val alpha = (1f - t) * (if (((t * 20).toInt() + s.phase) % 3 == 0) 0.4f else 1f)
                // Un pixel, ou une petite croix scintillante.
                val cells = if (s.star) listOf(0 to 0, -1 to 0, 1 to 0, 0 to -1, 0 to 1) else listOf(0 to 0)
                cells.forEach { (dx, dy) ->
                    drawRect(
                        s.color.copy(alpha = alpha),
                        topLeft = Offset((x / px).toInt() * px + dx * px, (y / px).toInt() * px + dy * px),
                        size = Size(px, px),
                    )
                }
            }
        }
    }
    if (!devMode && taps > 0) {
        Text(
            "(encore ${TAPS_TO_DEV - taps} coup${if (TAPS_TO_DEV - taps > 1) "s" else ""} de sabre...)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

private const val SPARKLE_COUNT = 40

private class Sparkle(val angle: Float, val speed: Float, val color: Color, val star: Boolean, val phase: Int) {
    companion object {
        private val colors = listOf('G', 'W', 'R', 'A', 'C').map { palette.getValue(it) }

        fun random() = Sparkle(
            angle = Random.nextFloat() * 2f * PI.toFloat(),
            speed = 20f + Random.nextFloat() * 45f,
            color = colors.random(),
            star = Random.nextInt(4) == 0,
            phase = Random.nextInt(3),
        )
    }
}
