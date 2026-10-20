package dev.mathieuburnat.piratefocus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Secondes à attendre avant de pouvoir confirmer. */
private const val CONFIRM_WAIT_S = 5

/**
 * Dernière chance avant une action sans retour (factory reset, liste noire vidée, compte supprimé...) :
 * [confirmLabel] ne répond qu'après [CONFIRM_WAIT_S] secondes. [onLinkAccount] : la porte de sortie
 * recommandée (lier un compte en ligne), proposée en premier quand elle est donnée.
 */
@Composable
fun ReallySureDialog(
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    confirmLabel: String = "CONFIRMER",
    onLinkAccount: (() -> Unit)? = null,
) {
    var left by remember { mutableIntStateOf(CONFIRM_WAIT_S) }
    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1_000)
            left--
        }
    }
    ChoiceDialog(
        title = "☠ ATTENTION ☠",
        onDismiss = onCancel,
        text = { TypewriterText(message, style = MaterialTheme.typography.bodyMedium) },
        choices = buildList {
            if (onLinkAccount != null) add(Choice(LINK_ACCOUNT_LABEL, ChoiceKind.ADVISED, onLinkAccount))
            add(Choice(confirmLabel, ChoiceKind.DANGER, onConfirm, waitLeft = left))
            add(Choice("non, annuler capitaine !", ChoiceKind.CANCEL, onCancel))
        },
    )
}

/** Le choix conseillé quand un matelot anonyme s'apprête à couler son butin (tient sur une ligne). */
const val LINK_ACCOUNT_LABEL = "LIER UN COMPTE"

/** Comment un choix de pop-up se présente. */
enum class ChoiceKind {
    /** La sortie conseillée : cadre et texte dorés. */
    ADVISED,

    /** Un choix ordinaire, encadré comme les menus. */
    NORMAL,

    /** L'action sans retour : en rouge (en pointillés blancs tant que [Choice.waitLeft] > 0). */
    DANGER,

    /** L'annulation : discrète, sans cadre. */
    CANCEL,
}

class Choice(val label: String, val kind: ChoiceKind, val onClick: () -> Unit, val waitLeft: Int = 0)

/**
 * Pop-up à choix (actions dangereuses en tête) : les choix sont numérotés et empilés comme les menus de l'appli
 * (`> 1. ...`), le conseillé d'abord, le destructeur en rouge, l'annulation discrète en dernier.
 */
@Composable
fun ChoiceDialog(title: String, onDismiss: () -> Unit, text: @Composable () -> Unit, choices: List<Choice>) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        title = { Text(title, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip) },
        text = text,
        confirmButton = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEachIndexed { index, choice -> ChoiceButton(index + 1, choice) }
            }
        },
    )
}

/**
 * Un choix numéroté. Le destructeur, tant qu'il faut patienter : bordure blanche en pointillés, texte grisé et
 * compte à rebours (aucune trace de rouge) ; puis, d'un coup, bordure rouge pleine, texte rouge vif et cliquable.
 */
@Composable
private fun ChoiceButton(number: Int, choice: Choice) {
    val colors = MaterialTheme.colorScheme
    val waiting = choice.waitLeft > 0
    val label = "> $number. ${choice.label}" + if (waiting) " (${choice.waitLeft})" else ""
    if (choice.kind == ChoiceKind.CANCEL) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.secondary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = choice.onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        return
    }
    val accent = when (choice.kind) {
        ChoiceKind.ADVISED -> colors.primary
        ChoiceKind.DANGER -> colors.error
        else -> colors.onSurface
    }
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (waiting) colors.onSurface.copy(alpha = 0.4f) else accent,
        modifier = Modifier
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawRect(
                    color = if (waiting) Color.White else accent,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(
                        width = stroke,
                        pathEffect = if (waiting) PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())) else null,
                    ),
                )
            }
            .fillMaxWidth()
            .clickable(enabled = !waiting, onClick = choice.onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}
