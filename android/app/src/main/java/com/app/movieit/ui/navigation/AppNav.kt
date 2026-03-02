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

        composable(Routes.MOVIES) {
            MoviesScreen(
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MOVIES) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}