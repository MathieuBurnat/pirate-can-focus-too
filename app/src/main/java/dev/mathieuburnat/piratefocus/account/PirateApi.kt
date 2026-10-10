package dev.mathieuburnat.piratefocus.account

import dev.mathieuburnat.piratefocus.journal.CrewMate
import dev.mathieuburnat.piratefocus.journal.Entry
import dev.mathieuburnat.piratefocus.journal.LogEntry
import dev.mathieuburnat.piratefocus.journal.ME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.util.UUID

/** Une réponse d'erreur du Worker (son message est déjà en français pirate), ou un réseau absent (status 0). */
class ApiException(val status: Int, message: String) : Exception(message)

/** Ce que le Worker sait du pirate connecté. */
data class Me(val name: String, val public: Boolean, val kind: String, val email: String?)

data class Session(val token: String, val created: Boolean, val me: Me)

data class SyncResult(val logs: List<LogEntry>, val doubloons: Int, val voyages: Int)

/** Le client de l'API du capitaine (backend/), sans dépendance : HttpURLConnection et org.json. */
class PirateApi(private val baseUrl: String = BASE_URL) {

    suspend fun startEmail(email: String) {
        call("POST", "/auth/email/start", body = JSONObject().put("email", email))
    }

    /** [name] ne sert qu'à la création du pirate (premier passage). */
    suspend fun verifyEmail(email: String, code: String, name: String?): Session {
        val body = JSONObject().put("email", email).put("code", code)
        if (name != null) body.put("name", name)
        val json = call("POST", "/auth/email/verify", body = body)
        return Session(json.getString("token"), json.optBoolean("created"), me(json.getJSONObject("me")))
    }

    suspend fun me(token: String): Me = me(call("GET", "/me", token))

    /** POST plutôt que PATCH : HttpURLConnection ne connaît pas PATCH. */
    suspend fun updateMe(token: String, name: String? = null, public: Boolean? = null): Me {
        val body = JSONObject()
        if (name != null) body.put("name", name)
        if (public != null) body.put("public", public)
        return me(call("POST", "/me", token, body))
    }

    suspend fun deleteMe(token: String) {
        call("DELETE", "/me", token)
    }

    suspend fun logout(token: String) {
        call("POST", "/auth/logout", token, JSONObject())
    }

    /** Envoie le journal (par paquets, le Worker en accepte 500 au plus) et le coffre ; renvoie tout ce que le Worker connaît. */
    suspend fun sync(token: String, logs: List<LogEntry>, doubloons: Int, voyages: Int): SyncResult {
        val chest = JSONObject().put("doubloons", doubloons).put("voyages", voyages)
        var json = JSONObject()
        val batches = logs.chunked(SYNC_BATCH).ifEmpty { listOf(emptyList()) }
        for (batch in batches) {
            val lines = JSONArray()
            batch.forEach { log ->
                lines.put(
                    JSONObject()
                        .put("id", log.id)
                        .put("entry", log.entry.name)
                        .put("at", log.at.toString())
                        .put("note", log.note ?: JSONObject.NULL),
                )
            }
            json = call("POST", "/sync", token, JSONObject().put("logs", lines).put("chest", chest))
        }
        val remote = json.getJSONArray("logs")
        val received = (0 until remote.length()).mapNotNull { i -> logEntry(remote.getJSONObject(i)) }
        val coffre = json.getJSONObject("chest")
        return SyncResult(received, coffre.getInt("doubloons"), coffre.getInt("voyages"))
    }

    /** Les pirates publics et leur journal du dernier mois (sans compte, sans jeton). */
    suspend fun crew(): List<CrewMate> {
        val mates = call("GET", "/crew").getJSONArray("crew")
        return (0 until mates.length()).map { i ->
            val mate = mates.getJSONObject(i)
            val name = mate.getString("name")
            val logs = mate.getJSONArray("logs")
            val entries = (0 until logs.length()).mapNotNull { j -> logEntry(logs.getJSONObject(j), who = name) }
            CrewMate(name, entries.sortedByDescending { it.at })
        }
    }

    private fun me(json: JSONObject): Me {
        val account = json.optJSONObject("account")
        return Me(
            name = json.getString("name"),
            public = json.getBoolean("public"),
            kind = account?.optString("kind") ?: "email",
            email = account?.takeUnless { it.isNull("email") }?.optString("email"),
        )
    }

    /** Une ligne illisible (entrée inconnue de cette version de l'app...) est ignorée. */
    private fun logEntry(json: JSONObject, who: String = ME): LogEntry? = runCatching {
        LogEntry(
            who = who,
            entry = Entry.valueOf(json.getString("entry")),
            at = LocalDateTime.parse(json.getString("at")),
            note = json.takeUnless { it.isNull("note") }?.getString("note"),
            id = json.optString("id").ifEmpty { UUID.randomUUID().toString() },
        )
    }.getOrNull()

    private suspend fun call(method: String, path: String, token: String? = null, body: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val connection = try {
                URL(baseUrl + path).openConnection() as HttpURLConnection
            } catch (e: IOException) {
                throw ApiException(0, NO_WIND)
            }
            try {
                connection.requestMethod = method
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.setRequestProperty("Accept", "application/json")
                token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.outputStream.use { it.write(body.toString().encodeToByteArray()) }
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(text) }.getOrDefault(JSONObject())
                if (status !in 200..299) throw ApiException(status, json.optString("error").ifEmpty { "Le navire a pris l'eau ($status)" })
                json
            } catch (e: IOException) {
                throw ApiException(0, NO_WIND)
            } finally {
                connection.disconnect()
            }
        }

    companion object {
        const val BASE_URL = "https://yaawwwk-pirate-can-focus-too.helveticademia.ch"
        private const val TIMEOUT_MS = 15_000
        private const val SYNC_BATCH = 400
        private const val NO_WIND = "Pas un souffle de vent : impossible de joindre le navire (réseau ?)"
    }
}
