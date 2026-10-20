package dev.mathieuburnat.piratefocus.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dev.mathieuburnat.piratefocus.account.AccountViewModel
import kotlinx.coroutines.launch

/**
 * L'ID client OAuth « Web » de Google (surtout pas celui du client Android) : l'ID token est émis pour lui,
 * et le Worker l'accepte (GOOGLE_CLIENT_IDS dans backend/wrangler.jsonc). Pas un secret.
 * Vide : le bouton Google reste caché.
 */
private const val GOOGLE_WEB_CLIENT_ID = "11309267566-unsfipric4p0s0rusokd63c8taknsvjg.apps.googleusercontent.com"

/** La connexion Google est configurée (ID client Web renseigné). */
val GOOGLE_SIGN_IN_READY = GOOGLE_WEB_CLIENT_ID.isNotEmpty()

/** Ce que fait le bouton « CONTINUER AVEC GOOGLE » : la fenêtre Google, puis le jeton part au Worker. */
@Composable
fun rememberGoogleSignIn(viewModel: AccountViewModel): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            when (val result = googleIdToken(context)) {
                is GoogleResult.Token -> viewModel.google(result.idToken)
                is GoogleResult.Failed -> viewModel.showError(result.message)
                GoogleResult.Cancelled -> Unit
            }
        }
    }
}

private sealed interface GoogleResult {
    data class Token(val idToken: String) : GoogleResult
    data class Failed(val message: String) : GoogleResult
    data object Cancelled : GoogleResult
}

/** Demande un ID token Google au téléphone (choix du compte Google par l'utilisateur). */
private suspend fun googleIdToken(context: Context): GoogleResult {
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(GOOGLE_WEB_CLIENT_ID).build())
        .build()
    return try {
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            GoogleResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            GoogleResult.Failed("Google a répondu en charabia, matelot. Réessaie !")
        }
    } catch (e: GetCredentialCancellationException) {
        GoogleResult.Cancelled
    } catch (e: GetCredentialException) {
        GoogleResult.Failed("Google ne répond pas : ${e.message ?: "brouillard en mer"}")
    } catch (e: GoogleIdTokenParsingException) {
        GoogleResult.Failed("Google a répondu en charabia, matelot. Réessaie !")
    }
}
