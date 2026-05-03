package com.app.movieit.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.movieit.data.model.PublicUserWithFollow
import com.app.movieit.ui.theme.AccentPurple
import com.app.movieit.ui.theme.GlowPurple
import com.app.movieit.ui.theme.TextPrimary
import com.app.movieit.ui.theme.TextSecondary
import com.app.movieit.ui.viewmodel.FollowListViewModel
import com.app.movieit.util.Constants

private val ScreenBg = Color(0xFF0B0B0F)
private val RowBg = Color(0x662A2A38)
private val RowBorder = Color(0x1AFFFFFF)

@Composable
fun FollowListScreen(
    onBack: () -> Unit,
    onUserClick: (Int) -> Unit,
    viewModel: FollowListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val title = if (viewModel.listType == "followers") "Followers" else "Following"

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0)
            lastVisible >= totalItems - 3
        }.collect { nearEnd ->
            if (nearEnd && !state.loading && !state.loadingMore && state.hasMore) {
                viewModel.loadMore()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        when {
            state.loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentPurple)
            }

            state.error != null && state.items.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Could not load list", color = TextPrimary, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.loadMore() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) { Text("Retry", color = TextPrimary) }
                }
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 72.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.items.isEmpty() && !state.loadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (viewModel.listType == "followers") "No followers yet." else "Not following anyone yet.",
                                color = TextSecondary,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                items(state.items, key = { it.id }) { user ->
                    UserRow(
                        user = user,
                        isCurrentUser = user.id == state.currentUserId,
                        isBusy = user.id in state.followBusy,
                        onUserClick = { onUserClick(user.id) },
                        onToggleFollow = { viewModel.toggleFollow(user.id) }
                    )
                }

                if (state.loadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = AccentPurple,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                }
            }
        }

        FollowListTopBar(title = title, onBack = onBack, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun UserRow(
    user: PublicUserWithFollow,
    isCurrentUser: Boolean,
    isBusy: Boolean,
    onUserClick: () -> Unit,
    onToggleFollow: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RowBg)
            .border(1.dp, RowBorder, RoundedCornerShape(12.dp))
            .clickable { onUserClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(AccentPurple, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val pictureUrl = user.profilePictureUrl
                ?.let { Constants.BASE_URL.trimEnd('/') + it }
            if (pictureUrl != null) {
                AsyncImage(
                    model = pictureUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = user.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Username
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.username,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "@${user.username}",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Follow / Unfollow button (hidden for own row)
        if (!isCurrentUser) {
            Spacer(Modifier.width(8.dp))
            if (user.isFollowing) {
                OutlinedButton(
                    onClick = onToggleFollow,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, AccentPurple),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0x33AB6DFF)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 14.dp,
                        vertical = 6.dp
                    )
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            color = GlowPurple,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Following", color = GlowPurple, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Button(
                    onClick = onToggleFollow,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPurple,
                        disabledContainerColor = AccentPurple.copy(alpha = 0.5f)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 14.dp,
                        vertical = 6.dp
                    )
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            color = TextPrimary,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Follow", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowListTopBar(
    title: String,
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
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
