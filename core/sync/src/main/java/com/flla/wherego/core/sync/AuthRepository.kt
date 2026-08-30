package com.flla.wherego.core.sync

import android.app.Activity
import kotlinx.coroutines.flow.Flow

data class AuthState(
    val firebaseUid: String?,
    val signedIn: Boolean,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val googleSub: String? = null,
) {
    companion object {
        val Guest = AuthState(firebaseUid = null, signedIn = false)
    }
}

enum class SignInFailure {
    MISSING_CLIENT_ID,
    NO_ID_TOKEN,
    NO_USER,
    CANCELLED,
    NO_GOOGLE_ACCOUNT,
    NOT_GOOGLE_CREDENTIAL,
}

class SignInException(val failure: SignInFailure) : Exception(failure.name)

interface AuthRepository {
    val state: Flow<AuthState>
    suspend fun current(): AuthState
    suspend fun signIn(activity: Activity): Result<AuthState>
    suspend fun signOut()
}
