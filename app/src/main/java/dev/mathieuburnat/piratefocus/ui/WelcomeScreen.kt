package dev.mathieuburnat.piratefocus.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mathieuburnat.piratefocus.account.AccountQuotes
import dev.mathieuburnat.piratefocus.account.AccountViewModel
import dev.mathieuburnat.piratefocus.account.PirateNames
import dev.mathieuburnat.piratefocus.focus.Phase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class WelcomeStep { CHOICE, ANONYMOUS, FREE_ACCOUNT }

/** Premier lancement : matelot anonyme ou compte gratuit. Aucun compte n'est obligatoire. */
@Composable
fun WelcomeScreen(viewModel: AccountViewModel, onDone: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableStateOf(WelcomeStep.CHOICE) }
    var name by rememberSaveable { mutableStateOf(PirateNames.random()) }

    // Le capitaine lève les yeux vers toi, puis sourit (une seule fois, à l'arrivée).
    var lookingAtYou by rememberSaveable { mutableStateOf(false) }
    var smiling by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (smiling) return@LaunchedEffect
        delay(700)
        lookingAtYou = true
        delay(600)
        smiling = true
    }

    // Puis il salue : il se dandine d'un pied sur l'autre en sautillant, à chaque retour sur le choix.
    val sway = remember { Animatable(0f) }
    val hop = remember { Animatable(0f) }
    LaunchedEffect(smiling, step) {
        if (!smiling || step != WelcomeStep.CHOICE) {
            sway.animateTo(0f)
            return@LaunchedEffect
        }
        launch {
            repeat(2) {
                hop.animateTo(1f, tween(140, easing = FastOutSlowInEasing))
                hop.animateTo(0f, tween(160))
            }
        }
        for (angle in listOf(-10f, 10f, -8f, 8f, -4f, 0f)) sway.animateTo(angle, tween(150))
        // Ensuite, un léger balancement tant qu'on choisit.
        while (true) {
            sway.animateTo(3f, tween(1100))
            sway.animateTo(-3f, tween(1100))
        }
    }

    // Le code est validé : le compte gratuit est prêt, on lève l'ancre.
    LaunchedEffect(state.account.signedIn) {
        if (state.account.signedIn) onDone()
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (step != WelcomeStep.CHOICE) {
                Text(
                    "< RETOUR",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .clickable {
                            viewModel.clearError()
                            step = WelcomeStep.CHOICE
                        }
                        .padding(vertical = 4.dp),
                )
            }
            Text("=== BIENVENUE À BORD ===", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            PixelPirate(
                phase = Phase.IDLE,
                happy = smiling,
                lookingAtYou = lookingAtYou,
                modifier = Modifier.graphicsLayer {
                    // Il pivote depuis ses bottes, comme un vrai loup de mer.
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    rotationZ = sway.value
                    translationY = -hop.value * 10.dp.toPx()
                },
            )
            Spacer(Modifier.height(16.dp))

            when (step) {
                WelcomeStep.CHOICE -> {
                    TypewriterText(
                        AccountQuotes.WELCOME,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    MenuItem("1. MATELOT ANONYME", onClick = { step = WelcomeStep.ANONYMOUS })
                    MenuItem("2. COMPTE GRATUIT", onClick = { step = WelcomeStep.FREE_ACCOUNT })
                }
                WelcomeStep.ANONYMOUS -> {
                    TypewriterText(
                        AccountQuotes.ANONYMOUS,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    PirateField(name, { name = it }, "> ton pseudo de pirate")
                    DiceReroll(onRoll = { name = PirateNames.random() })
                    NoAccountInfo()
                    AnchorOrSignIn(
                        onAnchor = { if (viewModel.chooseAnonymous(name)) onDone() },
                        onSignIn = { step = WelcomeStep.FREE_ACCOUNT },
                    )
                    BusyAndError(state)
                }
                WelcomeStep.FREE_ACCOUNT -> FreeAccountForm(
                    state = state,
                    onStartEmail = viewModel::startEmail,
                    onVerify = viewModel::verifyCode,
                    onChangeEmail = viewModel::changeEmail,
                )
            }
        }
    }
}

/**
 * « LEVER L'ANCRE » (5 s) puis « SE CONNECTER » (3 s), en boucle : le bouton se trémousse à chaque
 * changement, et fait ce qu'il affiche au moment du tap.
 */
@Composable
private fun AnchorOrSignIn(onAnchor: () -> Unit, onSignIn: () -> Unit) {
    var signIn by remember { mutableStateOf(false) }
    val wiggle = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(if (signIn) 3_000 else 5_000)
            signIn = !signIn
            for (angle in listOf(-4f, 4f, -3f, 3f, -1f, 0f)) wiggle.animateTo(angle, tween(60))
        }
    }
    MenuItem(
        if (signIn) "SE CONNECTER" else "LEVER L'ANCRE",
        onClick = { if (signIn) onSignIn() else onAnchor() },
        modifier = Modifier.graphicsLayer { rotationZ = wiggle.value },
    )
}
