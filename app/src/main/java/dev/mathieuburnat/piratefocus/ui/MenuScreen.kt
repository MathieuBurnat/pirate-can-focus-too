package dev.mathieuburnat.piratefocus.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import dev.mathieuburnat.piratefocus.account.AccountQuotes
import dev.mathieuburnat.piratefocus.account.AuthMode
import dev.mathieuburnat.piratefocus.focus.Phase

@Composable
fun MenuScreen(
    phase: Phase,
    signedIn: Boolean,
    devMode: Boolean,
    onFocus: () -> Unit,
    onJournal: () -> Unit,
    onSettings: () -> Unit,
    onAccount: () -> Unit,
    onSignIn: (AuthMode) -> Unit,
) {
    var showNoAccount by remember { mutableStateOf(false) }

    if (showNoAccount) {
        ChoiceDialog(
            title = "⚓ COMPTE LOCAL ⚓",
            onDismiss = { showNoAccount = false },
            text = { Text(AccountQuotes.NO_ACCOUNT_INFO, style = MaterialTheme.typography.bodyMedium) },
            choices = listOf(
                Choice("CRÉER UN COMPTE", ChoiceKind.ADVISED, {
                    showNoAccount = false
                    onSignIn(AuthMode.REGISTER)
                }),
                Choice("J'AI DÉJÀ UN COMPTE", ChoiceKind.NORMAL, {
                    showNoAccount = false
                    onSignIn(AuthMode.LOGIN)
                }),
                Choice("rester en local", ChoiceKind.CANCEL, { showNoAccount = false }),
            ),
        )
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        // Centré quand tout tient ; sur un petit écran, le menu défile au lieu d'écraser les derniers boutons.
        BoxWithConstraints(Modifier.safeDrawingPadding()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("=== PIRATE FOCUS ===", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text("~ un pirate aussi peut se concentrer ~", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                if (!signedIn) {
                    // Sans compte, le butin ne vit que sur ce téléphone : un petit tag le rappelle, et ouvre la porte d'un compte.
                    LocalAccountTag(onClick = { showNoAccount = true })
                }
                Spacer(Modifier.height(24.dp))
                PixelPirate(phase = Phase.IDLE)
                Spacer(Modifier.height(32.dp))

                val focusLabel = if (phase == Phase.FOCUS) "1. FOCUS  (traversée en cours)" else "1. FOCUS"
                MenuItem(focusLabel, onClick = onFocus)
                MenuItem("2. MON JOURNAL", onClick = onJournal)
                MenuItem("3. PARAMÈTRES", onClick = onSettings)
                // La fiche est l'écran du pirate connecté (bientôt son personnage et ses points). Matelot local :
                // il passe par le tag « Compte local », sauf en mode dev.
                if (signedIn || devMode) MenuItem("4. MA FICHE DE MATELOT", onClick = onAccount)
            }
        }
    }
}

/** Délai entre deux trémoussements du tag « [ Compte local ] ». */
private const val TAG_WIGGLE_EVERY_MS = 10_000L

/** « [ Compte local ] » : discret, mais il se trémousse toutes les 10 secondes pour qu'on le remarque. */
@Composable
private fun LocalAccountTag(onClick: () -> Unit) {
    val wiggle = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(TAG_WIGGLE_EVERY_MS)
            for (angle in listOf(-4f, 4f, -3f, 3f, -1f, 0f)) wiggle.animateTo(angle, tween(60))
        }
    }
    Text(
        "[ Compte local ]",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier
            .padding(top = 4.dp)
            .graphicsLayer { rotationZ = wiggle.value }
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
internal fun MenuItem(
    label: String,
    onClick: () -> Unit,
    dimmed: Boolean = false,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    Text(
        text = "> $label",
        style = MaterialTheme.typography.titleMedium,
        color = color,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .alpha(if (dimmed) 0.4f else 1f)
            .border(BorderStroke(2.dp, color), RectangleShape)
            .clickable(onClick = onClick)
            .padding(16.dp),
    )
}
