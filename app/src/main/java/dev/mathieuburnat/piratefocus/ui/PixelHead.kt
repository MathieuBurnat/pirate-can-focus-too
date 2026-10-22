package dev.mathieuburnat.piratefocus.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.mathieuburnat.piratefocus.player.PirateHeads

/** La tête d'un matelot (l'une des têtes prédéfinies de [PirateHeads]), en pixel art. */
@Composable
fun PixelHead(head: Int, modifier: Modifier = Modifier, size: Dp = 96.dp) {
    val pirate = PirateHeads[head]
    val colors = remember(head) { pirate.colors.mapValues { Color(it.value) } }
    Canvas(modifier.size(size)) {
        val pixel = this.size.minDimension / (pirate.sprite.size + 2)
        drawSprite(pirate.sprite, pixel, Offset(pixel, pixel), colors)
    }
}
