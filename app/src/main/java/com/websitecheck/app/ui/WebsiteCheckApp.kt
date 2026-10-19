package com.websitecheck.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.websitecheck.app.ui.log.LogScreen
import com.websitecheck.app.ui.settings.SettingsScreen
import com.websitecheck.app.ui.site.SiteEditScreen
import com.websitecheck.app.ui.site.SiteListScreen

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination("sites", "網站", Icons.Outlined.Language),
    TopLevelDestination("logs", "紀錄", Icons.Outlined.History),
    TopLevelDestination("settings", "設定", Icons.Outlined.Settings),
)

@Composable
fun WebsiteCheckApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in topLevelDestinations.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "sites",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("sites") {
                SiteListScreen(
                    onAdd = { navController.navigate("edit?siteId=0") },
                    onEdit = { siteId -> navController.navigate("edit?siteId=$siteId") },
                )
            }
            composable("logs") { LogScreen() }
            composable("settings") { SettingsScreen() }
            composable(
                route = "edit?siteId={siteId}",
                arguments = listOf(
                    navArgument("siteId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                ),
            ) { entry ->
                SiteEditScreen(
                    siteId = entry.arguments?.getLong("siteId") ?: 0L,
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}
