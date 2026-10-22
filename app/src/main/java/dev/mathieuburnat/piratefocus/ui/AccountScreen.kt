package dev.mathieuburnat.piratefocus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mathieuburnat.piratefocus.account.AccountQuotes
import dev.mathieuburnat.piratefocus.account.AccountViewModel
import dev.mathieuburnat.piratefocus.player.PirateHeads
import dev.mathieuburnat.piratefocus.player.PirateLevel

/**
 * « Ma fiche de matelot » (#18) : le joueur, pas le compte. Sa tête, son pseudo, son niveau et les têtes à choisir.
 * Tout ce qui touche au compte est derrière « INFORMATIONS DE CONNEXION ».
 */
@Composable
fun AccountScreen(viewModel: AccountViewModel, doubloons: Int, devMode: Boolean, onBack: () -> Unit, onConnection: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val account = state.account
    var name by rememberSaveable(account.pirateName) { mutableStateOf(account.pirateName) }
    // La tête que le matelot hésite à débloquer (null : pas de question).
    var unlocking by remember { mutableStateOf<Int?>(null) }
    var choosingHead by rememberSaveable { mutableStateOf(false) }
    val level = PirateLevel.of(doubloons)
    val freeSlots = PirateLevel.freeSlots(account.heads, level)

    // Le Worker a peut-être changé d'avis (pseudo, tête) depuis un autre téléphone.
    LaunchedEffect(Unit) { viewModel.refresh() }

    if (choosingHead) {
        HeadMenu(
            current = account.head,
            unlocked = account.heads,
            level = level,
            state = state,
            devMode = devMode,
            onChoose = { head ->
                when {
                    head == account.head -> Unit
                    PirateLevel.canWear(head, account.heads, level) -> viewModel.chooseHead(head, doubloons)
                    devMode -> viewModel.tryHead(head) // essayage du mode dev : rien n'est débloqué
                    freeSlots > 0 -> unlocking = head
                    else -> viewModel.chooseHead(head, doubloons) // le capitaine dit pourquoi c'est non
                }
            },
            onDismiss = {
                choosingHead = false
                viewModel.clearError()
            },
        )
    }

    unlocking?.let { head ->
        ChoiceDialog(
            title = "⚓ NOUVELLE TÊTE ? ⚓",
            onDismiss = { unlocking = null },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    PixelHead(head, size = 96.dp)
                    Text(
                        "Tu peux encore choisir $freeSlots tête${if (freeSlots > 1) "s" else ""}. " +
                            "Celle-ci restera à toi jusqu'au niveau ${PirateLevel.ALL_HEADS_LEVEL}, où toutes se débloquent.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            choices = listOf(
                Choice("PRENDRE CETTE TÊTE", ChoiceKind.ADVISED, {
                    unlocking = null
                    viewModel.chooseHead(head, doubloons)
                }),
                Choice("réfléchir encore", ChoiceKind.CANCEL, { unlocking = null }),
            ),
        )
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                "< MENU",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable(onClick = onBack).padding(vertical = 4.dp),
            )
            ScreenTitle("FICHE DE MATELOT")
            Spacer(Modifier.height(8.dp))

            // Le pirate en grand, son pseudo et son niveau.
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // Toucher la tête ouvre le menu des têtes (garçon, fille, ou neutre).
                PixelHead(account.head, size = 128.dp, modifier = Modifier.clickable { choosingHead = true })
                Text("~ touche ta tête pour en changer ~", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(account.pirateName, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text("Niveau $level", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(levelBar(doubloons), style = MaterialTheme.typography.bodySmall)
                Text(
                    "${doubloons - PirateLevel.doubloonsFor(level)} / ${PirateLevel.doubloonsFor(level + 1) - PirateLevel.doubloonsFor(level)} doublons " +
                        "avant le niveau ${level + 1}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(16.dp))

            SectionTitle("Identité")
            PirateField(name, { name = it }, "> ton pseudo de pirate")
            if (name.trim() != account.pirateName) MenuItem("RENOMMER", onClick = { viewModel.rename(name) }, dimmed = state.busy)
            Spacer(Modifier.height(16.dp))

            BusyAndError(state)

            Spacer(Modifier.height(24.dp))
            MenuItem(if (account.signedIn) "INFORMATIONS DE CONNEXION" else "CRÉER UN COMPTE GRATUIT", onClick = onConnection)
            if (!account.signedIn) Text(AccountQuotes.NO_ACCOUNT_INFO, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** La barre de niveau, façon terminal : « [######....] ». */
private fun levelBar(doubloons: Int, width: Int = 16): String {
    val filled = (PirateLevel.progress(doubloons) * width).toInt().coerceIn(0, width)
    return "[" + "#".repeat(filled) + ".".repeat(width - filled) + "]"
}

/** Titre d'une page de la fiche ; sur un petit écran, les « === » sautent plutôt que de passer seuls à la ligne. */
@Composable
internal fun ScreenTitle(title: String) {
    var narrow by remember { mutableStateOf(false) }
    Text(
        if (narrow) title else "=== $title ===",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        onTextLayout = { if (it.hasVisualOverflow) narrow = true },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Titre d'une section de la fiche, façon terminal : « -- Identité -- ». */
@Composable
internal fun SectionTitle(title: String, color: Color = MaterialTheme.colorScheme.onBackground) {
    Text("-- $title --", style = MaterialTheme.typography.titleSmall, color = color, modifier = Modifier.padding(bottom = 4.dp))
}
