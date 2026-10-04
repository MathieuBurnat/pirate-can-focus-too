package dev.mathieuburnat.piratefocus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private val ItemHeight = 44.dp
private const val VisibleItems = 5

/** Roue de sélection des minutes, façon horloge rétro : on fait défiler, ça s'aimante au centre. */
@Composable
fun MinutesWheel(
    range: IntRange,
    initial: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val values = remember(range) { range.toList() }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = values.indexOf(initial.coerceIn(range)).coerceAtLeast(0),
    )

    // L'élément dont le centre est le plus proche du milieu de la roue.
    val centeredIndex by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val middle = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - middle) }?.index
                ?: listState.firstVisibleItemIndex
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { centeredIndex }.collect { onValueChange(values[it]) }
    }

    val padding = ItemHeight * (VisibleItems / 2)
    Box(modifier = modifier.height(ItemHeight * VisibleItems), contentAlignment = Alignment.Center) {
        // Bandeau de sélection au centre.
        Box(
            Modifier
                .fillMaxWidth()
                .height(ItemHeight)
                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RectangleShape),
        )
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(vertical = padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(values.size) { index ->
                val distance = abs(index - centeredIndex)
                val selected = distance == 0
                Box(Modifier.height(ItemHeight).width(160.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "%03d min".format(values[index]),
                        fontSize = if (selected) 24.sp else 18.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.alpha(if (selected) 1f else (1f - 0.3f * distance).coerceAtLeast(0.2f)),
                    )
                }
            }
        }
    }
}
