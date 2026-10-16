package dev.mathieuburnat.piratefocus.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import dev.mathieuburnat.piratefocus.focus.Phase

/**
 * La page du mode dev (dénichée dans les paramètres) : l'activer ou non, et, mode dev allumé,
 * choisir les fausses données de l'équipage, ou tout couler (factory reset). 2 taps sur le titre : dire bonjour à papa.
 */
@Composable
fun DevModeScreen(
    devMode: Boolean,
    fakeData: Boolean,
    onBack: () -> Unit,
    onDevModeChange: (Boolean) -> Unit,
    onFakeDataChange: (Boolean) -> Unit,
    onFactoryReset: () -> Unit,
) {
    var hello by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var reallySure by remember { mutableStateOf(false) }
    var titleTaps by remember { mutableIntStateOf(0) }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            shape = RectangleShape,
            title = { Text("☠ FACTORY RESET ☠") },
            text = {
                Text(
                    "Tout ce qui est rangé sur ce téléphone sera effacé : pseudo, journal de bord, " +
                        "coffre et doublons, mode dev et applis bloquées.\n\n" +
                        "Avec un compte gratuit, tes données restent sauvegardées en ligne et reviennent à ta prochaine connexion. " +
                        "En matelot anonyme, tout coule pour de bon !",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    reallySure = true
                }) { Text("TOUT COULER") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("GARDER LE CAP") } },
        )
    }

    if (reallySure) {
        ReallySureDialog(
            onConfirm = {
                reallySure = false
                onFactoryReset()
            },
            onCancel = { reallySure = false },
        )
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                "< PARAMÈTRES",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable(onClick = onBack).padding(vertical = 4.dp),
            )
            // 2 taps sur le titre (sans effet de pression) et HI DAAD se pointe.
            Text(
                "=== MODE DEV ===",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    titleTaps++
                },
            )
            Spacer(Modifier.height(16.dp))

            MenuItem("1. MODE DEV  ${if (devMode) "[x]" else "[ ]"}", onClick = { onDevModeChange(!devMode) })
            // Mode dev allumé : les autres options défilent depuis la gauche, l'une après l'autre.
            ScrollIn(devMode, order = 0) {
                Column {
                    MenuItem("2. FAKE DATA  ${if (fakeData) "[x]" else "[ ]"}", onClick = { onFakeDataChange(!fakeData) })
                    Text(
                        if (fakeData) {
                            "Les statistiques de l'équipage montrent des matelots inventés, codés en dur dans l'app."
                        } else {
                            "Les statistiques de l'équipage montrent les vrais pirates publics. Aucune fausse donnée en base."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
            ScrollIn(devMode, order = 1) { MenuItem("3. FACTORY RESET", onClick = { confirmReset = true }) }
            ScrollIn(devMode && titleTaps >= HELLO_TAPS, order = 0) { MenuItem("4. HI DAAD", onClick = { hello = true }) }
        }
    }

    if (hello) HelloDadDialog(onDismiss = { hello = false })
}

/** Secondes à attendre avant de pouvoir confirmer le factory reset. */
private const val CONFIRM_WAIT_S = 5

/** Dernière chance avant de tout couler : « CONFIRMER » ne répond qu'après [CONFIRM_WAIT_S] secondes. */
@Composable
private fun ReallySureDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    var left by remember { mutableIntStateOf(CONFIRM_WAIT_S) }
    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1_000)
            left--
        }
    }
    AlertDialog(
        onDismissRequest = onCancel,
        shape = RectangleShape,
        title = { Text("☠ ATTENTION ☠", maxLines = 1, softWrap = false, overflow = TextOverflow.Clip) },
        text = {
            TypewriterText(
                "T'es vraiment sûr ? Toutes les données seront coulées à jamais !!",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = { ConfirmButton(left, onConfirm) },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(
                    "non, annuler capitaine !",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        },
    )
}

/**
 * « CONFIRMER » en rouge : bordure blanche en pointillés et compte à rebours tant qu'il faut patienter,
 * puis, d'un coup, bordure rouge pleine et bouton cliquable.
 */
@Composable
private fun ConfirmButton(left: Int, onConfirm: () -> Unit) {
    val ready = left == 0
    val red = MaterialTheme.colorScheme.error
    Text(
        if (ready) "CONFIRMER" else "CONFIRMER ($left)",
        style = MaterialTheme.typography.labelLarge,
        color = if (ready) red else red.copy(alpha = 0.5f),
        modifier = Modifier
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawRect(
                    color = if (ready) red else Color.White,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(
                        width = stroke,
                        pathEffect = if (ready) null else PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            }
            .clickable(enabled = ready, onClick = onConfirm)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** Taps sur le titre pour faire apparaître HI DAAD. */
private const val HELLO_TAPS = 2

/** Délai entre deux options qui défilent. */
private const val SCROLL_STAGGER_MS = 90

/** Une option qui arrive en défilant (aussi à l'ouverture de la page), [order] fois [SCROLL_STAGGER_MS] après la première. */
@Composable
private fun ScrollIn(visible: Boolean, order: Int, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    val delay = order * SCROLL_STAGGER_MS
    AnimatedVisibility(
        visibleState = state,
        enter = expandVertically(tween(220, delayMillis = delay)) +
            slideInHorizontally(tween(320, delayMillis = delay)) { -it } +
            fadeIn(tween(320, delayMillis = delay)),
        exit = shrinkVertically() + fadeOut(),
    ) { content() }
}

/** « HI DAAD » : le capitaine sourit, et les paillettes pleuvent sans fin. */
@Composable
private fun HelloDadDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box {
            Surface(
                shape = RectangleShape,
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RectangleShape),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp),
                ) {
                    PixelPirate(phase = Phase.IDLE, happy = true, lookingAtYou = true, size = 128.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "HI DAAD !",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tout l'équipage te salue bien bas !",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    MenuItem("À L'ABORDAGE !", onClick = onDismiss)
                }
            }
            SparkleBurst(trigger = 1, modifier = Modifier.matchParentSize(), count = 70, spread = 1.6f, forever = true)
        }
    }
}
