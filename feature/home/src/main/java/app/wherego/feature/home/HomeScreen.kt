package app.wherego.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.wherego.core.designsystem.component.WheregoGoAvatar
import app.wherego.core.designsystem.component.WheregoHero
import app.wherego.core.designsystem.theme.WheregoTheme
import app.wherego.core.designsystem.theme.WheregoType

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val firstName = profile?.displayName?.substringBefore(" ")?.takeIf { it.isNotBlank() }
    HomeScreen(greetingName = firstName ?: "you")
}

@Composable
fun HomeScreen(
    greetingName: String,
    modifier: Modifier = Modifier,
) {
    val colors = WheregoTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WheregoGoAvatar()
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Hey $greetingName 👋",
                style = WheregoType.greeting,
                color = colors.ink,
            )
        }
        Spacer(Modifier.height(24.dp))
        WheregoHero(amountLabel = "Rp 0")
    }
}
