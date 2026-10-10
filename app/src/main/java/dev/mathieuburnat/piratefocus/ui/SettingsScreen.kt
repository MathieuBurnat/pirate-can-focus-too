package dev.mathieuburnat.piratefocus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Les paramètres : applications bloquées, mode dev et déconnexion. */
@Composable
fun SettingsScreen(
    devMode: Boolean,
    signedIn: Boolean,
    onBack: () -> Unit,
    onBlockedApps: () -> Unit,
    onDevModeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                "< MENU",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable(onClick = onBack).padding(vertical = 4.dp),
            )
            Text("=== PARAMÈTRES ===", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))

            MenuItem("1. APPLICATIONS BLOQUÉES", onClick = onBlockedApps)
            MenuItem("2. MODE DEV  ${if (devMode) "[x]" else "[ ]"}", onClick = { onDevModeChange(!devMode) })
            Text(
                if (devMode) {
                    "Les statistiques de l'équipage montrent des matelots inventés, codés en dur dans l'app."
                } else {
                    "Les statistiques de l'équipage montrent les vrais pirates publics. Aucune fausse donnée en base."
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (signedIn) MenuItem("3. SE DÉCONNECTER", onClick = onLogout)
        }
    }
}
