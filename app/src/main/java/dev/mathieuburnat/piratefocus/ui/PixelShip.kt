package dev.mathieuburnat.piratefocus.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.mathieuburnat.piratefocus.focus.Phase

private val ship = listOf(
    ".......TKKK.....",
    ".......TKWK.....",
    ".......TKKK.....",
    "......WT........",
    ".....WWTW.......",
    "....WWWTWW......",
    "...WWWWTWWW.....",
    "..WWWWWTWWWW....",
    ".......T........",
    "HHHHHHHHHHHHHHHH",
    ".HHGHHHGHHHGHHH.",
    "..HHHHHHHHHHHH..",
)

/** Le navire reste au centre et tangue : ce sont les vagues qui défilent sous lui. */
@Composable
fun PixelShip(phase: Phase, modifier: Modifier = Modifier, pixelSize: Dp = 3.dp) {
    val transition = rememberInfiniteTransition(label = "houle")
    val swell by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 700), RepeatMode.Reverse),
        label = "swell",
    )

    val rows = ship.size
    Canvas(modifier = modifier.fillMaxWidth().height(pixelSize * (rows + 2))) {
        val pixel = pixelSize.toPx()
        val shipWidth = ship.maxOf { it.length } * pixel
        val x = ((size.width - shipWidth) / 2 / pixel).toInt() * pixel
        val y = when (phase) {
            Phase.FOCUS -> (swell * 2).toInt() * pixel
            Phase.SUNK -> 6 * pixel // à moitié sous l'eau
            else -> pixel
        }
        drawSprite(ship, pixel, Offset(x, y))
    }
}
