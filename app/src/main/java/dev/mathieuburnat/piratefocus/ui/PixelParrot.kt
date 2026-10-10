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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Coco, le perroquet rouge du capitaine. */
private val parrot = listOf(
    "......QQQQ......",
    ".....QQQQQQ.....",
    "....QWEQQQQQ....",
    "...GGQQQQQQQ....",
    "...GGGQQQQQQ....",
    "....GQQQQQQQ....",
    ".....QQQQAAAQ...",
    ".....QQQAAAAAQ..",
    "......QQAAAAAQ..",
    "......QQQAAAAQ..",
    ".......QQQAAQ...",
    "........QQQQ....",
    ".........QAA....",
    ".........QAAA...",
    "..........AAA...",
    "..........A.A...",
)

/** Coco sautille d'énervement. */
@Composable
fun PixelParrot(modifier: Modifier = Modifier, size: Dp = 128.dp) {
    val transition = rememberInfiniteTransition(label = "coco")
    val hop by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 250), RepeatMode.Reverse),
        label = "hop",
    )
    Canvas(modifier = modifier.size(size)) {
        val pixel = this.size.minDimension / (parrot.maxOf { it.length } + 2)
        drawSprite(parrot, pixel, Offset(pixel, pixel - (hop * 2).toInt() * pixel + pixel))
    }
}
