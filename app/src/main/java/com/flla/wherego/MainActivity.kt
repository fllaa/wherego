package com.flla.wherego

import android.content.Context
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import androidx.compose.ui.res.stringResource
import com.flla.wherego.core.i18n.ProvideAppLanguage
import com.flla.wherego.core.i18n.AppLocale
import com.flla.wherego.core.designsystem.theme.WheregoType
import com.flla.wherego.core.model.ThemeMode
import com.flla.wherego.feature.auth.WelcomeScreen
import com.flla.wherego.feature.settings.OnboardingRoute
import com.flla.wherego.navigation.WheregoNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.context(newBase, AppLocale.load(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val language by viewModel.language.collectAsStateWithLifecycle()
            val ready by viewModel.ready.collectAsStateWithLifecycle()
            LaunchedEffect(ready, language) {
                if (ready) AppLocale.applyAndRecreateIfNeeded(this@MainActivity, language)
            }
            ProvideAppLanguage(language) {
                val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
                val dark = when (themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    else -> isSystemInDarkTheme()
                }
                WheregoTheme(darkTheme = dark) {
                    val ready by viewModel.ready.collectAsStateWithLifecycle()
                    val welcomeSeen by viewModel.welcomeSeen.collectAsStateWithLifecycle()
                    val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()
                    var openCaptureOnStart by remember { mutableStateOf(false) }
                    var skipOnboarding by remember { mutableStateOf(false) }
                    LaunchedEffect(welcomeSeen) {
                        if (welcomeSeen == false) skipOnboarding = false
                    }
                    when {
                        !ready || welcomeSeen == null -> GuestSplash()
                        welcomeSeen == false -> WelcomeScreen(
                            onContinue = { fromBackup ->
                                skipOnboarding = fromBackup
                                viewModel.setWelcomeSeen(true)
                            },
                        )
                        !onboardingDone && !skipOnboarding -> OnboardingRoute(
                            onBackToWelcome = { viewModel.setWelcomeSeen(false) },
                            onFinish = { openCapture -> openCaptureOnStart = openCapture },
                        )
                        else -> WheregoNavHost(openCaptureOnStart = openCaptureOnStart)
                    }
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
            text = stringResource(R.string.app_name),
            style = WheregoType.heroAmount,
            color = colors.ink,
        )
    }
}
