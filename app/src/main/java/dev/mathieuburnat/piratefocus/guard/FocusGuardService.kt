package dev.mathieuburnat.piratefocus.guard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import dev.mathieuburnat.piratefocus.MainActivity
import dev.mathieuburnat.piratefocus.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Le gardien du navire : pendant une traversée, il regarde quelle appli est au premier plan
 * et fait surgir le capitaine si c'est une appli de la liste noire.
 */
class FocusGuardService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastForeground: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        scope.launch { watch() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun watch() {
        val usage = getSystemService(UsageStatsManager::class.java)
        val blacklist = BlacklistStore(this)
        while (scope.isActive) {
            val foreground = currentForeground(usage)
            if (foreground != null && foreground != packageName && foreground in blacklist.load()) {
                startActivity(CaughtActivity.intent(this, foreground))
                // Laisse le temps à l'écran du capitaine de prendre le premier plan.
                lastForeground = packageName
                delay(1_500)
            }
            delay(POLL_MS)
        }
    }

    /** Dernière appli passée au premier plan, d'après les événements récents. */
    private fun currentForeground(usage: UsageStatsManager): String? {
        val now = System.currentTimeMillis()
        val events = usage.queryEvents(now - 10_000, now)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) lastForeground = event.packageName
        }
        return lastForeground
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Gardien du navire", NotificationManager.IMPORTANCE_LOW),
        )
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_skull)
            .setContentTitle("Traversée en cours")
            .setContentText("Le capitaine surveille le pont. Pas de sirènes !")
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "guard"
        private const val NOTIFICATION_ID = 1
        private const val POLL_MS = 1_000L

        fun start(context: Context) {
            context.startForegroundService(Intent(context, FocusGuardService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FocusGuardService::class.java))
        }
    }
}
