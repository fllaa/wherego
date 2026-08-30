package com.flla.wherego.navigation

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
import com.flla.wherego.feature.capture.ReceiptAttachDialog
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
fun WheregoNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val selected = (entry?.destination?.route ?: Routes.Home).toTab()
    val colors = WheregoTheme.colors
    var captureOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Transaction?>(null) }
    var receiptTxId by remember { mutableStateOf<String?>(null) }
    var goMood by remember { mutableStateOf(GoMood.Idle) }

    fun openCapture(tx: Transaction?) {
        editing = tx
        captureOpen = true
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
            )
        }
        if (captureOpen) {
            CaptureSheet(
                editing = editing,
                onDismiss = {
                    captureOpen = false
                    editing = null
                },
                onParked = { parked ->
                    goMood = GoMood.Happy
                    receiptTxId = parked.id
                },
            )
        }
        val pendingReceipt = receiptTxId
        if (pendingReceipt != null) {
            ReceiptAttachDialog(
                transactionId = pendingReceipt,
                onFinished = { receiptTxId = null },
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
