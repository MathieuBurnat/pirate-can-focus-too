package dev.mathieuburnat.piratefocus.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.delay

/**
 * Le capitaine parle comme une machine à écrire (pressée).
 * Le texte entier est mis en page dès le départ, la fin restant invisible, pour que rien ne saute.
 */
@Composable
fun TypewriterText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    charDelayMs: Long = 18,
) {
    var shown by remember(text) { mutableIntStateOf(0) }
    LaunchedEffect(text) {
        while (shown < text.length) {
            delay(charDelayMs)
            shown++
        }
    }

    val color = style.color.takeIf { it != Color.Unspecified } ?: LocalContentColor.current
    val annotated = buildAnnotatedString {
        withStyle(SpanStyle(color = color)) { append(text.take(shown)) }
        if (shown < text.length) {
            withStyle(SpanStyle(color = color)) { append('▌') }
            withStyle(SpanStyle(color = Color.Transparent)) { append(text.drop(shown + 1)) }
        }
    }
    Text(annotated, style = style, textAlign = textAlign, modifier = modifier)
}
