package com.app.movieit.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.movieit.data.model.Director
import com.app.movieit.data.model.Movie
import com.app.movieit.ui.theme.*
import com.app.movieit.ui.viewmodel.MoviesViewModel
import java.util.Locale
import kotlin.math.roundToInt

private val DECADES = listOf(
    1930, 1940, 1950, 1960, 1970, 1980, 1990, 2000, 2010, 2020
)

private fun decadeLabel(decade: Int): String = when (decade) {
    1930 -> "30s"; 1940 -> "40s"; 1950 -> "50s"; 1960 -> "60s"
    1970 -> "70s"; 1980 -> "80s"; 1990 -> "90s"
    2000 -> "00s"; 2010 -> "10s"; 2020 -> "20s"
    else -> "${decade % 100}s"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviesScreen(
    onLoggedOut: () -> Unit,
    onMovieClick: (Int) -> Unit,
    viewModel: MoviesViewModel = hiltViewModel(),
    shouldRefresh: Boolean,
    onRefreshHandled: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Draft state resets to current ViewModel state each time the sheet opens
    var draftDecades by remember(showFilters) { mutableStateOf(state.selectedDecades) }
    var draftRating by remember(showFilters) { mutableStateOf(state.minRating ?: 0f) }

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

    // ── Filter bottom sheet ────────────────────────────────────────────────────
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
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Filters", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Divider(color = BorderColor, thickness = 0.5.dp)

                // Decades
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Decade", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                        lazyItems(DECADES) { decade ->
                            SelectableChip(
                                label = decadeLabel(decade),
                                selected = decade in draftDecades,
                                onClick = {
                                    draftDecades = if (decade in draftDecades)
                                        draftDecades - decade else draftDecades + decade
                                }
                            )
                        }
                    }
                }

                // Rating slider
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Minimum Rating", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentPurple.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (draftRating <= 0f) "Any" else "⭐ %.1f".format(draftRating),
                                color = GoldAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Slider(
                        value = draftRating,
                        onValueChange = { draftRating = (it * 2f).roundToInt() / 2f },
                        valueRange = 0f..10f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = GlowPurple,
                            activeTrackColor = AccentPurple,
                            inactiveTrackColor = BorderColor
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0.0", color = TextSecondary.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("5.0", color = TextSecondary.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("10.0", color = TextSecondary.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Reset / Apply
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = {
                            draftDecades = emptySet()
                            draftRating = 0f
                            viewModel.setDecadeAndRatingFilters(emptySet(), null)
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
                    Button(
                        onClick = {
                            viewModel.setDecadeAndRatingFilters(
                                draftDecades,
                                if (draftRating <= 0f) null else draftRating
                            )
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

    Scaffold(
        containerColor = DeepBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)) { append("MOVIE") }
                            withStyle(SpanStyle(color = GoldAccent, fontWeight = FontWeight.Black, letterSpacing = 1.sp)) { append("IT") }
                        },
                        fontSize = 22.sp
                    )
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF110A22), titleContentColor = TextPrimary),
                modifier = Modifier.drawBehind {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(Color(0xFF110A22), DeepBlack, DeepBlack)))
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Search bar ──────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.setSearch(it) },
                        placeholder = { Text("Search movies & directors...", color = TextSecondary, fontSize = 14.sp) },
                        leadingIcon = {
                            IconButton(onClick = {
                                viewModel.submitSearchToGrid(state.searchQuery)
                                keyboardController?.hide()
                            }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Submit search",
                                    tint = if (state.gridSearch != null) AccentPurple else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.clearSearch() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            viewModel.submitSearchToGrid(state.searchQuery)
                            keyboardController?.hide()
                        }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (state.gridSearch != null) AccentPurple else AccentPurple,
                            unfocusedBorderColor = if (state.gridSearch != null) AccentPurple.copy(alpha = 0.6f) else BorderColor,
                            cursorColor = GlowPurple,
                            focusedContainerColor = Color(0x1AAB6DFF),
                            unfocusedContainerColor = Color(0x0DFFFFFF)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                // ── Active director chip ────────────────────────────────────────
                if (state.selectedDirectorId != null) {
                    DirectorFilterChip(
                        name = state.selectedDirectorName ?: "",
                        avatarUrl = state.selectedDirectorAvatar,
                        onClear = { viewModel.clearDirectorFilter() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // ── Filter button + genre chips (single scrollable row) ──────────
                val filterBadge = state.selectedDecades.size + (if (state.minRating != null && state.minRating!! > 0f) 1 else 0)
                LazyRow(
                    contentPadding = PaddingValues(start = 12.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        // Purple Filters button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(AccentPurple)
                                .clickable { showFilters = true }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filters",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (filterBadge > 0) "Filters · $filterBadge" else "Filters",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    lazyItems(state.genres) { genre ->
                        SelectableChip(
                            label = genre.name,
                            selected = genre.id in state.selectedGenreIds,
                            onClick = { viewModel.toggleGenre(genre.id) }
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ── Active filter summary ───────────────────────────────────────
                val hasFilters = state.selectedGenreIds.isNotEmpty() ||
                        state.selectedDecades.isNotEmpty() ||
                        (state.minRating != null && state.minRating!! > 0f)
                if (hasFilters) {
                    ActiveFilterSummary(
                        genreCount = state.selectedGenreIds.size,
                        selectedDecades = state.selectedDecades,
                        minRating = state.minRating,
                        onClearAll = { viewModel.clearAllFilters() }
                    )
                }

                // ── Content ─────────────────────────────────────────────────────
                when {
                    state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator(color = AccentPurple, strokeWidth = 3.dp)
                            Text("Loading...", color = TextSecondary, fontSize = 13.sp)
                        }
                    }

                    state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = ErrorRed.copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
                            Text("Error: ${state.error}", color = ErrorRed, fontSize = 14.sp, textAlign = TextAlign.Center)
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

                    state.movies.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(56.dp))
                            Text("No movies found.", color = TextSecondary, fontSize = 15.sp)
                        }
                    }

                    else -> {
                        // Results header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                "Results",
                                color = TextPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                "${state.movies.size} MOVIES",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(items = state.movies, key = { it.id }) { movie ->
                                MovieGridItem(movie = movie, onClick = { onMovieClick(movie.id) })
                            }
                        }

                        // ── Paginator ───────────────────────────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawLine(
                                        brush = Brush.horizontalGradient(colors = listOf(Color.Transparent, AccentPurple, Color.Transparent)),
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width, 0f),
                                        strokeWidth = 1f
                                    )
                                }
                                .background(Color(0xFF110A22).copy(alpha = 0.8f))
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
                                Text("Page ${state.currentPage + 1}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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

            // ── Search dropdown (floats over content) ──────────────────────────
            if (state.dropdownVisible) {
                val suggestions = state.searchSuggestions
                val hasDirectors = suggestions?.directors?.isNotEmpty() == true
                val hasMovies = suggestions?.movies?.isNotEmpty() == true
                val isEmpty = !state.searchLoading && suggestions != null && !hasDirectors && !hasMovies

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 72.dp)
                        .padding(horizontal = 16.dp)
                        .heightIn(max = 360.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0F2E)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    when {
                        state.searchLoading -> Box(
                            Modifier.fillMaxWidth().padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AccentPurple, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        }

                        isEmpty -> Box(
                            Modifier.fillMaxWidth().padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No results for \"${state.searchQuery}\"", color = TextSecondary, fontSize = 13.sp)
                        }

                        else -> LazyColumn {
                            // Directors section
                            if (hasDirectors) {
                                item {
                                    DropdownSectionHeader("DIRECTORS")
                                }
                                itemsIndexed(suggestions!!.directors) { index, director ->
                                    DirectorSuggestRow(director = director, onClick = {
                                        viewModel.setDirectorFilter(director)
                                        keyboardController?.hide()
                                    })
                                    if (index < suggestions.directors.lastIndex || hasMovies) {
                                        Divider(color = BorderColor, thickness = 0.5.dp)
                                    }
                                }
                            }
                            // Movies section
                            if (hasMovies) {
                                item {
                                    DropdownSectionHeader("MOVIES")
                                }
                                itemsIndexed(suggestions!!.movies) { index, movie ->
                                    SearchDropdownItem(movie = movie, onClick = {
                                        viewModel.clearSearch()
                                        onMovieClick(movie.id)
                                    })
                                    if (index < suggestions.movies.lastIndex) {
                                        Divider(color = BorderColor, thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Dropdown section header ────────────────────────────────────────────────────
@Composable
private fun DropdownSectionHeader(label: String) {
    Text(
        text = label,
        color = GlowPurple,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

// ── Director suggestion row ────────────────────────────────────────────────────
@Composable
private fun DirectorSuggestRow(director: Director, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            if (!director.profileUrl.isNullOrBlank()) {
                AsyncImage(
                    model = director.profileUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = director.name,
                color = AccentPurple,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("Director", color = TextSecondary, fontSize = 11.sp)
        }
    }
}

// ── Active director filter chip ────────────────────────────────────────────────
@Composable
private fun DirectorFilterChip(
    name: String,
    avatarUrl: String?,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AccentPurple.copy(alpha = 0.15f))
            .border(1.dp, AccentPurple, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(AccentPurple.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(14.dp))
            }
        }
        Text(
            text = name,
            color = AccentPurple,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Clear director filter",
            tint = AccentPurple,
            modifier = Modifier
                .size(16.dp)
                .clickable { onClear() }
        )
    }
}

// ── Selectable chip ────────────────────────────────────────────────────────────
@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) AccentPurple else Color.Transparent)
            .border(1.dp, if (selected) AccentPurple else BorderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (selected) TextPrimary else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ── Active filter summary ──────────────────────────────────────────────────────
@Composable
private fun ActiveFilterSummary(
    genreCount: Int,
    selectedDecades: Set<Int>,
    minRating: Float?,
    onClearAll: () -> Unit
) {
    val parts = buildList {
        if (genreCount > 0) add("$genreCount genre${if (genreCount > 1) "s" else ""}")
        if (selectedDecades.isNotEmpty()) add(selectedDecades.sorted().joinToString(", ") { decadeLabel(it) })
        if (minRating != null && minRating > 0f) add("★ ≥ %.1f".format(minRating))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = parts.joinToString(" · "),
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(
            onClick = onClearAll,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text("Clear all", color = AccentPurple, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Search dropdown item ───────────────────────────────────────────────────────
@Composable
private fun SearchDropdownItem(movie: Movie, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 54.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2A2A2A))
        ) {
            if (!movie.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = movie.title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val year = movie.releaseDate?.take(4) ?: ""
            val rating = if (movie.avgRating > 0f) "⭐ %.1f".format(movie.avgRating) else ""
            val meta = listOf(year, rating).filter { it.isNotBlank() }.joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(text = meta, color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

// ── Movie grid item ────────────────────────────────────────────────────────────
@Composable
private fun MovieGridItem(movie: Movie, onClick: () -> Unit) {
    val rating = String.format(Locale.US, "%.1f", movie.avgRating)
    val year = movie.releaseDate?.take(4) ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF1C0F35), Color(0xFF110A22))))
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
                Icon(Icons.Default.Movie, contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
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
                    if (year.isNotBlank()) Text(year, color = TextSecondary, fontSize = 10.sp)
                    if (year.isNotBlank() && movie.avgRating > 0) Text(" · ", color = TextSecondary, fontSize = 10.sp)
                    if (movie.avgRating > 0) Text("⭐$rating", color = GoldAccent, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
