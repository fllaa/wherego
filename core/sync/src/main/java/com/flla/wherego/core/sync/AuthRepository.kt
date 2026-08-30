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

interface AuthRepository {
    val state: Flow<AuthState>
    suspend fun current(): AuthState
    suspend fun signIn(activity: Activity): Result<AuthState>
    suspend fun signOut()
}
