package com.sasayaki.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sasayaki.ui.history.HistoryScreen
import com.sasayaki.ui.home.HomeScreen
import com.sasayaki.ui.profiles.ProfilesScreen
import com.sasayaki.ui.settings.SettingsScreen

@Composable
fun SasayakiNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showNavigationBar = currentDestination == null || currentDestination.topLevelDestination() != null
    val transitions = rememberNavTransitions()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = showNavigationBar,
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastEffectsSpec())
            ) {
                ShortNavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination.isIn(destination)
                        ShortNavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTopLevel(destination) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.icon,
                                    contentDescription = null
                                )
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
            enterTransition = transitions.enter,
            exitTransition = transitions.exit,
            popEnterTransition = transitions.popEnter,
            popExitTransition = transitions.popExit
        ) {
            composable<HomeRoute> {
                HomeScreen(outerPadding = WindowInsets.statusBars.asPaddingValues())
            }
            composable<HistoryRoute> {
                HistoryScreen()
            }
            composable<ProfilesRoute> {
                ProfilesScreen(outerPadding = PaddingValues())
            }
            composable<SettingsRoute> {
                SettingsScreen(outerPadding = PaddingValues())
            }
        }
    }
}
