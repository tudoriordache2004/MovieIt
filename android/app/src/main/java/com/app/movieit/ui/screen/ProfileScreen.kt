package com.app.movieit.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.movieit.data.model.MovieMini
import com.app.movieit.ui.theme.AccentPurple
import com.app.movieit.ui.theme.BorderColor
import com.app.movieit.ui.theme.DeepBlack
import com.app.movieit.ui.theme.ErrorRed
import com.app.movieit.ui.theme.GlowPurple
import com.app.movieit.ui.theme.GoldAccent
import com.app.movieit.ui.theme.TextPrimary
import com.app.movieit.ui.theme.TextSecondary
import com.app.movieit.ui.viewmodel.ProfileViewModel
import com.app.movieit.util.Constants

private val ScreenBg = Color(0xFF0B0B0F)
private val CardBg = Color(0x662A2A38)

// Tabs for the own-profile view
private enum class OwnProfileTab { DIARY, REVIEWS, WATCHLIST }

@Composable
fun ProfileScreen(
    onOpenDiary: () -> Unit,
    onOpenWatchlist: () -> Unit,
    onMovieClick: (Int) -> Unit = {},
    onLogout: () -> Unit,
    onNavigateToFollowList: (userId: Int, listType: String) -> Unit = { _, _ -> },
    shouldRefresh: Boolean = false,
    onRefreshHandled: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) { viewModel.load(); onRefreshHandled() }
    }

    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.uploadProfilePicture(it, context) }
    }
    val coverLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.uploadCover(it, context) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg),
    ) {
        when {
            state.loading -> CircularProgressIndicator(color = AccentPurple, modifier = Modifier.align(Alignment.Center))

            state.error != null -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Something went wrong", color = TextPrimary, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.load() }, colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)) {
                    Text("Retry", color = TextPrimary)
                }
            }

            else -> {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

                    // ── Cover + avatar header ─────────────────────────────────
                    val coverUrl = state.coverPhotoUrl?.let { Constants.BASE_URL.trimEnd('/') + it }
                    val avatarUrl = state.profilePictureUrl?.let { Constants.BASE_URL.trimEnd('/') + it }

                    Box(Modifier.fillMaxWidth().height(206.dp)) {
                        // Cover
                        Box(Modifier.fillMaxWidth().height(150.dp).align(Alignment.TopStart)) {
                            if (coverUrl != null) {
                                AsyncImage(model = coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(AccentPurple.copy(alpha = 0.7f), DeepBlack))))
                            }
                            // Scrim
                            Box(Modifier.fillMaxWidth().height(80.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, ScreenBg))))
                            // Camera icon to change cover
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 12.dp, bottom = 8.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable { coverLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (state.coverUploading) {
                                    CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Camera, contentDescription = "Change cover", tint = TextPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Avatar — overlaps cover at bottom-start
                        Box(
                            Modifier
                                .align(Alignment.BottomStart)
                                .offset(x = 16.dp)
                                .size(96.dp),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(3.dp, Brush.sweepGradient(listOf(AccentPurple, GoldAccent, GlowPurple)), CircleShape)
                                    .background(Color(0xFF1E1E2E)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (avatarUrl != null) {
                                    AsyncImage(model = avatarUrl, contentDescription = "Profile picture", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(46.dp))
                                }
                            }
                            // Edit avatar badge
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(AccentPurple)
                                    .border(2.dp, ScreenBg, CircleShape)
                                    .clickable { avatarLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (state.uploading) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                                } else {
                                    Icon(Icons.Default.Edit, contentDescription = "Change avatar", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // ── Identity block ────────────────────────────────────────
                    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
                    // ... inside your Identity block ...
                        Text(state.username ?: "", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)

                        val currentBio = state.bio // Capture it locally
                        if (!currentBio.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(currentBio, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { viewModel.openEdit() },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit Profile", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = BorderColor)

                    // ── Stats row ─────────────────────────────────────────────
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        OwnStatCell(formatCount(state.followersCount), "Followers", onClick = state.userId?.let { id -> { onNavigateToFollowList(id, "followers") } })
                        OwnStatDivider()
                        OwnStatCell(formatCount(state.followingCount), "Following", onClick = state.userId?.let { id -> { onNavigateToFollowList(id, "following") } })
                        OwnStatDivider()
                        OwnStatCell(formatCount(state.diaryCount), "Watched")
                        OwnStatDivider()
                        OwnStatCell(formatCount(state.reviewsCount), "Reviews")
                        OwnStatDivider()
                        // Avg rating
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                                Text(state.averageRating?.let { "%.1f".format(it) } ?: "—", color = GlowPurple, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Avg", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }

                    // ── Top 4 Favorites ───────────────────────────────────────
                    Spacer(Modifier.height(24.dp))
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text("Top 4 Favorites", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (i in 0..3) {
                                OwnTopMovieSlot(movie = state.topMovies.getOrNull(i), modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = BorderColor)

                    // ── Tabs ──────────────────────────────────────────────────
                    val tabs = listOf(
                        Triple(OwnProfileTab.DIARY, "Diary", Icons.AutoMirrored.Filled.MenuBook),
                        Triple(OwnProfileTab.REVIEWS, "Reviews", Icons.Default.RateReview),
                        Triple(OwnProfileTab.WATCHLIST, "Watchlist", Icons.Default.BookmarkBorder),
                    )

                    // Simple local tab state stored in the VM would be ideal; use a local remember for now
                    val selectedTabIndex = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
                    val selected by selectedTabIndex

                    TabRow(
                        selectedTabIndex = selected,
                        containerColor = ScreenBg,
                        contentColor = TextPrimary,
                        indicator = { tabPositions ->
                            Box(Modifier.tabIndicatorOffset(tabPositions[selected]).height(2.dp).background(GlowPurple, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
                        },
                        divider = { HorizontalDivider(color = BorderColor) },
                    ) {
                        tabs.forEachIndexed { index, (_, label, icon) ->
                            val active = selected == index
                            Tab(
                                selected = active,
                                onClick = { selectedTabIndex.intValue = index },
                                text = { Text(label, fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, color = if (active) GlowPurple else TextSecondary) },
                                icon = { Icon(icon, contentDescription = null, tint = if (active) GlowPurple else TextSecondary, modifier = Modifier.size(18.dp)) },
                            )
                        }
                    }

                    when (tabs[selected].first) {
                        OwnProfileTab.DIARY -> ProfileDiaryTabContent(
                            entries = state.diaryEntries,
                            emptyMessage = "Your diary entries appear here",
                            actionLabel = "Open Diary",
                            onAction = onOpenDiary,
                            onMovieClick = onMovieClick,
                        )
                        OwnProfileTab.REVIEWS -> ProfileReviewsTabContent(
                            reviews = state.reviews,
                            emptyMessage = "Your reviews appear here",
                            onMovieClick = onMovieClick,
                        )
                        OwnProfileTab.WATCHLIST -> ProfileWatchlistTabContent(
                            items = state.watchlistItems,
                            emptyMessage = "Your watchlist appears here",
                            actionLabel = "Open Watchlist",
                            onAction = onOpenWatchlist,
                            onMovieClick = onMovieClick,
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = BorderColor)

                    // ── Logout ────────────────────────────────────────────────
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(14.dp)).clickable { onLogout() },
                        color = ErrorRed.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(ErrorRed.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Text("Logout", color = ErrorRed, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(88.dp))
                }
            }
        }

        // Edit dialog
        if (state.editing) {
            EditProfileDialog(
                editBio = state.editBio,
                editTopMovies = state.editTopMovies,
                editSaving = state.editSaving,
                editError = state.editError,
                coverUploading = state.coverUploading,
                currentCoverUrl = state.coverPhotoUrl,
                searchQuery = state.searchQuery,
                searchResults = state.searchResults,
                searchLoading = state.searchLoading,
                onDismiss = { viewModel.closeEdit() },
                onBioChange = { viewModel.updateBio(it) },
                onAddMovie = { viewModel.addTopMovie(it) },
                onRemoveMovieAtIndex = { viewModel.removeTopMovieAtIndex(it) },
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onSave = { viewModel.saveProfile() },
                onChangeCover = { coverLauncher.launch("image/*") },
            )
        }
    }
}

// ── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun OwnStatCell(value: String, label: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    ) {
        Text(value, color = GlowPurple, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label.uppercase(), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun OwnStatDivider() {
    Box(Modifier.width(1.dp).height(32.dp).background(BorderColor))
}

@Composable
private fun OwnTopMovieSlot(movie: MovieMini?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(8.dp))
            .background(CardBg)
            .border(1.dp, if (movie != null) BorderColor else TextSecondary.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (movie != null) {
            AsyncImage(model = movie.posterUrl, contentDescription = movie.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Default.Movie, contentDescription = null, tint = TextSecondary.copy(alpha = 0.3f), modifier = Modifier.size(28.dp))
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000f)
    count >= 1_000 -> "%.1fK".format(count / 1_000f)
    else -> count.toString()
}
