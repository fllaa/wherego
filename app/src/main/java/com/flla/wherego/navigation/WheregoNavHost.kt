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
import com.flla.wherego.CaptureRequest
import com.flla.wherego.core.designsystem.component.GoMood
import com.flla.wherego.core.designsystem.component.WheregoTab
import com.flla.wherego.core.designsystem.component.WheregoTabBar
import com.flla.wherego.core.designsystem.theme.WheregoTheme
import com.flla.wherego.core.model.ReceiptSource
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.feature.capture.CaptureSheet
import com.flla.wherego.feature.capture.cameraCaptureUri
import com.flla.wherego.feature.home.HomeRoute
import com.flla.wherego.feature.plan.PlanRoute
import com.flla.wherego.feature.settings.MeScreen
import com.flla.wherego.feature.stories.StoriesRoute
import com.flla.wherego.quicksettings.addCaptureTilePrompt
import java.io.File
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
    request: CaptureRequest = CaptureRequest.None,
    onRequestHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val selected = (entry?.destination?.route ?: Routes.Home).toTab()
    val colors = WheregoTheme.colors
    val context = LocalContext.current
    val quickTilePrompt = remember(context) { addCaptureTilePrompt(context) }
    var captureOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Transaction?>(null) }
    var fastScanUri by remember { mutableStateOf<Uri?>(null) }
    var fastScanSource by remember { mutableStateOf(ReceiptSource.OWN) }
    var cameraFastUri by remember { mutableStateOf<Uri?>(null) }
    var goMood by remember { mutableStateOf(GoMood.Idle) }

    fun openCapture(
        tx: Transaction?,
        initialReceipt: Uri? = null,
        source: ReceiptSource = ReceiptSource.OWN,
    ) {
        editing = tx
        fastScanUri = initialReceipt
        fastScanSource = source
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

    /**
     * The Quick Settings tile and the share sheet both arrive here as a [CaptureRequest].
     *
     * [ReceiptSource.SHARED] because the image came from another app: its read is offered rather
     * than filled in, and the image is never queued for backup. It is moot when the tile sent us
     * here with no image at all.
     */
    LaunchedEffect(request) {
        if (request.isEmpty) return@LaunchedEffect
        openCapture(
            tx = null,
            initialReceipt = request.receiptPath?.let { Uri.fromFile(File(it)) },
            source = ReceiptSource.SHARED,
        )
        onRequestHandled()
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
                composable(Routes.Me) { MeScreen(onAddQuickTile = quickTilePrompt) }
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
                initialReceiptSource = fastScanSource,
                onDismiss = {
                    captureOpen = false
                    editing = null
                    fastScanUri = null
                    fastScanSource = ReceiptSource.OWN
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
