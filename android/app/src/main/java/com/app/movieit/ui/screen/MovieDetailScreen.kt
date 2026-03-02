package com.app.movieit.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.movieit.data.model.ReviewOut
import com.app.movieit.ui.viewmodel.MovieDetailViewModel
import com.app.movieit.ui.viewmodel.ReviewsUiState
import com.app.movieit.ui.viewmodel.ReviewsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    onBack: () -> Unit,
    viewModel: MovieDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val reviewsVm: ReviewsViewModel = hiltViewModel()
    val reviewsState by reviewsVm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { innerPadding ->
        when {
            state.loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.error != null -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: ${state.error}")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.load() }) { Text("Retry") }
                }
            }

            state.movie != null -> {
                val m = state.movie!!
                val year = m.releaseDate?.take(4)
                val rating = String.format(Locale.US, "%.1f", m.avgRating)

                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = m.posterUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .width(120.dp) // Setezi o lățime rezonabilă pentru listă
                            .aspectRatio(2f / 3f) // Menține proporțiile de poster (înălțimea va fi 180.dp)
                            .clip(RoundedCornerShape(8.dp)), // Colțuri rotunjite pentru estetică
                        contentScale = ContentScale.Crop // Taie excesul dar umple frumos containerul
                    )

                    Text(
                        text = m.title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = listOfNotNull(year, "⭐ $rating").joinToString(" • "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val desc = m.description?.trim().orEmpty()
                    if (desc.isNotEmpty()) {
                        Text(desc, style = MaterialTheme.typography.bodyLarge)
                    }

                    val inWatchlist = state.inWatchlist
                    Button(
                        onClick = { viewModel.toggleWatchlist() },
                        enabled = inWatchlist != null && !state.watchlistBusy
                    ) {
                        Text(
                            when (inWatchlist) {
                                null -> "Checking..."
                                true -> "Remove from watchlist"
                                false -> "Add to watchlist"
                            }
                        )
                    }

                    ReviewsSection(
                        state = reviewsState,
                        onRatingChange = { reviewsVm.onRatingChange(it) },
                        onCommentChange = { reviewsVm.onCommentChange(it) },
                        onPost = { reviewsVm.postReview() },
                        onRetry = { reviewsVm.load() }
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewsSection(
    state: ReviewsUiState,
    onRatingChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onPost: () -> Unit,
    onRetry: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Reviews", style = MaterialTheme.typography.titleLarge)

        if (state.error != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Error: ${state.error}", modifier = Modifier.weight(1f))
                Spacer(Modifier.height(0.dp))
                Button(onClick = onRetry) { Text("Retry") }
            }
        }

        // --- Create review form ---
        Text("Your rating: ${state.myRating}/10", style = MaterialTheme.typography.titleMedium)

        Slider(
            value = state.myRating.toFloat(),
            onValueChange = { onRatingChange(it.toInt()) },
            valueRange = 1f..10f,
            steps = 8 // 1..10 => 9 intervale, steps = intervale-1 = 8
        )

        OutlinedTextField(
            value = state.myComment,
            onValueChange = onCommentChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Comment (optional)") },
            minLines = 2,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text
            )
        )

        Button(
            onClick = onPost,
            enabled = !state.posting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.posting) CircularProgressIndicator() else Text("Post review")
        }

        // --- List reviews ---
        if (state.loading) {
            CircularProgressIndicator()
        } else {
            if (state.reviews.isEmpty()) {
                Text("No reviews yet.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.reviews.forEach { review ->
                        ReviewRow(review)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(review: ReviewOut) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("⭐ ${review.rating}/10", style = MaterialTheme.typography.titleMedium)
        val comment = review.comment?.trim().orEmpty()
        if (comment.isNotEmpty()) {
            Text(comment, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = "User #${review.userId} • ${review.createdAt.take(10)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}