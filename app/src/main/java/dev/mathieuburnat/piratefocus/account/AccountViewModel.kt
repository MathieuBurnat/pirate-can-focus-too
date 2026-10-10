package dev.mathieuburnat.piratefocus.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.mathieuburnat.piratefocus.journal.CrewMate
import dev.mathieuburnat.piratefocus.journal.LogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Les deux temps de la connexion par email : l'adresse, puis le code reçu. */
enum class EmailStep { ADDRESS, CODE }

data class AccountUiState(
    val account: Account = Account(),
    val emailStep: EmailStep = EmailStep.ADDRESS,
    /** L'adresse à laquelle le code est parti. */
    val email: String = "",
    val busy: Boolean = false,
    /** Le dernier souci à afficher (déjà en français pirate). */
    val error: String? = null,
)

/**
 * Le compte du matelot : anonyme (tout reste sur le téléphone) ou compte gratuit (code par email).
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

    fun startEmail(email: String) = launchBusy {
        val address = email.trim().lowercase()
        api.startEmail(address)
        _uiState.update { it.copy(emailStep = EmailStep.CODE, email = address) }
    }

    /** [name] : le pseudo voulu si le pirate est créé à cette occasion (sinon celui déjà connu du Worker). */
    fun verifyCode(code: String, name: String) = launchBusy {
        val session = api.verifyEmail(_uiState.value.email, code.trim(), PirateNames.clean(name))
        save(
            account.copy(
                onboarded = true,
                pirateName = session.me.name,
                token = session.token,
                email = session.me.email,
                kind = session.me.kind,
                public = session.me.public,
            ),
        )
        _uiState.update { it.copy(emailStep = EmailStep.ADDRESS) }
    }

    /** Retour à la saisie de l'adresse (faute de frappe, code jamais reçu...). */
    fun changeEmail() = _uiState.update { it.copy(emailStep = EmailStep.ADDRESS, error = null) }

    /** Relit le compte chez le Worker (pseudo, visibilité) ; un jeton refusé déconnecte en douceur. */
    fun refresh() {
        val token = account.token ?: return
        viewModelScope.launch {
            try {
                val me = api.me(token)
                save(account.copy(pirateName = me.name, public = me.public, email = me.email, kind = me.kind))
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
            return
        }
        launchBusy { save(account.copy(pirateName = api.updateMe(token, name = clean).name)) }
    }

    fun setPublic(public: Boolean) {
        val token = account.token ?: return
        launchBusy { save(account.copy(public = api.updateMe(token, public = public).public)) }
    }

    /** Déconnexion : le journal reste sur le téléphone, on redevient matelot anonyme. */
    fun logout() {
        val token = account.token ?: return
        launchBusy {
            runCatching { api.logout(token) }
            signOutLocally()
        }
    }

    /** Suppression du compte : l'identité disparaît chez le Worker, le pirate devient un « Ancien matelot ». */
    fun deleteAccount() {
        val token = account.token ?: return
        launchBusy {
            api.deleteMe(token)
            signOutLocally()
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

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

    private fun signOutLocally() = save(account.copy(token = null, email = null, kind = null))

    private fun save(next: Account) {
        store?.save(next)
        _uiState.update { it.copy(account = next) }
    }

    private fun launchBusy(block: suspend () -> Unit) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true, error = null) }
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
