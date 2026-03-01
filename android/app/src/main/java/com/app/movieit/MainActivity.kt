package com.app.movieit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.app.movieit.ui.screen.LoginScreen
import com.app.movieit.ui.theme.MovieITTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LoginScreen(
                onLoginSuccess = { /* treci la MoviesScreen() */ },
                onNavigateToRegister = { /* deschizi RegisterScreen */ }
            )
            MovieItApp()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MovieITTheme {}
}