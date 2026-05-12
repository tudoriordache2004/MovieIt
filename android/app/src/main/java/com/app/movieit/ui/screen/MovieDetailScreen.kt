package com.app.movieit.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.movieit.data.model.ReviewOut
import com.app.movieit.ui.theme.AccentPurple
import com.app.movieit.ui.theme.BorderColor
import com.app.movieit.ui.theme.DeepBlack
import com.app.movieit.ui.theme.ErrorRed
import com.app.movieit.ui.theme.GlowPurple
import com.app.movieit.ui.theme.GoldAccent
import com.app.movieit.ui.theme.TextPrimary
import com.app.movieit.ui.theme.TextSecondary
import com.app.movieit.ui.viewmodel.MovieDetailViewModel
import com.app.movieit.ui.viewmodel.ReviewsUiState
import com.app.movieit.ui.viewmodel.ReviewsViewModel
import java.util.Locale

@Composable
fun MovieDetailScreen(
    onBack: () -> Unit,
    onUserClick: (Int) -> Unit = {},
    onDirectorClick: (Int) -> Unit = {},
    onLogToDiary: () -> Unit = {},
    refreshDiaryLog: Boolean = false,
    onRefreshDiaryLogHandled: () -> Unit = {},
    viewModel: MovieDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val reviewsVm: ReviewsViewModel = hiltViewModel()
    val reviewsState by reviewsVm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(refreshDiaryLog) {
        if (refreshDiaryLog) {
            viewModel.removeFromWatchlistIfPresent()?.join()
            viewModel.load()
            reviewsVm.load()
            onRefreshDiaryLogHandled()
        }
    }

    LaunchedEffect(reviewsState.autoMarkedSpoiler) {
        if (reviewsState.autoMarkedSpoiler) {
            snackbarHostState.showSnackbar("Your review was automatically marked as potentially containing spoilers")
            reviewsVm.consumeAutoMarkedSpoiler()
        }
    }

    LaunchedEffect(reviewsState.submissionError) {
        reviewsState.submissionError?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            reviewsVm.consumeSubmissionError()
        }
    }

    LaunchedEffect(reviewsState.reviewPosted) {
        if (reviewsState.reviewPosted) {
            viewModel.removeFromWatchlistIfPresent()?.join()
            viewModel.load()
            reviewsVm.consumeReviewPosted()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        when {
            state.loading -> {
                CircularProgressIndicator(
                    color = AccentPurple,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            state.error != null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Error: ${state.error}",
                        color = ErrorRed,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.load() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) { Text("Retry", color = TextPrimary) }
                }
            }

            state.movie != null -> {
                val m = state.movie!!
                val year = m.releaseDate?.take(4) ?: ""
                val avgRating = String.format(Locale.US, "%.1f", m.avgRating)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Space for top bar
                    item { Spacer(Modifier.height(72.dp)) }

                    // Movie header: poster card (left) + info (right)
                    item {
                        MovieHeaderSection(
                            movie = m,
                            year = year,
                            avgRating = avgRating,
                            inWatchlist = state.inWatchlist,
                            watchlistBusy = state.watchlistBusy,
                            onToggleWatchlist = { viewModel.toggleWatchlist() },
                            onLogToDiary = onLogToDiary,
                            onDirectorClick = onDirectorClick
                        )
                    }

                    // Synopsis
                    val desc = m.description?.trim().orEmpty()
                    if (desc.isNotEmpty()) {
                        item { SynopsisSection(description = desc) }
                    }

                    // Reviews
                    item {
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
                            onDelete = { reviewsVm.deleteReview(it) },
                            onSpoilerChange = { reviewsVm.onSpoilerChange(it) },
                            onEditSpoilerChange = { reviewsVm.onEditSpoilerChange(it) },
                            onModerateDeleteReview = { reviewsVm.onModerateDeleteReview(it) },
                            onUserClick = onUserClick
                        )
                    }
                }
            }
        }

        // Sticky top bar (always rendered on top)
        DetailTopBar(
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

    }
}

// ── Top Bar ──────────────────────────────────────────────────────────────────

@Composable
private fun DetailTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xB3070711))
            .drawBehind {
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent, AccentPurple, GlowPurple, AccentPurple, Color.Transparent
                        )
                    ),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.5f
                )
            }
            .statusBarsPadding()
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary
            )
        }
    }
}

// ── Movie Header: Poster Card (left) + Info (right) ──────────────────────────

@Composable
private fun MovieHeaderSection(
    movie: com.app.movieit.data.model.Movie,
    year: String,
    avgRating: String,
    inWatchlist: Boolean?,
    watchlistBusy: Boolean,
    onToggleWatchlist: () -> Unit,
    onLogToDiary: () -> Unit,
    onDirectorClick: (Int) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Poster card
        Box(
            modifier = Modifier
                .width(130.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0x33AB6DFF), RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = "${movie.title} poster",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Subtle gradient at bottom of poster
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(0f to Color.Transparent, 0.7f to Color.Transparent, 1f to Color(0x80070711))
                        )
                    )
            )
        }

        // Info column (right)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (year.isNotBlank()) YearChip(year)

            Text(
                text = movie.title,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    shadow = Shadow(
                        color = GlowPurple.copy(alpha = 0.4f),
                        offset = Offset(0f, 0f),
                        blurRadius = 8f
                    )
                )
            )

            RatingChip(avgRating)

            if (movie.directors.isNotEmpty()) {
                DirectedByRow(directors = movie.directors, onDirectorClick = onDirectorClick)
            }

            Spacer(Modifier.height(4.dp))

            // Watchlist button
            Button(
                onClick = onToggleWatchlist,
                enabled = inWatchlist != null && !watchlistBusy,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPurple,
                    disabledContainerColor = AccentPurple.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = if (inWatchlist == true) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = when (inWatchlist) {
                        null -> "Checking..."
                        true -> "In Watchlist"
                        false -> "Add to Watchlist"
                    },
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Diary button
            OutlinedButton(
                onClick = onLogToDiary,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AccentPurple),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0x33AB6DFF)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = GlowPurple,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "Log to Diary",
                    color = GlowPurple,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DirectedByRow(
    directors: List<com.app.movieit.data.model.Director>,
    onDirectorClick: (Int) -> Unit
) {
    // Each director name is its own clickable Text for independent tap zones
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Directed by ",
            color = TextSecondary,
            fontSize = 12.sp
        )
        directors.forEachIndexed { index, director ->
            if (index > 0) {
                Text(text = ", ", color = TextSecondary, fontSize = 12.sp)
            }
            Text(
                text = director.name,
                color = AccentPurple,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onDirectorClick(director.id) }
            )
        }
    }
}

@Composable
private fun YearChip(year: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x33FFD700))
            .border(1.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(year, color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RatingChip(avgRating: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x661E1E26))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = GoldAccent,
            modifier = Modifier.size(13.dp)
        )
        Text(avgRating, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Synopsis Section ──────────────────────────────────────────────────────────

@Composable
private fun SynopsisSection(description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "SYNOPSIS",
            color = GlowPurple,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            description,
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
    }
}

// ── Reviews Section ───────────────────────────────────────────────────────────

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
    onDelete: (Int) -> Unit,
    onSpoilerChange: (Boolean) -> Unit,
    onEditSpoilerChange: (Boolean) -> Unit,
    onModerateDeleteReview: (Int) -> Unit,
    onUserClick: (Int) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "USER REVIEWS",
            color = GlowPurple,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        if (state.error != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Error: ${state.error}",
                    color = ErrorRed,
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp
                )
                TextButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.textButtonColors(contentColor = GlowPurple)
                ) { Text("Retry") }
            }
        }

        if (state.loading) {
            CircularProgressIndicator(
                color = AccentPurple,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else if (state.reviews.isEmpty()) {
            Text("No reviews yet.", color = TextSecondary, fontSize = 14.sp)
        } else {
            state.reviews.forEach { review ->
                key(review.id) {
                    ReviewCard(
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
                        onDelete = { onDelete(review.id) },
                        editIsSpoiler = state.editIsSpoiler,
                        onEditSpoilerChange = onEditSpoilerChange,
                        currentUserRole = state.currentUserRole,
                        onModerateDeleteReview = { onModerateDeleteReview(review.id) },
                        onUserClick = { if (review.userId != state.currentUserId) onUserClick(review.userId) }
                    )
                }
            }
        }

        WriteReviewCard(
            state = state,
            onRatingChange = onRatingChange,
            onCommentChange = onCommentChange,
            onPost = onPost,
            onSpoilerChange = onSpoilerChange
        )
    }
}

// ── Review Card ───────────────────────────────────────────────────────────────

@Composable
private fun ReviewCard(
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
    onDelete: () -> Unit,
    editIsSpoiler: Boolean,
    onEditSpoilerChange: (Boolean) -> Unit,
    currentUserRole: String?,
    onModerateDeleteReview: () -> Unit,
    onUserClick: () -> Unit = {}
) {
    val isMine = currentUserId != null && review.userId == currentUserId
    val isEditing = editingReviewId == review.id
    val isDeletingThis = deletingReviewId == review.id
    val canModerate = !isMine && (currentUserRole == "mod" || currentUserRole == "admin")

    // Toggle: false = hidden, true = revealed
    var revealed by rememberSaveable(review.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x661E1E26), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header: avatar + user info + date/spoiler badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AccentPurple, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val pictureUrl = review.profilePictureUrl
                    ?.let { com.app.movieit.util.Constants.BASE_URL.trimEnd('/') + it }
                if (pictureUrl != null) {
                    AsyncImage(
                        model = pictureUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = review.username?.firstOrNull()?.uppercaseChar()?.toString() ?: "#",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    review.username ?: "User #${review.userId}",
                    color = if (!isMine) AccentPurple else TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = if (!isMine) Modifier.clickable { onUserClick() } else Modifier
                )
                if (!isEditing && review.rating != null && review.rating > 0) {
                    StarRatingInput(rating = review.rating, onRatingChange = {})
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    review.createdAt.take(10),
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                if (review.isSpoiler) {
                    Spacer(Modifier.height(4.dp))
                    SpoilerBadge()
                }
            }
        }

        // Comment with spoiler toggle (reveal ↔ hide)
        val comment = review.comment?.trim().orEmpty()
        if (!isEditing && comment.isNotEmpty()) {
            if (review.isSpoiler) {
                if (!revealed) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33FF4D6D))
                            .clickable { revealed = true }
                            .padding(10.dp)
                    ) {
                        Text(
                            "Spoiler — tap to reveal",
                            color = ErrorRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(comment, color = TextSecondary, fontSize = 14.sp)
                        TextButton(
                            onClick = { revealed = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary),
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
                        ) {
                            Text("Hide spoiler", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Text(comment, color = TextSecondary, fontSize = 14.sp)
            }
        }

        // Edit form (inline)
        if (isEditing) {
            Text("Edit Rating", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            StarRatingInput(rating = editRating, onRatingChange = onEditRatingChange)

            OutlinedTextField(
                value = editComment,
                onValueChange = onEditCommentChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Edit comment", color = TextSecondary) },
                minLines = 2,
                textStyle = TextStyle(color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = BorderColor,
                    cursorColor = GlowPurple,
                    focusedContainerColor = Color(0x0DFFFFFF),
                    unfocusedContainerColor = Color(0x0AFFFFFF)
                )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = editIsSpoiler,
                    onCheckedChange = onEditSpoilerChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = AccentPurple,
                        uncheckedColor = TextSecondary
                    )
                )
                Text("Contains spoilers", color = TextSecondary, fontSize = 13.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSaveEdit,
                    enabled = !savingEdit,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Text(if (savingEdit) "Saving..." else "Save", color = TextPrimary, fontSize = 13.sp)
                }
                TextButton(
                    onClick = onCancelEdit,
                    enabled = !savingEdit,
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) { Text("Cancel") }
            }
        }

        // Own review: Edit + Delete buttons (always shown when not editing)
        if (isMine && !isEditing) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onEdit,
                    colors = ButtonDefaults.textButtonColors(contentColor = GlowPurple),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) { Text("Edit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }

                TextButton(
                    onClick = onDelete,
                    enabled = !isDeletingThis,
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (isDeletingThis) "Deleting..." else "Delete",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Mod/admin remove (only for other users' reviews)
        if (canModerate && !isEditing) {
            TextButton(
                onClick = onModerateDeleteReview,
                colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) { Text("Remove", fontSize = 13.sp) }
        }
    }
}

@Composable
private fun SpoilerBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x33FF4D6D))
            .border(1.dp, ErrorRed.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text("SPOILER", color = ErrorRed, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

// ── Write Review Card ─────────────────────────────────────────────────────────

@Composable
private fun WriteReviewCard(
    state: ReviewsUiState,
    onRatingChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onPost: () -> Unit,
    onSpoilerChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x661E1E26), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "WRITE A REVIEW",
            color = GlowPurple,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Text("Your Rating", color = TextSecondary, fontSize = 13.sp)
        StarRatingInput(rating = state.myRating, onRatingChange = onRatingChange)

        OutlinedTextField(
            value = state.myComment,
            onValueChange = onCommentChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Your review (optional)", color = TextSecondary) },
            minLines = 3,
            textStyle = TextStyle(color = TextPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentPurple,
                unfocusedBorderColor = BorderColor,
                cursorColor = GlowPurple,
                focusedContainerColor = Color(0x0DFFFFFF),
                unfocusedContainerColor = Color(0x0AFFFFFF)
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text
            )
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.myIsSpoiler,
                onCheckedChange = onSpoilerChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = AccentPurple,
                    uncheckedColor = TextSecondary
                )
            )
            Text("Contains spoilers", color = TextSecondary, fontSize = 13.sp)
        }

        Button(
            onClick = onPost,
            enabled = !state.posting,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentPurple,
                disabledContainerColor = AccentPurple.copy(alpha = 0.5f)
            )
        ) {
            if (state.posting) {
                CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Post Review", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Log Diary Dialog ──────────────────────────────────────────────────────────

@Composable
private fun LogDiaryDialog(
    diaryLogState: com.app.movieit.ui.viewmodel.DiaryLogUiState,
    onDismiss: () -> Unit,
    onCommentChange: (String) -> Unit,
    onRatingChange: (Int) -> Unit,
    onClearRating: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log to diary", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (diaryLogState.error != null) {
                    Text("Error: ${diaryLogState.error}", color = ErrorRed)
                }

                OutlinedTextField(
                    value = diaryLogState.comment,
                    onValueChange = onCommentChange,
                    label = { Text("Comment (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                val r = diaryLogState.rating
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rating", fontWeight = FontWeight.Medium)
                    if (r == null) {
                        Text("(optional)", color = TextSecondary)
                        TextButton(onClick = { onRatingChange(6) }) { Text("Set") }
                    } else {
                        TextButton(onClick = onClearRating) { Text("Clear") }
                    }
                }

                if (r != null) {
                    StarRatingInput(rating = r, onRatingChange = onRatingChange)
                } else {
                    Text("No rating", color = TextSecondary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !diaryLogState.posting,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                if (diaryLogState.posting) {
                    CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save", color = TextPrimary)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !diaryLogState.posting) { Text("Cancel") }
        }
    )
}

// ── Star Rating Input ─────────────────────────────────────────────────────────

@Composable
fun StarRatingInput(
    rating: Int,      // 1..10 (each star = 2 steps: 1=half, 2=full)
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (star in 1..5) {
            val full = rating >= star * 2
            val half = rating == star * 2 - 1

            Box(modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = when {
                        full -> Icons.Filled.Star
                        half -> Icons.AutoMirrored.Filled.StarHalf
                        else -> Icons.Outlined.StarOutline
                    },
                    contentDescription = "$star stars",
                    tint = if (full || half) GoldAccent else TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxSize()
                )
                // Left half → half star
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.5f)
                        .align(Alignment.CenterStart)
                        .clickable { onRatingChange(star * 2 - 1) }
                )
                // Right half → full star
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.5f)
                        .align(Alignment.CenterEnd)
                        .clickable { onRatingChange(star * 2) }
                )
            }
        }

        Spacer(Modifier.width(6.dp))
        if (rating > 0) {
            Text(
                text = "${rating / 2}.${if (rating % 2 == 1) "5" else "0"} / 5",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}
