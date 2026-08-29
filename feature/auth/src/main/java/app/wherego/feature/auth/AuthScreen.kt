package app.wherego.feature.auth

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wherego.core.designsystem.theme.WheregoTheme
import app.wherego.core.designsystem.theme.WheregoType
import app.wherego.core.sync.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {
    fun signIn(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = auth.signIn()
            onResult(result.exceptionOrNull()?.message ?: "Signed in")
        }
    }
}

@Composable
fun AuthScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val colors = WheregoTheme.colors
    val scope = rememberCoroutineScope()
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
        Text(
            "Sign in with Google",
            color = colors.white,
            style = WheregoType.cta,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(colors.teal)
                .clickable {
                    viewModel.signIn { message = it }
                }
                .padding(horizontal = 18.dp, vertical = 12.dp),
        )
        message?.let {
            Text(it, style = WheregoType.meta, color = colors.coral)
        }
    }
}
