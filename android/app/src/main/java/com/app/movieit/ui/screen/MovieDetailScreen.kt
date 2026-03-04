package com.app.movieit.ui.screen

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.app.movieit.ui.viewmodel.DiaryLogViewModel
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
    val diaryLogVm: DiaryLogViewModel = hiltViewModel()
    val diaryLogState by diaryLogVm.uiState.collectAsState()
    var showLogDialog by remember { mutableStateOf(false) }
    val reviewsState by reviewsVm.uiState.collectAsState()

    LaunchedEffect(reviewsState.reviewPosted) {
        if (reviewsState.reviewPosted) {
            viewModel.load()
            reviewsVm.consumeReviewPosted()
        }
    }

    LaunchedEffect(diaryLogState.logged) {
        if (diaryLogState.logged) {
            showLogDialog = false
            diaryLogVm.consumeLogged()

            // refresh details (pentru avg_rating la diary)
            viewModel.load()
            reviewsVm.load()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { innerPadding ->
        when {
            state.loading -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.error != null -> Box(
                Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
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
                val year = m.releaseDate?.take(4) ?: ""
                val avgRating = String.format(Locale.US, "%.1f", m.avgRating)

                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --- HEADER: poster stânga + info dreapta ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Poster
                        AsyncImage(
                            model = m.posterUrl,
                            contentDescription = "${m.title} poster",
                            modifier = Modifier
                                .width(140.dp)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )

                        // Info dreapta
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = m.title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (year.isNotBlank()) {
                                Text(
                                    text = year,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "⭐ $avgRating / 10",
                                style = MaterialTheme.typography.titleMedium
                            )

                            // Buton watchlist
                            val inWatchlist = state.inWatchlist
                            Button(
                                onClick = { viewModel.toggleWatchlist() },
                                enabled = inWatchlist != null && !state.watchlistBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    when (inWatchlist) {
                                        null -> "Checking..."
                                        true -> "Remove from watchlist"
                                        false -> "Add to watchlist"
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = { showLogDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Log to diary")
                            }
                        }
                    }

                    // --- Descriere ---
                    val desc = m.description?.trim().orEmpty()
                    if (desc.isNotEmpty()) {
                        Text(desc, style = MaterialTheme.typography.bodyMedium)
                    }

                    Divider()

                    // --- Reviews ---
                    ReviewsSection(
                        state = reviewsState,
                        onRatingChange = { reviewsVm.onRatingChange(it) },
                        onCommentChange = { reviewsVm.onCommentChange(it) },
                        onPost = { reviewsVm.postReview() },
                        onRetry = { reviewsVm.load() },
                        onStartEdit = { reviewsVm.startEdit(it) },
                        onCancelEdit = { reviewsVm.cancelEdit() },
                        onEditRatingChange = { reviewsVm.onEditRatingChange(it) },
                        onEditCommentChange = { reviewsVm.onEditCommentChange(it) },
                        onSaveEdit = { reviewsVm.saveEdit() },
                        onDelete = { reviewsVm.deleteReview(it) }
                    )
                }
                if (showLogDialog) {
                    AlertDialog(
                        onDismissRequest = { showLogDialog = false },
                        title = { Text("Log to diary") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (diaryLogState.error != null) {
                                    Text("Error: ${diaryLogState.error}")
                                }

                                OutlinedTextField(
                                    value = diaryLogState.watchedOn,
                                    onValueChange = { diaryLogVm.onWatchedOnChange(it) },
                                    label = { Text("Watched on (YYYY-MM-DD)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text("Your rating", style = MaterialTheme.typography.titleMedium)
                                StarRatingInput(
                                    rating = diaryLogState.rating,
                                    onRatingChange = { diaryLogVm.onRatingChange(it) }
                                )

                                OutlinedTextField(
                                    value = diaryLogState.comment,
                                    onValueChange = { diaryLogVm.onCommentChange(it) },
                                    label = { Text("Comment (optional)") },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { diaryLogVm.logToDiary() },
                                enabled = !diaryLogState.posting
                            ) {
                                if (diaryLogState.posting) CircularProgressIndicator() else Text("Save")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showLogDialog = false },
                                enabled = !diaryLogState.posting
                            ) { Text("Cancel") }
                        }
                    )
                }

            }

        }
    }
}

// ── Star rating (5 stele, click pe stânga/dreapta = half/full) ──────────────

@Composable
fun StarRatingInput(
    rating: Int,      // 1..10 (fiecare stea = 2 pași: 1=half, 2=full)
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (star in 1..5) {
            val full = rating >= star * 2
            val half = !full && rating >= star * 2 - 1

            // Fiecare stea e împărțită în două zone de click (half / full)
            Box(modifier = Modifier.size(36.dp)) {
                // Icon de bază (goală sau plină)
                Icon(
                    imageVector = if (full) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "$star stars",
                    tint = if (full || half) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxSize()
                )

                // Click stânga (half star) → valoare star*2-1
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.5f)
                        .align(Alignment.CenterStart)
                        .clickable { onRatingChange(star * 2 - 1) }
                )

                // Click dreapta (full star) → valoare star*2
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.5f)
                        .align(Alignment.CenterEnd)
                        .clickable { onRatingChange(star * 2) }
                )
            }
        }

        Spacer(Modifier.width(8.dp))
        Text(
            text = "${rating / 2}.${if (rating % 2 == 1) "5" else "0"} / 5",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

// ── Reviews section ──────────────────────────────────────────────────────────

@Composable
fun ReviewsSection(
    state: ReviewsUiState,
    onRatingChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onPost: () -> Unit,
    onRetry: () -> Unit,
    onStartEdit: (ReviewOut) -> Unit,
    onCancelEdit: () -> Unit,
    onEditRatingChange: (Int) -> Unit,
    onEditCommentChange: (String) -> Unit,
    onSaveEdit: () -> Unit,
    onDelete: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Reviews", style = MaterialTheme.typography.titleLarge)

        if (state.error != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Error: ${state.error}", modifier = Modifier.weight(1f))
                Button(onClick = onRetry) { Text("Retry") }
            }
        }

        // Form post review
        Text("Your rating", style = MaterialTheme.typography.titleMedium)

        StarRatingInput(
            rating = state.myRating,
            onRatingChange = onRatingChange
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

        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

        // Lista reviews
        if (state.loading) {
            CircularProgressIndicator()
        } else if (state.reviews.isEmpty()) {
            Text("No reviews yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.reviews.forEach { review ->
                    ReviewRow(
                        review = review,
                        currentUserId = state.currentUserId,
                        editingReviewId = state.editingReviewId,
                        editRating = state.editRating,
                        editComment = state.editComment,
                        savingEdit = state.savingEdit,
                        deletingReviewId = state.deletingReviewId,
                        onEdit = { onStartEdit(review) },
                        onCancelEdit = onCancelEdit,
                        onEditRatingChange = onEditRatingChange,
                        onEditCommentChange = onEditCommentChange,
                        onSaveEdit = onSaveEdit,
                        onDelete = { onDelete(review.id) }
                    )
                    Divider()
                }
            }
        }
    }
}

// ── Review row ───────────────────────────────────────────────────────────────

@Composable
private fun ReviewRow(
    review: ReviewOut,
    currentUserId: Int?,
    editingReviewId: Int?,
    editRating: Int,
    editComment: String,
    savingEdit: Boolean,
    deletingReviewId: Int?,
    onEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onEditRatingChange: (Int) -> Unit,
    onEditCommentChange: (String) -> Unit,
    onSaveEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isMine = currentUserId != null && review.userId == currentUserId
    val isEditing = editingReviewId == review.id
    val isDeletingThis = deletingReviewId == review.id

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Star display (read-only)
        if (!isEditing) {
            StarRatingInput(rating = review.rating, onRatingChange = {})
        }

        val comment = review.comment?.trim().orEmpty()
        if (!isEditing && comment.isNotEmpty()) {
            Text(comment, style = MaterialTheme.typography.bodyMedium)
        }

        Text(
            text = "User #${review.userId} • ${review.createdAt.take(10)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isMine) {
            if (!isEditing) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onEdit) { Text("Edit") }
                    TextButton(
                        onClick = onDelete,
                        enabled = !isDeletingThis
                    ) { Text(if (isDeletingThis) "Deleting..." else "Delete") }
                }
            } else {
                Text("Edit rating", style = MaterialTheme.typography.titleMedium)

                StarRatingInput(
                    rating = editRating,
                    onRatingChange = onEditRatingChange
                )

                OutlinedTextField(
                    value = editComment,
                    onValueChange = onEditCommentChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Edit comment") },
                    minLines = 2
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onSaveEdit,
                        enabled = !savingEdit
                    ) { Text(if (savingEdit) "Saving..." else "Save") }

                    TextButton(
                        onClick = onCancelEdit,
                        enabled = !savingEdit
                    ) { Text("Cancel") }
                }
            }
        }
    }
}