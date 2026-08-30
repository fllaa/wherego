package com.flla.wherego.feature.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.sync.AuthRepository
import com.flla.wherego.core.sync.AuthState
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

    fun signIn(activity: Activity?, onResult: (String) -> Unit) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            try {
                if (!auth.current().signedIn) {
                    val act = activity ?: run {
                        onResult("Need an Activity to sign in.")
                        return@launch
                    }
                    auth.signIn(act).onFailure {
                        onResult(it.message ?: "Sign-in didn’t land.")
                        return@launch
                    }
                }
                val onboarded = runCatching { syncEngine.sync() }
                    .getOrElse {
                        onResult(it.message ?: "Couldn’t restore backup.")
                        return@launch
                    }
                fromBackup = onboarded
                syncScheduler.enqueueNow()
                onResult("Backup is on. Capture never waited.")
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
    var message by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "← Me",
            style = WheregoType.cardTitle,
            color = colors.ink,
            modifier = Modifier.clickable(onClick = onBack),
        )
        Text("Backup", style = WheregoType.cardTitle, color = colors.ink)
        Text(
            "Sign in to backup. Capture never waits on this.",
            style = WheregoType.meta,
            color = colors.muted,
        )
        if (authState.signedIn) {
            Text(
                authState.email ?: authState.displayName ?: "Signed in",
                style = WheregoType.meta,
                color = colors.ink,
            )
            Text(
                "Sign out",
                color = colors.white,
                style = WheregoType.cta,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.teal)
                    .clickable {
                        viewModel.signOut()
                        message = "Local stays. Cloud paused."
                    }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        } else {
            Text(
                "Sign in with Google",
                color = colors.white,
                style = WheregoType.cta,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.teal)
                    .clickable {
                        val activity = context.findActivity()
                        if (activity == null) {
                            message = "Need an Activity to sign in."
                        } else {
                            viewModel.signIn(activity) { message = it }
                        }
                    }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }
        message?.let {
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
