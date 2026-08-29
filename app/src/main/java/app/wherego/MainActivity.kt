package app.wherego

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.wherego.core.designsystem.theme.WheregoTheme
import app.wherego.core.designsystem.theme.WheregoType
import app.wherego.core.model.ThemeMode
import app.wherego.feature.settings.OnboardingRoute
import app.wherego.navigation.WheregoNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val dark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            WheregoTheme(darkTheme = dark) {
                val ready by viewModel.ready.collectAsStateWithLifecycle()
                val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()
                when {
                    !ready -> GuestSplash()
                    !onboardingDone -> OnboardingRoute()
                    else -> WheregoNavHost()
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun GuestSplash() {
    val colors = WheregoTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.paper),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Wherego",
            style = WheregoType.heroAmount,
            color = colors.ink,
        )
    }
}
