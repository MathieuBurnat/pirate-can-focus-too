package dev.mathieuburnat.piratefocus.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mathieuburnat.piratefocus.BuildConfig

/** Petite version discrète en bas des pages (0.1.x, x = nombre de commits). */
@Composable
fun VersionFooter(modifier: Modifier = Modifier) {
    Text(
        text = "v${BuildConfig.VERSION_NAME}",
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 4.dp)
            .alpha(0.35f),
    )
}
