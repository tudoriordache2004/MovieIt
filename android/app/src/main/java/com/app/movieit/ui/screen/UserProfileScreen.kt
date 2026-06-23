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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.app.movieit.ui.theme.GlowPurple
import com.app.movieit.ui.theme.GoldAccent
import com.app.movieit.ui.theme.TextPrimary
import com.app.movieit.ui.theme.TextSecondary
import com.app.movieit.ui.viewmodel.ProfileTab
import com.app.movieit.ui.viewmodel.UserProfileViewModel
import com.app.movieit.util.Constants

private val ScreenBg = Color(0xFF0B0B0F)
private val CardBg = Color(0x662A2A38)

@Composable
fun UserProfileScreen(
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit = {},
    onNavigateToFollowList: (userId: Int, listType: String) -> Unit = { _, _ -> },
    shouldRefresh: Boolean = false,
    onRefreshHandled: () -> Unit = {},
    viewModel: UserProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val coverLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.uploadCover(it, context.contentResolver) }
    }

    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            viewModel.load()
            onRefreshHandled()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg),
    ) {
        when {
            state.loading -> {
                CircularProgressIndicator(
                    color = AccentPurple,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            state.error != null && state.profile == null -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Could not load profile", color = TextPrimary, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.load() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    ) { Text("Retry", color = TextPrimary) }
                }
            }

            state.profile != null -> {
                val profile = state.profile!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    // ── Cover + avatar header ─────────────────────────────────
                    val coverUrl = profile.coverPhotoUrl
                        ?.let { Constants.BASE_URL.trimEnd('/') + it }
                    val avatarUrl = profile.profilePictureUrl
                        ?.let { Constants.BASE_URL.trimEnd('/') + it }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(206.dp),  // 150dp cover + 56dp avatar overflow
                    ) {
                        // Cover background
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .align(Alignment.TopStart),
                        ) {
                            if (coverUrl != null) {
                                AsyncImage(
                                    model = coverUrl,
                                    contentDescription = "Cover photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(AccentPurple.copy(alpha = 0.7f), DeepBlack)
                                            )
                                        )
                                )
                            }
                            // Bottom scrim for readability
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(listOf(Color.Transparent, ScreenBg))
                                    )
                            )
                        }

                        // Avatar circle — overlaps the cover
                        Box(
                            Modifier
                                .align(Alignment.BottomStart)
                                .offset(x = 16.dp)
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(
                                    3.dp,
                                    Brush.sweepGradient(listOf(AccentPurple, GoldAccent, GlowPurple)),
                                    CircleShape,
                                )
                                .background(Color(0xFF1E1E2E)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (avatarUrl != null) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Profile picture",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(46.dp),
                                )
                            }
                        }
                    }

                    // ── Identity block ────────────────────────────────────────
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp),
                    ) {
                        Text(
                            text = profile.username,
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        if (!profile.bio.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = profile.bio,
                                color = TextSecondary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        if (!profile.isMe) {
                            if (profile.isFollowing) {
                                OutlinedButton(
                                    onClick = { viewModel.toggleFollow() },
                                    enabled = !state.followBusy,
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentPurple),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0x33AB6DFF)),
                                ) {
                                    if (state.followBusy) {
                                        CircularProgressIndicator(color = GlowPurple, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text("Following", color = GlowPurple, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.toggleFollow() },
                                    enabled = !state.followBusy,
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentPurple,
                                        disabledContainerColor = AccentPurple.copy(alpha = 0.5f),
                                    ),
                                ) {
                                    if (state.followBusy) {
                                        CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text("Follow", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        } else {
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
                    }

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = BorderColor)

                    // ── Stats row ─────────────────────────────────────────────
                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        ProfileStatCell(
                            value = formatCount(profile.followersCount),
                            label = "Followers",
                            onClick = { onNavigateToFollowList(profile.id, "followers") },
                        )
                        StatDivider()
                        ProfileStatCell(
                            value = formatCount(profile.followingCount),
                            label = "Following",
                            onClick = { onNavigateToFollowList(profile.id, "following") },
                        )
                        StatDivider()
                        ProfileStatCell(
                            value = formatCount(profile.moviesWatchedCount),
                            label = "Watched",
                        )
                        StatDivider()
                        ProfileStatCell(
                            value = formatCount(profile.reviewsCount),
                            label = "Reviews",
                        )
                        StatDivider()
                        // Avg rating with star
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = profile.averageRating?.let { "%.1f".format(it / 2.0) } ?: "—",
                                    color = GlowPurple,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Text("Avg", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }

                    // ── Top 4 Favorites ───────────────────────────────────────
                    Spacer(Modifier.height(24.dp))
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            "Top 4 Favorites",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (i in 0..3) {
                                val movie = profile.topMovies.getOrNull(i)
                                TopMovieSlot(
                                    movie = movie,
                                    onClick = { movie?.let { onMovieClick(it.id) } },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = BorderColor)

                    // ── Tabs ──────────────────────────────────────────────────
                    val tabs = listOf(
                        Triple(ProfileTab.DIARY, "Diary", Icons.AutoMirrored.Filled.MenuBook),
                        Triple(ProfileTab.REVIEWS, "Reviews", Icons.Default.RateReview),
                        Triple(ProfileTab.WATCHLIST, "Watchlist", Icons.Default.BookmarkBorder),
                    )
                    val selectedIndex = tabs.indexOfFirst { it.first == state.selectedTab }

                    TabRow(
                        selectedTabIndex = selectedIndex,
                        containerColor = ScreenBg,
                        contentColor = TextPrimary,
                        indicator = { tabPositions ->
                            Box(
                                Modifier
                                    .tabIndicatorOffset(tabPositions[selectedIndex])
                                    .height(2.dp)
                                    .background(GlowPurple, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)),
                            )
                        },
                        divider = { HorizontalDivider(color = BorderColor) },
                    ) {
                        tabs.forEach { (tab, label, icon) ->
                            val active = state.selectedTab == tab
                            Tab(
                                selected = active,
                                onClick = { viewModel.selectTab(tab) },
                                text = {
                                    Text(
                                        label,
                                        fontSize = 13.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        color = if (active) GlowPurple else TextSecondary,
                                    )
                                },
                                icon = {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = if (active) GlowPurple else TextSecondary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                            )
                        }
                    }

                    when (state.selectedTab) {
                        ProfileTab.DIARY -> ProfileDiaryTabContent(
                            entries = state.diaryEntries,
                            emptyMessage = "No diary entries yet",
                            onMovieClick = onMovieClick,
                        )
                        ProfileTab.REVIEWS -> ProfileReviewsTabContent(
                            reviews = state.reviews,
                            emptyMessage = "No reviews yet",
                            onMovieClick = onMovieClick,
                        )
                        ProfileTab.WATCHLIST -> ProfileWatchlistTabContent(
                            items = state.watchlistItems,
                            emptyMessage = "Watchlist is empty",
                            onMovieClick = onMovieClick,
                        )
                    }

                    Spacer(Modifier.height(88.dp))
                }
            }
        }

        // Floating back button — overlays the cover
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(top = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier
                        .size(22.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        .padding(2.dp),
                )
            }
        }

        // Edit dialog
        if (state.editing && state.profile != null) {
            EditProfileDialog(
                editBio = state.editBio,
                editTopMovies = state.editTopMovies,
                editSaving = state.editSaving,
                editError = state.editError,
                coverUploading = state.coverUploading,
                currentCoverUrl = state.profile!!.coverPhotoUrl,
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
private fun ProfileStatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 4.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    ) {
        Text(value, color = GlowPurple, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label.uppercase(), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun StatDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(32.dp)
            .background(BorderColor),
    )
}

@Composable
private fun TopMovieSlot(
    movie: MovieMini?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(8.dp))
            .background(CardBg)
            .border(
                1.dp,
                if (movie != null) BorderColor else TextSecondary.copy(alpha = 0.25f),
                RoundedCornerShape(8.dp),
            )
            .clickable(enabled = movie != null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (movie != null) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Default.Movie,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.3f),
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000f)
    count >= 1_000 -> "%.1fK".format(count / 1_000f)
    else -> count.toString()
}
