package com.cinemagallery.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cinemagallery.domain.model.MediaItem
import com.cinemagallery.ui.details.MovieDetailsScreen
import com.cinemagallery.ui.details.TvShowDetailsScreen
import com.cinemagallery.ui.library.LibraryScreen
import com.cinemagallery.ui.settings.SettingsScreen
import com.cinemagallery.ui.unmatched.UnmatchedFilesScreen

sealed class NavDestination(val route: String, val label: String, val icon: ImageVector) {
    object Library : NavDestination("library", "Library", Icons.Default.VideoLibrary)
    object Unmatched : NavDestination("unmatched", "Unmatched", Icons.Default.Build)
    object Settings : NavDestination("settings", "Settings", Icons.Default.Settings)
}

val bottomNavDestinations = listOf(
    NavDestination.Library,
    NavDestination.Unmatched,
    NavDestination.Settings
)

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBottomBarVisible = bottomNavDestinations.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavDestinations.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.background,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavDestination.Library.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavDestination.Library.route) {
                LibraryScreen(
                    viewModel = hiltViewModel(),
                    onMediaClick = { media ->
                        when (media) {
                            is MediaItem.Movie -> navController.navigate("movie_details/${media.id}")
                            is MediaItem.TvShow -> navController.navigate("tv_details/${media.id}")
                        }
                    }
                )
            }

            composable(
                route = "movie_details/{movieId}",
                arguments = listOf(navArgument("movieId") { type = NavType.StringType })
            ) {
                MovieDetailsScreen(
                    viewModel = hiltViewModel(),
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = "tv_details/{tvShowId}",
                arguments = listOf(navArgument("tvShowId") { type = NavType.StringType })
            ) {
                TvShowDetailsScreen(
                    viewModel = hiltViewModel(),
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(NavDestination.Unmatched.route) {
                UnmatchedFilesScreen(
                    viewModel = hiltViewModel()
                )
            }

            composable(NavDestination.Settings.route) {
                SettingsScreen(
                    viewModel = hiltViewModel()
                )
            }
        }
    }
}
