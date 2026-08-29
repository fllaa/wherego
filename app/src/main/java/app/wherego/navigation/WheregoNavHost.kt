package app.wherego.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.wherego.core.designsystem.component.WheregoTab
import app.wherego.core.designsystem.component.WheregoTabBar
import app.wherego.core.designsystem.theme.WheregoTheme
import app.wherego.feature.home.HomeRoute
import app.wherego.feature.plan.PlanScreen
import app.wherego.feature.settings.MeScreen
import app.wherego.feature.stories.StoriesScreen

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

    Column(
        modifier
            .fillMaxSize()
            .background(colors.paper),
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.Home,
            modifier = Modifier.weight(1f),
        ) {
            composable(Routes.Home) { HomeRoute() }
            composable(Routes.Stories) { StoriesScreen() }
            composable(Routes.Plan) { PlanScreen() }
            composable(Routes.Me) { MeScreen() }
        }
        WheregoTabBar(
            selected = selected,
            onSelect = { tab ->
                navController.navigate(tab.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
    }
}
