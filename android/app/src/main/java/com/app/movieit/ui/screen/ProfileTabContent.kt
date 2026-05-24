package com.app.movieit.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.movieit.data.model.DiaryOut
import com.app.movieit.data.model.ReviewOut
import com.app.movieit.data.model.WatchlistItemWithMovie
import com.app.movieit.ui.theme.BorderColor
import com.app.movieit.ui.theme.GlowPurple
import com.app.movieit.ui.theme.GoldAccent
import com.app.movieit.ui.theme.TextPrimary
import com.app.movieit.ui.theme.TextSecondary

private val ProfileCardBg = Color(0xFF171223)

@Composable
fun ProfileDiaryTabContent(
    entries: List<DiaryOut>,
    emptyMessage: String,
    onMovieClick: (Int) -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    ProfileTabSection(
        isEmpty = entries.isEmpty(),
        emptyIcon = Icons.AutoMirrored.Filled.MenuBook,
        emptyMessage = emptyMessage,
        actionLabel = actionLabel,
        onAction = onAction,
    ) {
        entries.forEach { entry ->
            ProfileActivityCard(
                posterUrl = entry.movie.posterUrl,
                title = entry.movie.title,
                subtitle = "Watched ${entry.watchedOn.take(10)}",
                rating = entry.review?.rating,
                body = entry.review?.comment,
                onClick = { onMovieClick(entry.movie.id) },
            )
        }
    }
}

@Composable
fun ProfileReviewsTabContent(
    reviews: List<ReviewOut>,
    emptyMessage: String,
    onMovieClick: (Int) -> Unit,
) {
    ProfileTabSection(
        isEmpty = reviews.isEmpty(),
        emptyIcon = Icons.Default.RateReview,
        emptyMessage = emptyMessage,
    ) {
        reviews.forEach { review ->
            val movie = review.movie
            ProfileActivityCard(
                posterUrl = movie?.posterUrl,
                title = movie?.title ?: "Movie #${review.movieId}",
                subtitle = "Reviewed ${review.createdAt.take(10)}",
                rating = review.rating,
                body = review.comment,
                onClick = { onMovieClick(movie?.id ?: review.movieId) },
            )
        }
    }
}

@Composable
fun ProfileWatchlistTabContent(
    items: List<WatchlistItemWithMovie>,
    emptyMessage: String,
    onMovieClick: (Int) -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    ProfileTabSection(
        isEmpty = items.isEmpty(),
        emptyIcon = Icons.Default.BookmarkBorder,
        emptyMessage = emptyMessage,
        actionLabel = actionLabel,
        onAction = onAction,
    ) {
        items.forEach { item ->
            ProfileActivityCard(
                posterUrl = item.movie.posterUrl,
                title = item.movie.title,
                subtitle = "Added ${item.addedAt.take(10)}",
                rating = null,
                body = item.movie.description,
                onClick = { onMovieClick(item.movie.id) },
            )
        }
    }
}

@Composable
private fun ProfileTabSection(
    isEmpty: Boolean,
    emptyIcon: ImageVector,
    emptyMessage: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (isEmpty) {
        ProfileEmptyTab(emptyIcon, emptyMessage, actionLabel, onAction)
    } else {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
        if (actionLabel != null && onAction != null) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                OutlinedButton(
                    onClick = onAction,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GlowPurple),
                ) {
                    Text(actionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ProfileActivityCard(
    posterUrl: String?,
    title: String,
    subtitle: String,
    rating: Int?,
    body: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ProfileCardBg)
            .border(1.dp, BorderColor.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(84.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF252033)),
            contentAlignment = Alignment.Center,
        ) {
            if (!posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = "$title poster",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(Icons.Default.Movie, contentDescription = null, tint = TextSecondary.copy(alpha = 0.45f), modifier = Modifier.size(26.dp))
            }
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            if (rating != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                    Text("$rating/10", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            val trimmedBody = body?.trim().orEmpty()
            if (trimmedBody.isNotEmpty()) {
                Text(trimmedBody, color = TextSecondary.copy(alpha = 0.85f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ProfileEmptyTab(
    icon: ImageVector,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
        Text(message, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            OutlinedButton(
                onClick = onAction,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GlowPurple),
            ) {
                Text(actionLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
