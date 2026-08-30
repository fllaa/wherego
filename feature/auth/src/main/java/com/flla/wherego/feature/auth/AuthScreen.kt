package com.flla.wherego.feature.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.i18n.R
import com.flla.wherego.core.sync.AuthRepository
import com.flla.wherego.core.sync.AuthState
import com.flla.wherego.core.sync.SignInException
import com.flla.wherego.core.sync.SignInFailure
import com.flla.wherego.core.sync.SyncEngine
import com.flla.wherego.core.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SignInResult {
    data object Ok : SignInResult
    data class Failed(@StringRes val messageRes: Int) : SignInResult
    data class FailedRaw(val message: String) : SignInResult
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val syncScheduler: SyncScheduler,
    private val syncEngine: SyncEngine,
) : ViewModel() {
    val state: StateFlow<AuthState> = auth.state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AuthState.Guest,
    )

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** True when the last successful Google continue adopted an onboarded cloud profile. */
    var fromBackup: Boolean = false
        private set

    fun signIn(activity: Activity?, onResult: (SignInResult) -> Unit) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            try {
                if (!auth.current().signedIn) {
                    val act = activity ?: run {
                        onResult(SignInResult.Failed(R.string.auth_err_need_activity))
                        return@launch
                    }
                    auth.signIn(act).onFailure {
                        onResult(it.toSignInResult())
                        return@launch
                    }
                }
                val onboarded = runCatching { syncEngine.sync() }
                    .getOrElse {
                        onResult(SignInResult.Failed(R.string.auth_err_restore_failed))
                        return@launch
                    }
                fromBackup = onboarded
                syncScheduler.enqueueNow()
                onResult(SignInResult.Ok)
            } finally {
                _busy.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }
}

@Composable
fun AuthScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val colors = WheregoTheme.colors
    val context = LocalContext.current
    val authState by viewModel.state.collectAsStateWithLifecycle()
    var result by remember { mutableStateOf<SignInResult?>(null) }
    var signedOut by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.auth_back_me),
            style = WheregoType.cardTitle,
            color = colors.ink,
            modifier = Modifier.clickable(onClick = onBack),
        )
        Text(stringResource(R.string.auth_title), style = WheregoType.cardTitle, color = colors.ink)
        Text(
            stringResource(R.string.auth_body),
            style = WheregoType.meta,
            color = colors.muted,
        )
        if (authState.signedIn) {
            Text(
                authState.email ?: authState.displayName ?: stringResource(R.string.auth_signed_in),
                style = WheregoType.meta,
                color = colors.ink,
            )
            Text(
                stringResource(R.string.auth_sign_out),
                color = colors.white,
                style = WheregoType.cta,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.teal)
                    .clickable {
                        viewModel.signOut()
                        signedOut = true
                        result = null
                    }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        } else {
            Text(
                stringResource(R.string.auth_sign_in_google),
                color = colors.white,
                style = WheregoType.cta,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.teal)
                    .clickable {
                        signedOut = false
                        viewModel.signIn(context.findActivity()) { result = it }
                    }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }
        val banner = if (signedOut) {
            stringResource(R.string.auth_sign_out_done)
        } else {
            result?.let { signInResultMessage(it) }
        }
        banner?.let {
            Text(it, style = WheregoType.meta, color = colors.coral)
        }
    }
}

internal fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
internal fun signInResultMessage(result: SignInResult): String = when (result) {
    SignInResult.Ok -> stringResource(R.string.auth_ok_backup_on)
    is SignInResult.Failed -> stringResource(result.messageRes)
    is SignInResult.FailedRaw ->
        result.message.ifBlank { stringResource(R.string.auth_err_sign_in_failed) }
}

private fun Throwable.toSignInResult(): SignInResult {
    val failure = (this as? SignInException)?.failure
    return when (failure) {
        SignInFailure.MISSING_CLIENT_ID -> SignInResult.Failed(R.string.auth_err_missing_client_id)
        SignInFailure.NO_ID_TOKEN -> SignInResult.Failed(R.string.auth_err_no_id_token)
        SignInFailure.NO_USER -> SignInResult.Failed(R.string.auth_err_no_user)
        SignInFailure.CANCELLED -> SignInResult.Failed(R.string.auth_err_cancelled)
        SignInFailure.NO_GOOGLE_ACCOUNT -> SignInResult.Failed(R.string.auth_err_no_google_account)
        SignInFailure.NOT_GOOGLE_CREDENTIAL -> SignInResult.Failed(R.string.auth_err_not_google_credential)
        null -> SignInResult.FailedRaw(message ?: "")
    }
}
