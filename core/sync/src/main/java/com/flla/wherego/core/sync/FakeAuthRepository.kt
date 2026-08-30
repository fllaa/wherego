package com.flla.wherego.core.sync

import android.app.Activity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {
    private val _state = MutableStateFlow(AuthState.Guest)
    override val state: Flow<AuthState> = _state.asStateFlow()

    override suspend fun current(): AuthState = _state.value

    override suspend fun signIn(activity: Activity): Result<AuthState> =
        Result.failure(IllegalStateException("FakeAuthRepository is not bound."))

    override suspend fun signOut() {
        _state.value = AuthState.Guest
    }
}
