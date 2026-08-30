package com.flla.wherego.core.sync

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.flla.wherego.core.database.UserProfileStore
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Singleton
class FirebaseAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profiles: UserProfileStore,
) : AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(auth.currentUser.toState())
    override val state: Flow<AuthState> = _state.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val next = firebaseAuth.currentUser.toState()
            _state.value = next
            val user = firebaseAuth.currentUser
            if (user != null) {
                scope.launch { link(user) }
            }
        }
    }

    override suspend fun current(): AuthState = auth.currentUser.toState()

    override suspend fun signIn(activity: Activity): Result<AuthState> {
        val webClientId = webClientId()
            ?: return Result.failure(SignInException(SignInFailure.MISSING_CLIENT_ID))
        return try {
            val idToken = requestIdToken(activity, webClientId)
                ?: return Result.failure(SignInException(SignInFailure.NO_ID_TOKEN))
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val user = auth.signInWithCredential(credential).await().user
                ?: return Result.failure(SignInException(SignInFailure.NO_USER))
            link(user)
            val next = user.toState()
            _state.value = next
            Result.success(next)
        } catch (_: GetCredentialCancellationException) {
            Result.failure(SignInException(SignInFailure.CANCELLED))
        } catch (_: NoCredentialException) {
            Result.failure(SignInException(SignInFailure.NO_GOOGLE_ACCOUNT))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        auth.signOut()
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
            // Local session already cleared.
        }
        _state.value = AuthState.Guest
    }

    private suspend fun requestIdToken(activity: Activity, webClientId: String): String {
        return try {
            requestGoogleId(activity, webClientId, filterAuthorized = true)
        } catch (_: NoCredentialException) {
            try {
                requestGoogleId(activity, webClientId, filterAuthorized = false)
            } catch (_: NoCredentialException) {
                requestSignInButton(activity, webClientId)
            }
        }
    }

    private suspend fun requestGoogleId(
        activity: Activity,
        webClientId: String,
        filterAuthorized: Boolean,
    ): String {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(filterAuthorized)
            .setAutoSelectEnabled(filterAuthorized)
            .build()
        return getIdToken(activity, option)
    }

    private suspend fun requestSignInButton(activity: Activity, webClientId: String): String {
        val option = GetSignInWithGoogleOption.Builder(webClientId).build()
        return getIdToken(activity, option)
    }

    private suspend fun getIdToken(
        activity: Activity,
        option: androidx.credentials.CredentialOption,
    ): String {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val response = withContext(Dispatchers.Main.immediate) {
            CredentialManager.create(activity).getCredential(activity, request)
        }
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        throw SignInException(SignInFailure.NOT_GOOGLE_CREDENTIAL)
    }

    private suspend fun link(user: FirebaseUser) {
        val googleSub = user.providerData
            .firstOrNull { it.providerId == GoogleAuthProvider.PROVIDER_ID }
            ?.uid
        profiles.linkGoogle(
            firebaseUid = user.uid,
            googleSub = googleSub,
            email = user.email,
            displayName = user.displayName,
            photoUrl = user.photoUrl?.toString(),
        )
    }

    private fun webClientId(): String? {
        val id = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName,
        )
        if (id == 0) return null
        return context.getString(id).trim().takeIf { it.isNotEmpty() }
    }

    private fun FirebaseUser?.toState(): AuthState {
        if (this == null) return AuthState.Guest
        val googleSub = providerData
            .firstOrNull { it.providerId == GoogleAuthProvider.PROVIDER_ID }
            ?.uid
        return AuthState(
            firebaseUid = uid,
            signedIn = true,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl?.toString(),
            googleSub = googleSub,
        )
    }
}
