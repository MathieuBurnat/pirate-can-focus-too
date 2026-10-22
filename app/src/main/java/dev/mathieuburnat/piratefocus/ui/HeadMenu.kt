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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import dev.mathieuburnat.piratefocus.account.AccountQuotes
import dev.mathieuburnat.piratefocus.account.AccountUiState
import dev.mathieuburnat.piratefocus.player.PirateHeads
import dev.mathieuburnat.piratefocus.player.PirateLevel

/** Têtes par rangée dans le menu. */
private const val HEADS_PER_ROW = 5

/**
 * Le menu des têtes, ouvert en touchant la tête du pirate sur sa fiche : pirate garçon, pirate fille,
 * ou pirate neutre (plus androgyne), puis les 50 têtes de cette sorte. En mode dev, toutes les têtes s'essaient.
 */
@Composable
fun HeadMenu(
    current: Int,
    unlocked: List<Int>,
    level: Int,
    state: AccountUiState,
    devMode: Boolean,
    onChoose: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    // On ouvre sur la sorte de la tête portée.
    var kind by rememberSaveable { mutableStateOf(PirateHeads[current].kind) }
    val freeSlots = PirateLevel.freeSlots(unlocked, level)

    BackHandler(onBack = onDismiss)
    run {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    "< MA FICHE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.clickable(onClick = onDismiss).padding(vertical = 4.dp),
                )
                ScreenTitle("MA TÊTE")
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PixelHead(current, size = 112.dp) }
                Text(PirateHeads[current].label, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))

                SectionTitle("Mon pirate")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    PirateHeads.Kind.entries.forEach { option ->
                        KindTab(option.label, selected = option == kind, onClick = { kind = option }, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))

                SectionTitle("Têtes")
                Text(AccountQuotes.headsExplained(level, freeSlots), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                if (devMode) {
                    Text(
                        "[ mode dev ] Toutes les têtes s'essaient, sans rien débloquer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                PirateHeads.ofKind(kind).chunked(HEADS_PER_ROW).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                        row.forEach { head ->
                            val wearable = PirateLevel.canWear(head.id, unlocked, level)
                            HeadCell(
                                head = head.id,
                                worn = head.id == current,
                                wearable = wearable,
                                unlockable = !wearable && (freeSlots > 0 || devMode),
                                modifier = Modifier.weight(1f),
                                onClick = { onChoose(head.id) },
                            )
                        }
                        // Dernière rangée incomplète : les têtes gardent la même taille.
                        repeat(HEADS_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                BusyAndError(state)
            }
        }
    }
}

/** « [ FILLE ] » : l'onglet choisi est encadré en doré. */
@Composable
private fun KindTab(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier
            .border(BorderStroke(if (selected) 2.dp else 1.dp, color), RectangleShape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
    )
}

/** Une tête du menu : portée (cadre doré), permise (cadre fin), à débloquer, ou encore verrouillée (pâle). */
@Composable
private fun HeadCell(head: Int, worn: Boolean, wearable: Boolean, unlockable: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val border = when {
        worn -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        wearable -> BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
        unlockable -> BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
        else -> BorderStroke(1.dp, Color.Transparent)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .border(border, RectangleShape)
            .clickable(onClick = onClick)
            .alpha(if (wearable || unlockable) 1f else 0.3f),
    ) {
        PixelHead(head, size = 56.dp)
    }
}
