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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.movieit.data.model.ActivityFeedItem
import com.app.movieit.ui.theme.AccentPurple
import com.app.movieit.ui.theme.BorderColor
import com.app.movieit.ui.theme.DeepBlack
import com.app.movieit.ui.theme.ErrorRed
import com.app.movieit.ui.theme.GlowPurple
import com.app.movieit.ui.theme.GoldAccent
import com.app.movieit.ui.theme.TextPrimary
import com.app.movieit.ui.theme.TextSecondary
import com.app.movieit.ui.viewmodel.ActivityFeedViewModel
import com.app.movieit.util.Constants
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun ActivityFeedScreen(
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onUserClick: (Int) -> Unit,
    viewModel: ActivityFeedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.markAllSeen()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .statusBarsPadding()
    ) {
        // ── Top bar ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Activity",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            )
        }

        HorizontalDivider(color = BorderColor)

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentPurple)
                }
            }

            state.error != null -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error ?: "Unknown error",
                        color = ErrorRed,
                        fontSize = 14.sp
                    )
                }
            }

            state.items.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No activity yet — follow someone or log a movie!",
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.items, key = { it.id }) { item ->
                        ActivityRow(
                            item = item,
                            onMovieClick = onMovieClick,
                            onUserClick = onUserClick,
                        )
                        HorizontalDivider(
                            color = BorderColor,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(
    item: ActivityFeedItem,
    onMovieClick: (Int) -> Unit,
    onUserClick: (Int) -> Unit,
) {
    val isDiaryLog = item.activityType == "diary_log"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isDiaryLog) item.movie?.let { onMovieClick(it.id) }
                else onUserClick(item.user.id)
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // ── Avatar ─────────────────────────────────────────────────────────────
        UserAvatar(
            profilePictureUrl = item.user.profilePictureUrl,
            username = item.user.username,
        )

        // ── Content ────────────────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                // Activity sentence
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = GlowPurple, fontWeight = FontWeight.Bold)) {
                            append(item.user.username)
                        }
                        withStyle(SpanStyle(color = TextSecondary)) {
                            if (isDiaryLog) append(" logged")
                            else append(" started following you")
                        }
                    },
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )

                Spacer(Modifier.width(8.dp))

                // Relative time
                Text(
                    text = relativeTime(item.createdAt),
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
            }

            // Diary-specific: movie info row
            if (isDiaryLog) {
                item.movie?.let { movie ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        // Mini poster
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(72.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentPurple.copy(alpha = 0.18f))
                                .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                        ) {
                            if (movie.posterUrl != null) {
                                AsyncImage(
                                    model = movie.posterUrl,
                                    contentDescription = movie.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.Center),
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = movie.title,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (item.rating != null) {
                                StarDisplay(rating = item.rating)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserAvatar(profilePictureUrl: String?, username: String) {
    val imageUrl = profilePictureUrl?.let {
        if (it.startsWith("http")) it
        else Constants.BASE_URL.trimEnd('/') + it
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(AccentPurple.copy(alpha = 0.22f))
            .border(1.dp, BorderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = username,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun StarDisplay(rating: Int) {
    val fullStars = rating / 2
    val hasHalf = rating % 2 == 1
    val emptyStars = 5 - fullStars - (if (hasHalf) 1 else 0)

    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(fullStars) {
            Text("★", color = GoldAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        if (hasHalf) {
            Text("½", color = GoldAccent, fontSize = 12.sp)
        }
        repeat(emptyStars) {
            Text("★", color = TextSecondary.copy(alpha = 0.3f), fontSize = 14.sp)
        }
    }
}

private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

private fun relativeTime(isoString: String): String {
    return try {
        val dt = LocalDateTime.parse(isoString.take(19), isoFormatter)
        val now = LocalDateTime.now()
        val hours = ChronoUnit.HOURS.between(dt, now)
        val days = ChronoUnit.DAYS.between(dt, now)
        when {
            hours < 1 -> "just now"
            hours < 24 -> "${hours}h"
            days < 7 -> "${days}d"
            else -> dt.format(DateTimeFormatter.ofPattern("d MMM"))
        }
    } catch (_: Exception) {
        ""
    }
}
