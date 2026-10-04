package dev.mathieuburnat.piratefocus.guard

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

/** Les autorisations spéciales dont le gardien a besoin pour surveiller le pont. */
object GuardPermissions {

    /** Voir quelle appli est au premier plan. */
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Surgir par-dessus une appli interdite (nécessaire pour ouvrir un écran depuis l'arrière-plan). */
    fun hasOverlay(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** Afficher la notification du gardien (Android 13+). */
    fun hasNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun isReady(context: Context): Boolean = hasUsageAccess(context) && hasOverlay(context)

    fun usageAccessIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun overlayIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
}
