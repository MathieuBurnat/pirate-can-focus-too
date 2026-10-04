package dev.mathieuburnat.piratefocus.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.mathieuburnat.piratefocus.focus.Phase

/** Un caractère = un pixel. '.' = transparent. */
internal val palette = mapOf(
    'T' to Color(0xFF5C3A1E), // mât
    'H' to Color(0xFF6B4423), // coque
    'K' to Color(0xFF1B1B1B), // chapeau
    'W' to Color(0xFFF2E8CF), // tête de mort du chapeau
    'R' to Color(0xFFD64545), // bandana
    'S' to Color(0xFFE8B98A), // peau
    'P' to Color(0xFF111111), // cache-œil
    'E' to Color(0xFF2B2B2B), // œil
    'M' to Color(0xFF8C2F2F), // bouche
    'B' to Color(0xFF7A4A21), // barbe
    'G' to Color(0xFFF4C542), // boucle d'oreille en or
    'X' to Color(0xFF7FB7D9), // larme (naufrage)
)

private val captain = listOf(
    "......KKKK......",
    "....KKKKKKKK....",
    "..KKKKKWWKKKKK..",
    ".KKKKKWKKWKKKKK.",
    "KKKKKKKWWKKKKKKK",
    "..RRRRRRRRRRRR..",
    "..SSSSSSSSSSSS..",
    "..SPPPPSSSSSSS..",
    "..SPPPSSSSEESS.G",
    "..SSPSSSSSSSSS.G",
    "..SSSSSSSSSSSS..",
    "..BSSSSMMMSSSB..",
    "..BBSSSSSSSSBB..",
    "...BBBBBBBBBB...",
    "....BBBBBBBB....",
    ".....BBBBBB.....",
)

/** Version naufragée : bouche triste et une larme. */
private val sunkCaptain = captain.mapIndexed { row, line ->
    when (row) {
        10 -> "..SSSSSSSSSXSS.."
        11 -> "..BSSSSSSSSSSB.."
        12 -> "..BBSSMMMMSSBB.."
        else -> line
    }
}

@Composable
fun PixelPirate(phase: Phase, modifier: Modifier = Modifier, size: Dp = 192.dp) {
    val sprite = if (phase == Phase.SUNK) sunkCaptain else captain

    // Le capitaine tangue quand il est en mer.
    val transition = rememberInfiniteTransition(label = "tangage")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Reverse),
        label = "bob",
    )

    Canvas(modifier = modifier.size(size)) {
        val columns = sprite.maxOf { it.length }
        val pixel = this.size.minDimension / (columns + 2)
        // Déplacement par pixel entier pour garder un rendu bien "pixel art".
        val offsetY = when (phase) {
            Phase.FOCUS -> (bob * 2).toInt() * pixel
            else -> 0f
        }
        drawSprite(sprite, pixel, Offset(pixel, pixel + offsetY))
    }
}

/** Dessine une grille de caractères, un carré par pixel. */
internal fun DrawScope.drawSprite(sprite: List<String>, pixel: Float, origin: Offset) {
    sprite.forEachIndexed { y, line ->
        line.forEachIndexed { x, char ->
            val color = palette[char] ?: return@forEachIndexed
            drawRect(
                color = color,
                topLeft = Offset(origin.x + x * pixel, origin.y + y * pixel),
                size = Size(pixel, pixel),
            )
        }
    }
}
