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
                }
            )
        }

        composable(
            route = Routes.MOVIE_DETAILS,
            arguments = listOf(navArgument("movieId") { type = NavType.IntType })
        ) {
            MovieDetailScreen(
                onBack = {
                    // Pentru refresh in frontend la reviews
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("refresh_movies", true)

                    navController.popBackStack()
                }
            )
        }

        composable(Routes.WATCHLIST) {
            WatchlistScreen(
                onBack = { navController.popBackStack() },
                onMovieClick = { movieId ->
                    navController.navigate(Routes.movieDetails(movieId))
                }
            )
        }

        composable(Routes.DIARY) {
            MyDiaryScreen(
                onBack = { navController.popBackStack() },
                onMovieClick = { movieId ->
                    navController.navigate(Routes.movieDetails(movieId))
                }
            )
        }
    }
}