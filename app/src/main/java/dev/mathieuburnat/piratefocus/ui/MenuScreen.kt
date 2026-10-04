package dev.mathieuburnat.piratefocus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import dev.mathieuburnat.piratefocus.focus.Phase

@Composable
fun MenuScreen(phase: Phase, onFocus: () -> Unit, onSettings: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("=== PIRATE FOCUS ===", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text("~ un pirate aussi peut se concentrer ~", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(24.dp))
            PixelPirate(phase = Phase.IDLE)
            Spacer(Modifier.height(32.dp))

            val focusLabel = if (phase == Phase.FOCUS) "1. FOCUS  (traversée en cours)" else "1. FOCUS"
            MenuItem(focusLabel, onClick = onFocus)
            MenuItem("2. MON JOURNAL  (bientôt)", onClick = null)
            MenuItem("3. PARAMÈTRES", onClick = onSettings)
        }
    }
}

@Composable
private fun MenuItem(label: String, onClick: (() -> Unit)?) {
    val enabled = onClick != null
    Text(
        text = "> $label",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground), RectangleShape)
            .clickable(enabled = enabled) { onClick?.invoke() }
            .padding(16.dp),
    )
}
