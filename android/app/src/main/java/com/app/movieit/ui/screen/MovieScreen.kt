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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.movieit.data.model.Movie
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
) {
    val state by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }

    // Draft Sheet (se aplica dupa aply)
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

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Filters", style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = yearDraft,
                    onValueChange = { input ->
                        // acceptăm doar cifre și max 4 caractere
                        val filtered = input.filter { it.isDigit() }.take(4)
                        yearDraft = filtered
                    },
                    label = { Text("Year (e.g. 2025)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Min average rating: ${String.format(Locale.US, "%.1f", minRatingDraft)}",
                    style = MaterialTheme.typography.titleMedium
                )

                Slider(
                    value = minRatingDraft,
                    onValueChange = { v ->
                        // pas de 0.5
                        minRatingDraft = (v * 2f).roundToInt() / 2f
                    },
                    valueRange = 0f..10f,
                    steps = 19 // 0..10 în pași de 0.5 => 21 valori => steps=19
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val year = yearDraft.toIntOrNull()
                            val min = if (minRatingDraft <= 0f) null else minRatingDraft
                            viewModel.setYear(year)
                            viewModel.setMinRating(min)
                            showFilters = false
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Apply") }

                    TextButton(
                        onClick = {
                            viewModel.setYear(null)
                            viewModel.setMinRating(null)
                            showFilters = false
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Clear") }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Movies") },
                actions = {
                    TextButton(onClick = { showFilters = true }) { Text("Filters") }
                    TextButton(onClick = onOpenDiary) { Text("Diary") }
                    TextButton(onClick = onOpenWatchlist) { Text("Watchlist") }
                    TextButton(onClick = { viewModel.logout() }) { Text("Logout") }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Search bar (mereu vizibil)
            OutlinedTextField(
                value = state.search,
                onValueChange = { viewModel.setSearch(it) },
                label = { Text("Search movies") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )

            when {
                state.loading -> Box(
                    Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                state.error != null -> Box(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${state.error}")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadMovies(state.currentPage) }) {
                            Text("Retry")
                        }
                    }
                }

                state.movies.isEmpty() -> Box(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No movies found.")
                }

                else -> {
                    // Grid 3 coloane (poster dominant)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = state.movies, key = { it.id }) { movie ->
                            MovieGridItem(
                                movie = movie,
                                onClick = { onMovieClick(movie.id) }
                            )
                        }
                    }

                    // paginator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.prevPage() },
                            enabled = state.currentPage > 0 && !state.loading
                        ) { Text("← Prev") }

                        Text("Page ${state.currentPage + 1}")

                        TextButton(
                            onClick = { viewModel.nextPage() },
                            enabled = state.hasMore && !state.loading
                        ) { Text("Next →") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieGridItem(movie: Movie, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    val rating = String.format(Locale.US, "%.1f", movie.avgRating)
    val year = movie.releaseDate?.take(4) ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(6.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
                Text(
                    "No poster",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = movie.title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = listOfNotNull(year.ifBlank { null }, "⭐$rating").joinToString(" "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}