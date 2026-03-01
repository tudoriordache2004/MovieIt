package com.app.movieit.ui.screen

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.app.movieit.ui.viewmodel.AuthGateState
import com.app.movieit.ui.viewmodel.AuthGateViewModel
import com.app.movieit.util.Routes

@Composable
fun AuthGateScreen(
    navController: NavController,
    viewModel: AuthGateViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        when (state) {
            AuthGateState.Authed -> {
                navController.navigate(Routes.MOVIES) {
                    popUpTo(Routes.AUTH_GATE) { inclusive = true }
                    launchSingleTop = true
                }
            }
            AuthGateState.Unauthed -> {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.AUTH_GATE) { inclusive = true }
                    launchSingleTop = true
                }
            }
            AuthGateState.Loading -> Unit
        }
    }

    // UI minimal cat timp decidem
    CircularProgressIndicator()
}