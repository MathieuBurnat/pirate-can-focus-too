package dev.mathieuburnat.piratefocus.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.mathieuburnat.piratefocus.account.AccountQuotes
import dev.mathieuburnat.piratefocus.account.AccountUiState
import dev.mathieuburnat.piratefocus.account.AuthMode
import dev.mathieuburnat.piratefocus.account.PirateNames
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Mot de passe : 8 caractères au moins (le Worker vérifie aussi). */
private const val PASSWORD_MIN = 8

/**
 * Le compte gratuit : créer un compte (email comme identifiant, mot de passe, pseudo), s'y connecter,
 * ou retrouver un mot de passe oublié avec le code secret.
 */
@Composable
fun FreeAccountForm(
    state: AccountUiState,
    onMode: (AuthMode) -> Unit,
    onRegister: (login: String, password: String, name: String) -> Unit,
    onLogin: (login: String, password: String) -> Unit,
    onRecover: (login: String, code: String, newPassword: String) -> Unit,
) {
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf(state.account.pirateName.ifEmpty { PirateNames.random() }) }
    val loginOk = EMAIL.matches(login.trim())
    val passwordOk = password.length >= PASSWORD_MIN

    Column(Modifier.fillMaxWidth()) {
        TypewriterText(AccountQuotes.FREE_ACCOUNT, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        // Deux onglets de même hauteur : créer un compte, ou monter à bord d'un compte existant.
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AuthTab("CRÉER MON COMPTE", selected = state.authMode == AuthMode.REGISTER, onClick = { onMode(AuthMode.REGISTER) }, modifier = Modifier.weight(1f))
            AuthTab("J'AI DÉJÀ UN COMPTE", selected = state.authMode != AuthMode.REGISTER, onClick = { onMode(AuthMode.LOGIN) }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        // L'email sert d'identifiant : jamais vérifié, aucun mail envoyé.
        PirateField(login, { login = it.filter { c -> !c.isWhitespace() }.take(254) }, "ton-adresse@mail.com", KeyboardType.Email, autoCorrect = false)
        Text(AccountQuotes.LOGIN_EXPLAINED, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        when (state.authMode) {
            AuthMode.REGISTER -> {
                PasswordField(password, { password = it }, "> mot de passe ($PASSWORD_MIN caractères min.)")
                Text("Ton pseudo de pirate (celui que l'équipage voit) :", style = MaterialTheme.typography.bodySmall)
                PirateField(name, { name = it }, "> ton pseudo de pirate")
                DiceReroll(onRoll = { name = PirateNames.random() })
                MenuItem(
                    "CRÉER MON COMPTE GRATUIT",
                    onClick = { if (loginOk && passwordOk) onRegister(login, password, name) },
                    dimmed = state.busy || !loginOk || !passwordOk,
                )
            }
            AuthMode.LOGIN -> {
                PasswordField(password, { password = it }, "> mot de passe")
                MenuItem("MONTER À BORD", onClick = { if (loginOk && password.isNotEmpty()) onLogin(login, password) }, dimmed = state.busy || !loginOk || password.isEmpty())
                Text(
                    "mot de passe oublié ?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.clickable { onMode(AuthMode.FORGOT) }.padding(vertical = 6.dp),
                )
            }
            AuthMode.FORGOT -> {
                Text(AccountQuotes.FORGOT_EXPLAINED, style = MaterialTheme.typography.bodySmall)
                PirateField(code, { code = it.uppercase().take(30) }, "> code secret (XXXX-XXXX-...)")
                PasswordField(password, { password = it }, "> nouveau mot de passe ($PASSWORD_MIN min.)")
                MenuItem(
                    "RETROUVER MON COMPTE",
                    onClick = { if (loginOk && code.isNotBlank() && passwordOk) onRecover(login, code, password) },
                    dimmed = state.busy || !loginOk || code.isBlank() || !passwordOk,
                )
                Text(
                    "< j'ai retrouvé mon mot de passe",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.clickable { onMode(AuthMode.LOGIN) }.padding(vertical = 6.dp),
                )
            }
        }
        BusyAndError(state)
    }
}

/** Une adresse email plausible (une seule @, un domaine avec un point, pas d'espace). */
private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

@Composable
private fun AuthTab(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .border(BorderStroke(2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)), RectangleShape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
    }
}

/** Un mot de passe masqué, avec « voir » / « cacher ». */
@Composable
fun PasswordField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RectangleShape,
        textStyle = MaterialTheme.typography.bodyLarge,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            Text(
                if (visible) "cacher" else "voir",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable { visible = !visible }.padding(horizontal = 8.dp),
            )
        },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

/** Le code secret, montré une seule fois : à noter (ou copier) avant de continuer. */
@Composable
fun RecoveryCodeDialog(code: String, onNoted: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Surface(
            shape = RectangleShape,
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RectangleShape),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                Text("☠ TON CODE SECRET ☠", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                Spacer(Modifier.height(12.dp))
                Text(
                    code,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground), RectangleShape)
                        .padding(12.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(AccountQuotes.RECOVERY_WARNING, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                MenuItem(
                    if (copied) "COPIÉ !" else "COPIER",
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    },
                )
                MenuItem("J'AI NOTÉ MON CODE", onClick = onNoted)
            }
        }
    }
}

@Composable
fun BusyAndError(state: AccountUiState) {
    if (state.busy) Text(AccountQuotes.FLYING, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    state.error?.let { Text("[!!] $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
}

@Composable
fun PirateField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboard: KeyboardType = KeyboardType.Text,
    autoCorrect: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RectangleShape,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, autoCorrectEnabled = autoCorrect),
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

/** Le dé des pseudos, centré : on le lance, il saute, tourne, change de face et propose un autre pseudo en retombant. */
@Composable
fun DiceReroll(onRoll: () -> Unit) {
    var face by rememberSaveable { mutableIntStateOf(5) }
    var throws by remember { mutableIntStateOf(0) }
    val spin = remember { Animatable(0f) }
    val flight = remember { Animatable(0f) }
    val latestOnRoll by rememberUpdatedState(onRoll)
    LaunchedEffect(throws) {
        if (throws == 0) return@LaunchedEffect
        launch {
            spin.snapTo(0f)
            spin.animateTo(720f, tween(DICE_ROLL_MS, easing = FastOutSlowInEasing))
        }
        launch {
            flight.snapTo(0f)
            flight.animateTo(1f, tween(DICE_ROLL_MS, easing = LinearEasing))
        }
        // Le dé roule : il montre plusieurs faces avant de se poser sur la dernière.
        repeat(DICE_ROLL_MS / DICE_FACE_MS) {
            face = ((1..6) - face).random()
            delay(DICE_FACE_MS.toLong())
        }
        latestOnRoll()
    }
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { throws++ }
            .padding(vertical = 8.dp),
    ) {
        PixelDice(
            face,
            modifier = Modifier.graphicsLayer {
                rotationZ = spin.value
                // Un petit saut en cloche : monte puis retombe.
                translationY = -sin(flight.value * PI.toFloat()) * 14.dp.toPx()
            },
        )
        Spacer(Modifier.width(10.dp))
        Text("un autre pseudo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
    }
}

private const val DICE_ROLL_MS = 600
private const val DICE_FACE_MS = 75
