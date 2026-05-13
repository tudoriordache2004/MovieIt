package com.app.movieit.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.movieit.ui.theme.AccentPurple
import com.app.movieit.ui.theme.DeepBlack
import com.app.movieit.ui.theme.ErrorRed
import com.app.movieit.ui.theme.GlowPurple
import com.app.movieit.ui.theme.GoldAccent
import com.app.movieit.ui.theme.TextPrimary
import com.app.movieit.ui.theme.TextSecondary
import com.app.movieit.ui.viewmodel.MoviePickerViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoviePickerResultScreen(
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit,
    viewModel: MoviePickerViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val pick = state.result

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Bar ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF110A22))
                    .statusBarsPadding()
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
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
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
                        text = "Your Pick",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            // ── Body ───────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.loading && pick == null) {
                    LoadingState()
                } else if (pick == null) {
                    Text(
                        text = state.error ?: "Nothing to show yet.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF14142A))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0x33AB6DFF), RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E1E2A))
                            ) {
                                AsyncImage(
                                    model = pick.movie.posterUrl,
                                    contentDescription = "${pick.movie.title} poster",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colorStops = arrayOf(
                                                    0f to Color.Transparent,
                                                    0.7f to Color.Transparent,
                                                    1f to Color(0x80070711)
                                                )
                                            )
                                        )
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val year = pick.movie.releaseDate?.take(4).orEmpty()
                                if (year.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AccentPurple.copy(alpha = 0.18f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(year, color = GlowPurple, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Text(
                                    text = pick.movie.title,
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.3).sp,
                                    lineHeight = 22.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (pick.movie.avgRating > 0f) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(13.dp))
                                        Text("%.1f".format(pick.movie.avgRating / 2f), color = GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (pick.matchedGenres.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        pick.matchedGenres.forEach { genre ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(AccentPurple.copy(alpha = 0.2f))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(genre, color = GlowPurple, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (pick.reason.isNotBlank()) {
                            Text(
                                text = pick.reason,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                        }

                        if (pick.avoidedSignals.isNotEmpty()) {
                            Text(
                                text = "Avoided: ${pick.avoidedSignals.joinToString(", ")}",
                                color = TextSecondary.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onMovieClick(pick.movie.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPurple),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentPurple)
                            ) {
                                Text("View details", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = { viewModel.anotherPick() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                enabled = pick.hasMore && !state.loading
                            ) {
                                Text(
                                    text = if (pick.hasMore) "Another pick" else "No more picks",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (state.loading) {
                        LoadingState()
                    }

                    if (state.error != null && !state.loading) {
                        Text(
                            text = state.error!!,
                            color = ErrorRed,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(color = AccentPurple, strokeWidth = 3.dp)
        Text(
            text = "Finding your next pick...",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}
