package app.wherego.core.sync

import kotlinx.coroutines.flow.Flow

data class AuthState(
    val firebaseUid: String?,
    val signedIn: Boolean,
) {
    companion object {
        val Guest = AuthState(firebaseUid = null, signedIn = false)
    }
}

interface AuthRepository {
    val state: Flow<AuthState>
    suspend fun current(): AuthState
    suspend fun signIn(): Result<AuthState>
    suspend fun signOut()
}
