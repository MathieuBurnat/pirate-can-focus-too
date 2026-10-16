package dev.mathieuburnat.piratefocus.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val BURST_MS = 1400

/**
 * Une gerbe de paillettes en pixel art qui jaillit de [originX] (fraction de la largeur), puis retombe en scintillant.
 * Elle part à chaque changement de [trigger] (0 = rien) ; avec [forever], elle repart sans fin.
 * À poser par-dessus un élément avec `Modifier.matchParentSize()` : elle déborde volontiers.
 */
@Composable
fun SparkleBurst(
    trigger: Int,
    modifier: Modifier = Modifier,
    count: Int = 40,
    spread: Float = 1f,
    originX: Float = 0.5f,
    forever: Boolean = false,
) {
    var wave by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(1f) }
    val sparkles = remember(trigger, wave) { List(count) { Sparkle.random(spread) } }

    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        do {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(BURST_MS, easing = LinearOutSlowInEasing))
            wave++
        } while (forever)
    }

    Canvas(modifier) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val px = 3.dp.toPx()
        val origin = Offset(size.width * originX, size.height / 2)
        sparkles.forEach { s ->
            val x = origin.x + cos(s.angle) * s.speed * t * px
            val y = origin.y + sin(s.angle) * s.speed * t * px + 40f * t * t * px
            val alpha = (1f - t) * (if (((t * 20).toInt() + s.phase) % 3 == 0) 0.4f else 1f)
            // Un pixel, ou une petite croix qui scintille.
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

private class Sparkle(val angle: Float, val speed: Float, val color: Color, val star: Boolean, val phase: Int) {
    companion object {
        private val colors = listOf('G', 'W', 'R', 'A', 'C').map { palette.getValue(it) }

        fun random(spread: Float) = Sparkle(
            angle = Random.nextFloat() * 2f * PI.toFloat(),
            speed = (20f + Random.nextFloat() * 45f) * spread,
            color = colors.random(),
            star = Random.nextInt(4) == 0,
            phase = Random.nextInt(3),
        )
    }
}
