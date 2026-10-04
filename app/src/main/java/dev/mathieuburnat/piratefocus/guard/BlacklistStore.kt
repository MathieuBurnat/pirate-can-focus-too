package dev.mathieuburnat.piratefocus.guard

import android.content.Context

/** Liste noire des applis interdites pendant une traversée, stockée localement. */
class BlacklistStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("blacklist", Context.MODE_PRIVATE)

    fun load(): Set<String> = prefs.getStringSet(KEY, null)?.toSet() ?: DEFAULTS

    fun save(packages: Set<String>) {
        prefs.edit().putStringSet(KEY, packages).apply()
    }

    fun toggle(packageName: String): Set<String> {
        val current = load()
        val next = if (packageName in current) current - packageName else current + packageName
        save(next)
        return next
    }

    companion object {
        private const val KEY = "packages"

        /** Les sirènes les plus dangereuses des sept mers. */
        val DEFAULTS = setOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically", // TikTok
            "com.ss.android.ugc.trill", // TikTok (certaines régions)
            "com.reddit.frontpage",
        )
    }
}
