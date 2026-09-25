package com.optimusprime.wick.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.optimusprime.wick.ui.components.WickBottomBar
import com.optimusprime.wick.ui.screens.goals.GoalsScreen
import com.optimusprime.wick.ui.screens.home.HomeScreen
import com.optimusprime.wick.ui.screens.permissions.PermissionsScreen
import com.optimusprime.wick.ui.screens.permissions.hasCompletedOnboarding
import com.optimusprime.wick.ui.screens.progress.ProgressScreen
import com.optimusprime.wick.ui.screens.session.SessionScreen

private val mainTabs = listOf(
    "home" to "Home",
    "session" to "Study",
    "goals" to "Goals",
    "progress" to "Progress"
)

@Composable
fun WickNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val startDestination = if (hasCompletedOnboarding(context)) "home" else "permissions"

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = mainTabs.any { it.first == currentRoute }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            NavHost(navController = navController, startDestination = startDestination) {
                composable("permissions") {
                    PermissionsScreen(
                        onFinished = {
                            navController.navigate("home") {
                                popUpTo("permissions") { inclusive = true }
                            }
                        }
                    )
                }
                composable("home") {
                    HomeScreen(
                        onStartStudying = { navController.navigate("session") },
                        onSeeAllGoals = { navController.navigate("goals") }
                    )
                }
                composable("session") { SessionScreen() }
                composable("goals") { GoalsScreen() }
                composable("progress") { ProgressScreen() }
            }
        }

        if (showBottomBar) {
            WickBottomBar(
                items = mainTabs.map { it.second },
                selectedIndex = mainTabs.indexOfFirst { it.first == currentRoute }.coerceAtLeast(0),
                onSelect = { index ->
                    val route = mainTabs[index].first
                    if (route != currentRoute) {
                        // Navigating away from session must NOT save session state —
                        // the clock and camera should fully reset on next entry.
                        val leavingSession = currentRoute == "session"
                        navController.navigate(route) {
                            launchSingleTop = true
                            if (leavingSession) {
                                // Pop session off the stack entirely (no save).
                                popUpTo("session") { inclusive = true; saveState = false }
                            } else {
                                popUpTo("home") { saveState = true }
                                restoreState = true
                            }
                        }
                    }
                }
            )
        }
    }
}
