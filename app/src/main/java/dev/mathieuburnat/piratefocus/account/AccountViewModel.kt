package dev.mathieuburnat.piratefocus.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.mathieuburnat.piratefocus.journal.CrewMate
import dev.mathieuburnat.piratefocus.journal.LogEntry
import dev.mathieuburnat.piratefocus.player.PirateLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Le formulaire du compte gratuit : créer un compte, s'y connecter, ou retrouver un mot de passe oublié. */
enum class AuthMode { REGISTER, LOGIN, FORGOT }

data class AccountUiState(
    val account: Account = Account(),
    val authMode: AuthMode = AuthMode.REGISTER,
    /** Le code secret à montrer une seule fois (null : rien à montrer). */
    val recoveryCode: String? = null,
    /** Le code secret à montrer est celui d'un compte qui vient d'être créé (3e et dernière étape). */
    val freshAccount: Boolean = false,
    /** Le mot de passe vient d'être changé (petit mot de confirmation). */
    val passwordChanged: Boolean = false,
    val busy: Boolean = false,
    /** Le dernier souci à afficher (déjà en français pirate). */
    val error: String? = null,
    /** Un petit mot à montrer en toast (connexion, déconnexion), puis à oublier avec [AccountViewModel.toastShown]. */
    val toast: String? = null,
)

/**
 * Le compte du matelot : anonyme (tout reste sur le téléphone) ou compte gratuit (identifiant, mot de passe et code secret).
 * [store] null : rien n'est gardé (tests, aperçus).
 */
class AccountViewModel(
    private val store: AccountStore? = null,
    private val api: PirateApi = PirateApi(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState(account = store?.load() ?: Account()))
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private val account: Account get() = _uiState.value.account

    init {
        refresh()
    }

    /** Matelot anonyme : un pseudo (tiré au sort ou choisi), et c'est tout. */
    fun chooseAnonymous(name: String): Boolean {
        val clean = PirateNames.clean(name)
        if (clean == null) {
            _uiState.update { it.copy(error = "Un pseudo de ${PirateNames.MIN_LENGTH} à ${PirateNames.MAX_LENGTH} caractères, matelot !") }
            return false
        }
        save(account.copy(onboarded = true, pirateName = clean))
        return true
    }

    fun setAuthMode(mode: AuthMode) = _uiState.update { it.copy(authMode = mode, error = null) }

    /**
     * Nouveau compte gratuit : le journal et le coffre du téléphone partent ensuite chez le Worker (synchro).
     * Le code secret reste affiché tant que le matelot n'a pas confirmé l'avoir noté.
     */
    fun register(login: String, password: String, name: String) = launchBusy {
        signIn(api.register(login.trim(), password, PirateNames.clean(name)))
        _uiState.update { it.copy(freshAccount = true) }
    }

    fun login(login: String, password: String) = launchBusy { signIn(api.login(login.trim(), password)) }

    /**
     * Connexion (ou création) avec Google, à partir de l'ID token donné par le téléphone. À la création,
     * le pseudo du matelot local le suit.
     */
    fun google(idToken: String) = launchBusy { signIn(api.google(idToken, PirateNames.clean(account.pirateName))) }

    /** Lie un compte Google au pirate connecté : il pourra ensuite monter à bord par Google aussi. */
    fun linkGoogle(idToken: String) {
        val token = account.token ?: return
        launchBusy {
            saveMe(api.linkGoogle(token, idToken))
            _uiState.update { it.copy(toast = AccountQuotes.GOOGLE_LINKED) }
        }
    }

    fun unlinkGoogle() {
        val token = account.token ?: return
        launchBusy { saveMe(api.unlinkGoogle(token)) }
    }

    /** Un souci venu d'ailleurs que du Worker (la fenêtre Google, par exemple), à afficher. */
    fun showError(message: String) = _uiState.update { it.copy(error = message) }

    /** Mot de passe oublié : un nouveau code secret remplace celui qui vient de servir. */
    fun recover(login: String, code: String, newPassword: String) = launchBusy {
        signIn(api.recover(login.trim(), code, newPassword))
    }

    fun newRecoveryCode() {
        val token = account.token ?: return
        launchBusy {
            val code = api.newRecoveryCode(token)
            _uiState.update { it.copy(recoveryCode = code) }
        }
    }

    fun changePassword(current: String, new: String) {
        val token = account.token ?: return
        launchBusy {
            api.changePassword(token, current, new)
            _uiState.update { it.copy(passwordChanged = true) }
        }
    }

    fun toastShown() = _uiState.update { it.copy(toast = null) }

    /** Le matelot a noté son code secret : on ne le montrera plus jamais. */
    fun recoveryCodeNoted() = _uiState.update { it.copy(recoveryCode = null, freshAccount = false) }

    private fun signIn(session: Session) {
        _uiState.update {
            it.copy(recoveryCode = session.recoveryCode, authMode = AuthMode.LOGIN, toast = AccountQuotes.signedIn(session.me.name))
        }
        save(account.copy(onboarded = true, token = session.token))
        saveMe(session.me)
    }

    /** Ce que le Worker sait du pirate, rangé sur le téléphone. */
    private fun saveMe(me: Me) = save(
        account.copy(
            pirateName = me.name,
            public = me.public,
            email = me.email,
            login = me.login,
            kind = me.kind,
            google = me.google,
            head = me.head ?: account.head,
            heads = me.heads ?: account.heads,
        ),
    )

    /** Relit le compte chez le Worker (pseudo, visibilité) ; un jeton refusé déconnecte en douceur. */
    fun refresh() {
        val token = account.token ?: return
        viewModelScope.launch {
            try {
                saveMe(api.me(token))
            } catch (e: ApiException) {
                if (e.status == 401) signOutLocally()
            }
        }
    }

    fun rename(name: String) {
        val clean = PirateNames.clean(name)
        if (clean == null) {
            _uiState.update { it.copy(error = "Un pseudo de ${PirateNames.MIN_LENGTH} à ${PirateNames.MAX_LENGTH} caractères, matelot !") }
            return
        }
        val token = account.token
        if (token == null) {
            save(account.copy(pirateName = clean))
            _uiState.update { it.copy(toast = AccountQuotes.renamed(clean)) }
            return
        }
        launchBusy {
            val renamed = api.updateMe(token, name = clean).name
            save(account.copy(pirateName = renamed))
            _uiState.update { it.copy(toast = AccountQuotes.renamed(renamed)) }
        }
    }

    /**
     * Porter la tête [head] (#18). Le niveau vient des [doubloons] du coffre : niveau 5, 3 têtes au choix ; niveau 10, toutes.
     * Avec un compte, le Worker tranche (il connaît le coffre synchronisé) ; sans compte, le téléphone.
     */
    fun chooseHead(head: Int, doubloons: Int) {
        val unlocked = PirateLevel.choose(head, account.heads, PirateLevel.of(doubloons))
        if (unlocked == null) {
            _uiState.update { it.copy(error = AccountQuotes.headLocked(PirateLevel.of(doubloons))) }
            return
        }
        val token = account.token
        if (token == null) {
            save(account.copy(head = head, heads = unlocked))
            return
        }
        launchBusy { saveMe(api.chooseHead(token, head)) }
    }

    fun setPublic(public: Boolean) {
        val token = account.token ?: return
        launchBusy { save(account.copy(public = api.updateMe(token, public = public).public)) }
    }

    /** Déconnexion : le journal reste sur le téléphone, on redevient matelot anonyme. Puis [onSignedOut]. */
    fun logout(onSignedOut: () -> Unit = {}) {
        val token = account.token ?: return
        launchBusy {
            runCatching { api.logout(token) }
            signOutLocally()
            _uiState.update { it.copy(toast = AccountQuotes.signedOut(account.pirateName)) }
            onSignedOut()
        }
    }

    /**
     * Suppression du compte : l'identité disparaît chez le Worker, le pirate devient un « Ancien matelot ».
     * [onDeleted] n'est appelé qu'une fois la suppression réussie.
     */
    fun deleteAccount(onDeleted: () -> Unit = {}) {
        val token = account.token ?: return
        launchBusy {
            api.deleteMe(token)
            signOutLocally()
            _uiState.update { it.copy(toast = AccountQuotes.DELETED) }
            onDeleted()
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    /**
     * Factory reset : on oublie le compte sur le téléphone. Avec un compte gratuit, on se déconnecte
     * chez le Worker au passage (sans insister si le réseau boude) ; ses données y restent.
     */
    fun reset() {
        val token = account.token
        _uiState.value = AccountUiState()
        if (token != null) viewModelScope.launch { runCatching { api.logout(token) } }
    }

    /** Synchronise journal et coffre ; null sans compte ou en cas de souci (on réessaiera au prochain changement). */
    suspend fun sync(logs: List<LogEntry>, doubloons: Int, voyages: Int): SyncResult? {
        val token = account.token ?: return null
        return try {
            api.sync(token, logs, doubloons, voyages)
        } catch (e: ApiException) {
            if (e.status == 401) signOutLocally()
            null
        }
    }

    /**
     * Les vrais pirates publics, accessibles même sans compte. Le pirate de ce téléphone en est retiré :
     * il apparaît déjà sous « Toi ». Lance [ApiException] si le Worker est injoignable.
     */
    suspend fun crew(): List<CrewMate> {
        val mine = account.pirateName.takeIf { account.signedIn }
        return api.crew().filter { it.name != mine }
    }

    private fun signOutLocally() = save(account.copy(token = null, email = null, login = null, kind = null, google = false))

    private fun save(next: Account) {
        store?.save(next)
        _uiState.update { it.copy(account = next) }
    }

    private fun launchBusy(block: suspend () -> Unit) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true, error = null, passwordChanged = false) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: ApiException) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(busy = false) }
            }
        }
    }
}
