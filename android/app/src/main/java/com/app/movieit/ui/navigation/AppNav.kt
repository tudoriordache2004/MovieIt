package com.app.movieit.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.movieit.ui.screen.AuthGateScreen
import com.app.movieit.ui.screen.LoginScreen
import com.app.movieit.ui.screen.MoviesScreen
import com.app.movieit.ui.screen.RegisterScreen
import com.app.movieit.util.Routes
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.app.movieit.ui.screen.MovieDetailScreen
import com.app.movieit.ui.screen.MyDiaryScreen
import com.app.movieit.ui.screen.ProfileScreen
import com.app.movieit.ui.screen.WatchlistScreen

@Composable
fun AppNav() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.AUTH_GATE) {

        composable(Routes.AUTH_GATE) {
            AuthGateScreen(navController = navController)
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MOVIES) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.MOVIES) { backStackEntry ->
            val shouldRefresh = backStackEntry.savedStateHandle.get<Boolean>("refresh_movies") ?: false
            
            MoviesScreen(
                shouldRefresh = shouldRefresh,

                onRefreshHandled = {
                    backStackEntry.savedStateHandle["refresh_movies"] = false
                },

                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MOVIES) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onMovieClick = { movieId ->
                    navController.navigate(Routes.movieDetails(movieId))
                },
                onOpenWatchlist = {
                    navController.navigate(Routes.WATCHLIST)
                },
                onOpenDiary = {
                    navController.navigate(Routes.DIARY)
                },
                onOpenProfile = {
                    navController.navigate(Routes.PROFILE)
                }
            )
        }

        composable(
            route = Routes.MOVIE_DETAILS,
            arguments = listOf(navArgument("movieId") { type = NavType.IntType })
        ) {
            MovieDetailScreen(
                onBack = {
                    val prev = navController.previousBackStackEntry
                    when (prev?.destination?.route) {
                        // fetch constant la flags pe back button pentru updates
                        Routes.MOVIES -> prev.savedStateHandle["refresh_movies"] = true
                        Routes.WATCHLIST -> prev.savedStateHandle["refresh_needed"] = true
                        Routes.PROFILE -> prev.savedStateHandle["refresh_profile"] = true
                        Routes.DIARY -> prev.savedStateHandle["refresh_diary"] = true
                    }
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.WATCHLIST) { backStackEntry -> // <-- Adaugă backStackEntry aici
            val shouldRefresh = backStackEntry.savedStateHandle.get<Boolean>("refresh_needed") ?: false

            WatchlistScreen(
                shouldRefresh = shouldRefresh,
                onRefreshHandled = {
                    // Dupa refresh in watchlist schimbam flag-ul
                    backStackEntry.savedStateHandle["refresh_needed"] = false
                },
                onBack = {
                    val prev = navController.previousBackStackEntry
                    if (prev?.destination?.route == Routes.PROFILE) {
                        prev.savedStateHandle["refresh_profile"] = true
                    }
                    navController.popBackStack()
                },
                onMovieClick = { movieId ->
                    navController.navigate(Routes.movieDetails(movieId))
                }
            )
        }

        composable(Routes.DIARY) {
            MyDiaryScreen(
                onBack = {
                    val prev = navController.previousBackStackEntry
                    if (prev?.destination?.route == Routes.PROFILE) {
                        prev.savedStateHandle["refresh_profile"] = true
                    }
                    navController.popBackStack()
                },
                onMovieClick = { movieId ->
                    navController.navigate(Routes.movieDetails(movieId))
                }
            )
        }

        composable(Routes.PROFILE) { backStackEntry ->
            val shouldRefresh = backStackEntry.savedStateHandle.get<Boolean>("refresh_profile") ?: false

            ProfileScreen(
                shouldRefresh = shouldRefresh,
                onRefreshHandled = {
                    backStackEntry.savedStateHandle["refresh_profile"] = false
                },
                onBack = { navController.popBackStack() },
                onOpenDiary = { navController.navigate(Routes.DIARY) },
                onOpenWatchlist = { navController.navigate(Routes.WATCHLIST) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MOVIES) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}