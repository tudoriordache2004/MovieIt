package com.app.movieit.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.movieit.data.model.Movie
import com.app.movieit.ui.theme.*
import com.app.movieit.ui.viewmodel.MoviesViewModel
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen(
    onLoggedOut: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onOpenWatchlist: () -> Unit,
    onOpenDiary: () -> Unit,
    viewModel: MoviesViewModel = hiltViewModel(),
    shouldRefresh: Boolean,
    onRefreshHandled: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }

    var yearDraft by remember(showFilters) { mutableStateOf(state.year?.toString().orEmpty()) }
    var minRatingDraft by remember(showFilters) { mutableStateOf(state.minRating ?: 0f) }

    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) {
            onLoggedOut()
            viewModel.consumeLoggedOut()
        }
    }

    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            viewModel.loadMovies(state.currentPage)
            onRefreshHandled()
        }
    }

    // ─── Filter Bottom Sheet ──────────────────────────────────────────────────
    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF140B26),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(BorderColor)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Filter",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Divider(color = BorderColor, thickness = 0.5.dp)

                // Year field
                OutlinedTextField(
                    value = yearDraft,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }.take(4)
                        yearDraft = filtered
                    },
                    label = { Text("Year of release (ex. 2025)", color = TextSecondary, fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = TextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = BorderColor,
                        cursorColor = GlowPurple,
                        focusedContainerColor = Color(0x1AFFFFFF),
                        unfocusedContainerColor = Color(0x0DFFFFFF)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Rating slider
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Minimum rating",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentPurple.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "⭐ ${String.format(Locale.US, "%.1f", minRatingDraft)}",
                                color = GoldAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Slider(
                        value = minRatingDraft,
                        onValueChange = { v ->
                            minRatingDraft = (v * 2f).roundToInt() / 2f
                        },
                        valueRange = 0f..10f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = GlowPurple,
                            activeTrackColor = AccentPurple,
                            inactiveTrackColor = BorderColor
                        )
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Clear
                    TextButton(
                        onClick = {
                            viewModel.setYear(null)
                            viewModel.setMinRating(null)
                            showFilters = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1AFF4D6D)),
                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                    ) {
                        Text("Reset", fontWeight = FontWeight.SemiBold)
                    }

                    // Apply
                    Button(
                        onClick = {
                            val year = yearDraft.toIntOrNull()
                            val min = if (minRatingDraft <= 0f) null else minRatingDraft
                            viewModel.setYear(year)
                            viewModel.setMinRating(min)
                            showFilters = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Apply", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ─── Main Scaffold ────────────────────────────────────────────────────────
    Scaffold(
        containerColor = DeepBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)) {
                                append("MOVIE")
                            }
                            withStyle(SpanStyle(color = GoldAccent, fontWeight = FontWeight.Black, letterSpacing = 1.sp)) {
                                append("IT")
                            }
                        },
                        fontSize = 22.sp
                    )
                },
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filters", tint = if (state.year != null || state.minRating != null) GoldAccent else TextSecondary)
                    }
                    IconButton(onClick = onOpenDiary) {
                        Icon(Icons.Default.MenuBook, contentDescription = "Diary", tint = TextSecondary)
                    }
                    IconButton(onClick = onOpenWatchlist) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = "Watchlist", tint = TextSecondary)
                    }
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = TextSecondary)
                    }
                    TextButton(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed.copy(alpha = 0.8f))
                    ) {
                        Text("Exit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkNavy,
                    titleContentColor = TextPrimary
                ),
                modifier = Modifier.drawBehind {
                    // bottom glow line
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, AccentPurple, GlowPurple, AccentPurple, Color.Transparent)
                        ),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.5f
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DarkNavy, DeepBlack, DeepBlack)
                    )
                )
                .padding(innerPadding)
        ) {
            // ─── Search Bar ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = state.search,
                    onValueChange = { viewModel.setSearch(it) },
                    placeholder = { Text("Search...", color = TextSecondary, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = BorderColor,
                        cursorColor = GlowPurple,
                        focusedContainerColor = Color(0x1AAB6DFF),
                        unfocusedContainerColor = Color(0x0DFFFFFF)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Active filter chips
            if (state.year != null || (state.minRating != null && state.minRating!! > 0f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.year?.let {
                        FilterChip(label = "📅 $it")
                    }
                    state.minRating?.let {
                        if (it > 0f) FilterChip(label = "⭐ ≥ ${String.format(Locale.US, "%.1f", it)}")
                    }
                }
            }

            // ─── Content ──────────────────────────────────────────────────────
            when {
                state.loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(color = AccentPurple, strokeWidth = 3.dp)
                        Text("Loading...", color = TextSecondary, fontSize = 13.sp)
                    }
                }

                state.error != null -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = ErrorRed.copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
                        Text(
                            "Error: ${state.error}",
                            color = ErrorRed,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.loadMovies(state.currentPage) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Try again.", color = TextPrimary)
                        }
                    }
                }

                state.movies.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(56.dp))
                        Text("No movies found.", color = TextSecondary, fontSize = 15.sp)
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items = state.movies, key = { it.id }) { movie ->
                            MovieGridItem(
                                movie = movie,
                                onClick = { onMovieClick(movie.id) }
                            )
                        }
                    }

                    // ─── Paginator ────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, AccentPurple, Color.Transparent)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 1f
                                )
                            }
                            .background(DarkNavy.copy(alpha = 0.8f))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.prevPage() },
                            enabled = state.currentPage > 0 && !state.loading,
                            colors = ButtonDefaults.textButtonColors(contentColor = GlowPurple)
                        ) { Text("← Previous", fontWeight = FontWeight.SemiBold) }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1AFFFFFF))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "Page ${state.currentPage + 1}",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        TextButton(
                            onClick = { viewModel.nextPage() },
                            enabled = state.hasMore && !state.loading,
                            colors = ButtonDefaults.textButtonColors(contentColor = GlowPurple)
                        ) { Text("Next →", fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }
}

// ─── Filter Chip ──────────────────────────────────────────────────────────────
@Composable
private fun FilterChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(AccentPurple.copy(alpha = 0.3f), MidPurple.copy(alpha = 0.3f))
                )
            )
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(label, color = SoftLavender, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── Movie Grid Item ──────────────────────────────────────────────────────────
@Composable
private fun MovieGridItem(movie: Movie, onClick: () -> Unit) {
    val rating = String.format(Locale.US, "%.1f", movie.avgRating)
    val year = movie.releaseDate?.take(4) ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1C0F35), Color(0xFF110A22))
                )
            )
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            if (!movie.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = "${movie.title} poster",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Movie,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = movie.title,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (year.isNotBlank() || movie.avgRating > 0) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (year.isNotBlank()) {
                        Text(year, color = TextSecondary, fontSize = 10.sp)
                    }
                    if (year.isNotBlank() && movie.avgRating > 0) {
                        Text(" · ", color = TextSecondary, fontSize = 10.sp)
                    }
                    if (movie.avgRating > 0) {
                        Text(
                            "⭐$rating",
                            color = GoldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

