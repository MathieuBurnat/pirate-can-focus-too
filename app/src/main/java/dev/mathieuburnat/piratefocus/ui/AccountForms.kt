package dev.mathieuburnat.piratefocus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.mathieuburnat.piratefocus.account.AccountQuotes
import dev.mathieuburnat.piratefocus.account.AccountUiState
import dev.mathieuburnat.piratefocus.account.EmailStep
import dev.mathieuburnat.piratefocus.account.PirateNames

/** Le compte gratuit : l'adresse email, puis le code reçu (et le pseudo, au premier passage). */
@Composable
fun FreeAccountForm(
    state: AccountUiState,
    onStartEmail: (String) -> Unit,
    onVerify: (code: String, name: String) -> Unit,
    onChangeEmail: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf(state.email) }
    var code by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf(state.account.pirateName.ifEmpty { PirateNames.random() }) }

    Column(Modifier.fillMaxWidth()) {
        TypewriterText(AccountQuotes.FREE_ACCOUNT, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        when (state.emailStep) {
            EmailStep.ADDRESS -> {
                PirateField(email, { email = it }, "ton-adresse@mail.com", KeyboardType.Email)
                MenuItem("RECEVOIR MON CODE", onClick = { if (email.isNotBlank()) onStartEmail(email) }, dimmed = state.busy || email.isBlank())
                MenuItem("AVEC GOOGLE  (bientôt)", onClick = {}, dimmed = true)
            }
            EmailStep.CODE -> {
                Text("${AccountQuotes.CODE_SENT} ${state.email}. Entre le code à 6 chiffres :", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                PirateField(code, { code = it.filter(Char::isDigit).take(6) }, "> 000000", KeyboardType.NumberPassword)
                Text("Ton pseudo (si c'est ton premier passage) :", style = MaterialTheme.typography.bodySmall)
                PirateField(name, { name = it }, "> ton pseudo de pirate")
                DiceReroll(onRoll = { name = PirateNames.random() })
                MenuItem("MONTER À BORD", onClick = { if (code.length == 6) onVerify(code, name) }, dimmed = state.busy || code.length != 6)
                Text(
                    "< changer d'adresse",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.clickable(onClick = onChangeEmail).padding(vertical = 6.dp),
                )
            }
        }
        BusyAndError(state)
    }
}

@Composable
fun BusyAndError(state: AccountUiState) {
    if (state.busy) Text(AccountQuotes.FLYING, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    state.error?.let { Text("[!!] $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
}

@Composable
fun PirateField(value: String, onValueChange: (String) -> Unit, placeholder: String, keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RectangleShape,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

/** Le petit mot des matelots sans compte : où vit leur butin. */
@Composable
fun NoAccountInfo(modifier: Modifier = Modifier) {
    Text(
        "[i] ${AccountQuotes.NO_ACCOUNT_INFO}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier.padding(vertical = 8.dp),
    )
}

/** Le dé des pseudos, centré : on le lance, il change de face et propose un autre pseudo. */
@Composable
fun DiceReroll(onRoll: () -> Unit) {
    var face by rememberSaveable { mutableIntStateOf(5) }
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                face = ((1..6) - face).random()
                onRoll()
            }
            .padding(vertical = 8.dp),
    ) {
        PixelDice(face)
        Spacer(Modifier.width(10.dp))
        Text("un autre pseudo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
    }
}
