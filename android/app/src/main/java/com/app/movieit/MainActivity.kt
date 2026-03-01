package com.app.movieit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.movieit.ui.screen.LoginScreen
import com.app.movieit.ui.screen.RegisterScreen
import com.app.movieit.ui.theme.MovieITTheme
import com.app.movieit.ui.viewmodel.RegisterViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MovieITTheme {
                var showRegister by remember { mutableStateOf(false) }

                if (showRegister) {
                    RegisterScreen(
                        onRegisterSuccess = { /* același ca login: treci la Movies */ },
                        onNavigateToLogin = { showRegister = false }
                    )
                } else {
                    LoginScreen(
                        onLoginSuccess = { /* treci la MoviesScreen */ },
                        onNavigateToRegister = { showRegister = true }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MovieITTheme {}
}