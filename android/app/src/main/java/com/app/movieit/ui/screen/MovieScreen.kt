package com.app.movieit.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.movieit.ui.viewmodel.MoviesViewModel

@Composable
fun MoviesScreen(
    onLoggedOut: () -> Unit,
    viewModel: MoviesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) {
            onLoggedOut()
            viewModel.consumeLoggedOut()
        }
    }

    Column {
        Button(onClick = { viewModel.logout() }) {
            Text("Logout")
        }

        when {
            state.loading -> CircularProgressIndicator()
            state.error != null -> Text("Error: ${state.error}")
            else -> LazyColumn {
                items(state.movies.size) { idx ->
                    Text(state.movies[idx].title)
                }
            }
        }
    }
}