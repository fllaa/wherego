package com.flla.wherego.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flla.wherego.core.designsystem.component.GoMood
import com.flla.wherego.core.designsystem.component.WheregoTab
import com.flla.wherego.core.designsystem.component.WheregoTabBar
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.feature.capture.CaptureSheet
import com.flla.wherego.feature.capture.cameraCaptureUri
import com.flla.wherego.feature.home.HomeRoute
import com.flla.wherego.feature.plan.PlanRoute
import com.flla.wherego.feature.settings.MeScreen
import com.flla.wherego.feature.stories.StoriesRoute
import kotlinx.coroutines.delay

private object Routes {
    const val Home = "home"
    const val Stories = "stories"
    const val Plan = "plan"
    const val Me = "me"
}

private fun String.toTab(): WheregoTab = when (this) {
    Routes.Stories -> WheregoTab.Stories
    Routes.Plan -> WheregoTab.Plan
    Routes.Me -> WheregoTab.Me
    else -> WheregoTab.Home
}

private val WheregoTab.route: String
    get() = when (this) {
        WheregoTab.Home -> Routes.Home
        WheregoTab.Stories -> Routes.Stories
        WheregoTab.Plan -> Routes.Plan
        WheregoTab.Me -> Routes.Me
    }

@Composable
fun WheregoNavHost(
    modifier: Modifier = Modifier,
    openCaptureOnStart: Boolean = false,
) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val selected = (entry?.destination?.route ?: Routes.Home).toTab()
    val colors = WheregoTheme.colors
    val context = LocalContext.current
    var captureOpen by remember { mutableStateOf(openCaptureOnStart) }
    var editing by remember { mutableStateOf<Transaction?>(null) }
    var fastScanUri by remember { mutableStateOf<Uri?>(null) }
    var cameraFastUri by remember { mutableStateOf<Uri?>(null) }
    var goMood by remember { mutableStateOf(GoMood.Idle) }

    fun openCapture(tx: Transaction?, initialReceipt: Uri? = null) {
        editing = tx
        fastScanUri = initialReceipt
        captureOpen = true
    }

    val takeFastPicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { ok ->
        val uri = cameraFastUri
        if (ok && uri != null) {
            openCapture(null, uri)
        }
    }

    val requestFastCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = cameraCaptureUri(context)
            cameraFastUri = uri
            takeFastPicture.launch(uri)
        }
    }

    fun launchFastScan() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            val uri = cameraCaptureUri(context)
            cameraFastUri = uri
            takeFastPicture.launch(uri)
        } else {
            requestFastCamera.launch(Manifest.permission.CAMERA)
        }
    }

    fun go(tab: WheregoTab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.paper),
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.Home,
                modifier = Modifier.weight(1f),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                composable(Routes.Home) {
                    HomeRoute(
                        onOpenPlan = { go(WheregoTab.Plan) },
                        onOpenStories = { go(WheregoTab.Stories) },
                        onOpenCapture = ::openCapture,
                        goMood = goMood,
                    )
                }
                composable(Routes.Stories) { StoriesRoute() }
                composable(Routes.Plan) { PlanRoute() }
                composable(Routes.Me) { MeScreen() }
            }
            WheregoTabBar(
                selected = selected,
                onSelect = ::go,
                onAdd = { openCapture(null) },
                onScanReceipt = ::launchFastScan,
            )
        }
        if (captureOpen) {
            CaptureSheet(
                editing = editing,
                initialReceiptUri = fastScanUri,
                onDismiss = {
                    captureOpen = false
                    editing = null
                    fastScanUri = null
                },
                onParked = {
                    goMood = GoMood.Happy
                },
            )
        }
    }
    LaunchedEffect(goMood) {
        if (goMood == GoMood.Happy) {
            delay(800)
            goMood = GoMood.Idle
        }
    }
}
