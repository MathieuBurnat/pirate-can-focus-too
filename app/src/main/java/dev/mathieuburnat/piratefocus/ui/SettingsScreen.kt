package dev.mathieuburnat.piratefocus.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.mathieuburnat.piratefocus.guard.BlacklistStore
import dev.mathieuburnat.piratefocus.guard.GuardPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class AppEntry(val packageName: String, val label: String, val installed: Boolean)

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { BlacklistStore(context) }
    var blacklist by remember { mutableStateOf(store.load()) }
    var installedApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }

    var usageOk by remember { mutableStateOf(false) }
    var overlayOk by remember { mutableStateOf(false) }
    var notifOk by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        usageOk = GuardPermissions.hasUsageAccess(context)
        overlayOk = GuardPermissions.hasOverlay(context)
        notifOk = GuardPermissions.hasNotifications(context)
        onPauseOrDispose { }
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifOk = it }

    LaunchedEffect(Unit) {
        installedApps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(launcher, 0)
                .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
                .filter { (pkg, _) -> pkg != context.packageName }
                .distinctBy { it.first }
                .map { (pkg, label) -> AppEntry(pkg, label, installed = true) }
        }
    }

    // Les applis de la liste noire d'abord, puis le reste par ordre alphabétique.
    val installedPackages = installedApps.map { it.packageName }.toSet()
    val missing = blacklist.filter { it !in installedPackages }.map { AppEntry(it, it, installed = false) }
    val entries = (installedApps + missing)
        .sortedWith(compareByDescending<AppEntry> { it.packageName in blacklist }.thenBy { it.label.lowercase() })

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                "< MENU",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable(onClick = onBack).padding(vertical = 4.dp),
            )
            Text("=== PARAMÈTRES ===", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))

            Text("-- Le gardien du navire --", style = MaterialTheme.typography.titleSmall)
            Text(
                "Pour surprendre les sirènes, le capitaine a besoin de ces autorisations :",
                style = MaterialTheme.typography.bodySmall,
            )
            PermissionRow("Voir l'appli ouverte", usageOk) { context.startActivity(GuardPermissions.usageAccessIntent()) }
            PermissionRow("Surgir par-dessus", overlayOk) { context.startActivity(GuardPermissions.overlayIntent(context)) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionRow("Notification de veille", notifOk) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("-- Liste noire (${blacklist.size}) --", style = MaterialTheme.typography.titleSmall)
            Text("Coche les applis interdites pendant une traversée.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))

            LazyColumn(Modifier.fillMaxWidth()) {
                items(entries, key = { it.packageName }) { app ->
                    val banned = app.packageName in blacklist
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { blacklist = store.toggle(app.packageName) }
                            .padding(vertical = 8.dp),
                    ) {
                        Text(
                            if (banned) "[x] " else "[ ] ",
                            color = if (banned) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            if (app.installed) app.label else "${app.label} (non installée)",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onRequest: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !granted, onClick = onRequest)
            .padding(vertical = 6.dp),
    ) {
        Text(
            if (granted) "[OK] " else "[!!] ",
            color = if (granted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            if (granted) label else "$label  > autoriser",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
