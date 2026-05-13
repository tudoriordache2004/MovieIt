package com.app.movieit.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.movieit.ui.screen.AuthGateScreen
import com.app.movieit.ui.screen.DiaryLogScreen
import com.app.movieit.ui.screen.FollowListScreen
import com.app.movieit.ui.screen.HomeScreen
import com.app.movieit.ui.screen.LoginScreen
import com.app.movieit.ui.screen.MovieDetailScreen
import com.app.movieit.ui.screen.MovieItLensScreen
import com.app.movieit.ui.screen.MoviePickerResultScreen
import com.app.movieit.ui.screen.MoviePickerScreen
import com.app.movieit.ui.screen.MoviesScreen
import com.app.movieit.ui.viewmodel.MoviePickerViewModel
import androidx.navigation.navigation
import com.app.movieit.ui.screen.MyDiaryScreen
import com.app.movieit.ui.screen.ProfileScreen
import com.app.movieit.ui.screen.RegisterScreen
import com.app.movieit.ui.screen.DirectorProfileScreen
import com.app.movieit.ui.screen.UserProfileScreen
import com.app.movieit.ui.screen.WatchlistScreen
import com.app.movieit.ui.theme.AccentPurple
import com.app.movieit.ui.theme.DeepBlack
import com.app.movieit.ui.theme.GlowPurple
import com.app.movieit.ui.theme.TextSecondary
import com.app.movieit.util.Routes

private val bottomNavRoutes = setOf(
    Routes.HOME,
    Routes.MOVIES,
    Routes.WATCHLIST,
    Routes.DIARY,
    Routes.PROFILE,
    Routes.MOVIE_DETAILS,
    Routes.USER_PROFILE,
    Routes.FOLLOW_LIST,
    Routes.DIARY_LOG
)

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomNav = currentRoute in bottomNavRoutes ||
            currentRoute?.startsWith("movie/") == true ||
            currentRoute?.startsWith("user_profile/") == true ||
            currentRoute?.startsWith("follow_list/") == true ||
            currentRoute?.startsWith("diary_log/") == true ||
            currentRoute?.startsWith("director/") == true

    Scaffold(
        containerColor = DeepBlack,
        bottomBar = {
            if (showBottomNav) {
                AppBottomNavBar(
                    currentRoute = currentRoute,
                    navController = navController
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.AUTH_GATE,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {

            composable(Routes.AUTH_GATE) {
                AuthGateScreen(navController = navController)
            }

            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
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

            composable(Routes.HOME) { backStackEntry ->
                val shouldRefresh = backStackEntry.savedStateHandle.get<Boolean>("refresh_home") ?: false
                HomeScreen(
                    shouldRefresh = shouldRefresh,
                    onRefreshHandled = { backStackEntry.savedStateHandle["refresh_home"] = false },
                    onMovieClick = { movieId ->
                        navController.navigate(Routes.movieDetails(movieId))
                    },
                    onPickerClick = {
                        navController.navigate(Routes.MOVIE_PICKER)
                    },
                    onLensClick = {
                        navController.navigate(Routes.MOVIEIT_LENS)
                    }
                )
            }

            navigation(
                route = Routes.MOVIE_PICKER,
                startDestination = Routes.MOVIE_PICKER_WIZARD
            ) {
                composable(Routes.MOVIE_PICKER_WIZARD) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.MOVIE_PICKER)
                    }
                    val sharedVm: MoviePickerViewModel = hiltViewModel(parentEntry)
                    MoviePickerScreen(
                        onBack = { navController.popBackStack(Routes.MOVIE_PICKER, inclusive = true) },
                        onResultReady = { navController.navigate(Routes.MOVIE_PICKER_RESULT) },
                        viewModel = sharedVm
                    )
                }
                composable(Routes.MOVIE_PICKER_RESULT) { backStackEntry ->
                    val parentEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Routes.MOVIE_PICKER)
                    }
                    val sharedVm: MoviePickerViewModel = hiltViewModel(parentEntry)
                    MoviePickerResultScreen(
                        onBack = { navController.popBackStack() },
                        onMovieClick = { movieId -> navController.navigate(Routes.movieDetails(movieId)) },
                        viewModel = sharedVm
                    )
                }
            }

            composable(Routes.MOVIEIT_LENS) {
                MovieItLensScreen(
                    onBack = { navController.popBackStack() },
                    onMovieClick = { movieId -> navController.navigate(Routes.movieDetails(movieId)) }
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
                    }
                )
            }

            composable(
                route = Routes.MOVIE_DETAILS,
                arguments = listOf(navArgument("movieId") { type = NavType.IntType })
            ) { backStackEntry ->
                val movieId = backStackEntry.arguments?.getInt("movieId") ?: return@composable
                val refreshDiaryLog = backStackEntry.savedStateHandle.get<Boolean>("refresh_diary_log") ?: false
                MovieDetailScreen(
                    onBack = {
                        val prev = navController.previousBackStackEntry
                        when (prev?.destination?.route) {
                            Routes.HOME -> prev.savedStateHandle["refresh_home"] = true
                            Routes.MOVIES -> prev.savedStateHandle["refresh_movies"] = true
                            Routes.WATCHLIST -> prev.savedStateHandle["refresh_needed"] = true
                            Routes.PROFILE -> prev.savedStateHandle["refresh_profile"] = true
                            Routes.DIARY -> prev.savedStateHandle["refresh_diary"] = true
                        }
                        navController.popBackStack()
                    },
                    onMovieClick = { id -> navController.navigate(Routes.movieDetails(id)) },
                    onLogToDiary = { navController.navigate(Routes.diaryLog(movieId)) },
                    refreshDiaryLog = refreshDiaryLog,
                    onRefreshDiaryLogHandled = { backStackEntry.savedStateHandle["refresh_diary_log"] = false },
                    onUserClick = { userId ->
                        navController.navigate(Routes.userProfile(userId))
                    },
                    onDirectorClick = { directorId ->
                        navController.navigate(Routes.directorProfile(directorId))
                    }
                )
            }

            composable(
                route = Routes.DIARY_LOG,
                arguments = listOf(
                    navArgument("movieId") { type = NavType.IntType },
                    navArgument("entryId") { type = NavType.IntType }
                )
            ) {
                DiaryLogScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        val prev = navController.previousBackStackEntry
                        when (prev?.destination?.route) {
                            Routes.MOVIE_DETAILS -> prev.savedStateHandle["refresh_diary_log"] = true
                            Routes.DIARY -> prev.savedStateHandle["refresh_diary"] = true
                        }
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Routes.USER_PROFILE,
                arguments = listOf(navArgument("userId") { type = NavType.IntType })
            ) { backStackEntry ->
                val shouldRefresh = backStackEntry.savedStateHandle.get<Boolean>("refresh_user_profile") ?: false
                UserProfileScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToFollowList = { uid, listType ->
                        navController.navigate(Routes.followList(uid, listType))
                    },
                    shouldRefresh = shouldRefresh,
                    onRefreshHandled = { backStackEntry.savedStateHandle["refresh_user_profile"] = false }
                )
            }

            composable(
                route = Routes.DIRECTOR_PROFILE,
                arguments = listOf(navArgument("directorId") { type = NavType.IntType })
            ) {
                DirectorProfileScreen(
                    onBack = { navController.popBackStack() },
                    onMovieClick = { id -> navController.navigate(Routes.movieDetails(id)) }
                )
            }

            composable(
                route = Routes.FOLLOW_LIST,
                arguments = listOf(
                    navArgument("userId") { type = NavType.IntType },
                    navArgument("listType") { type = NavType.StringType }
                )
            ) {
                FollowListScreen(
                    onBack = {
                        val prev = navController.previousBackStackEntry
                        when (prev?.destination?.route) {
                            Routes.PROFILE -> prev.savedStateHandle["refresh_profile"] = true
                            Routes.USER_PROFILE -> prev.savedStateHandle["refresh_user_profile"] = true
                        }
                        navController.popBackStack()
                    },
                    onUserClick = { userId -> navController.navigate(Routes.userProfile(userId)) }
                )
            }

            composable(Routes.WATCHLIST) { backStackEntry ->
                val shouldRefresh = backStackEntry.savedStateHandle.get<Boolean>("refresh_needed") ?: false
                WatchlistScreen(
                    shouldRefresh = shouldRefresh,
                    onRefreshHandled = {
                        backStackEntry.savedStateHandle["refresh_needed"] = false
                    },
                    onMovieClick = { movieId ->
                        navController.navigate(Routes.movieDetails(movieId))
                    }
                )
            }

            composable(Routes.DIARY) { backStackEntry ->
                val shouldRefresh = backStackEntry.savedStateHandle.get<Boolean>("refresh_diary") ?: false
                MyDiaryScreen(
                    onMovieClick = { movieId ->
                        navController.navigate(Routes.movieDetails(movieId))
                    },
                    onEditEntry = { entryId, movieId ->
                        navController.navigate(Routes.diaryLogEdit(movieId, entryId))
                    },
                    shouldRefresh = shouldRefresh,
                    onRefreshHandled = { backStackEntry.savedStateHandle["refresh_diary"] = false }
                )
            }

            composable(Routes.PROFILE) { backStackEntry ->
                val shouldRefresh = backStackEntry.savedStateHandle.get<Boolean>("refresh_profile") ?: false
                ProfileScreen(
                    onOpenDiary = { navController.navigate(Routes.DIARY) },
                    onOpenWatchlist = { navController.navigate(Routes.WATCHLIST) },
                    onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToFollowList = { userId, listType ->
                        navController.navigate(Routes.followList(userId, listType))
                    },
                    shouldRefresh = shouldRefresh,
                    onRefreshHandled = { backStackEntry.savedStateHandle["refresh_profile"] = false }
                )
            }
        }
    }
}

// ── Global Bottom Navigation Bar ──────────────────────────────────────────────

@Composable
private fun AppBottomNavBar(
    currentRoute: String?,
    navController: NavController
) {
    data class NavTab(val icon: ImageVector, val label: String, val route: String)

    val tabs = listOf(
        NavTab(Icons.Default.Home,           "Home",      Routes.HOME),
        NavTab(Icons.Default.Movie,          "Browse",    Routes.MOVIES),
        NavTab(Icons.Default.BookmarkBorder, "Watchlist", Routes.WATCHLIST),
        NavTab(Icons.Default.CalendarMonth,  "Diary",     Routes.DIARY),
        NavTab(Icons.Default.Person,         "Profile",   Routes.PROFILE)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xCC070711))
            .drawBehind {
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent, AccentPurple, GlowPurple, AccentPurple, Color.Transparent
                        )
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.5f
                )
            }
            .navigationBarsPadding()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isActive = when (tab.route) {
                Routes.HOME   -> currentRoute == Routes.HOME
                Routes.MOVIES -> currentRoute == Routes.MOVIES ||
                        currentRoute?.startsWith("movie/") == true
                else          -> currentRoute == tab.route
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (isActive) GlowPurple else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    tab.label,
                    color = if (isActive) GlowPurple else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(4.dp)
                            .background(GlowPurple, androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
        }
    }
}
