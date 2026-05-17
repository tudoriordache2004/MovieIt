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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.movieit.data.model.DirectorMovie
import com.app.movieit.data.model.DirectorProfile
import com.app.movieit.ui.theme.AccentPurple
import com.app.movieit.ui.theme.LocalAccentPalette
import com.app.movieit.ui.theme.TextPrimary
import com.app.movieit.ui.theme.TextSecondary
import com.app.movieit.ui.theme.rememberAccentPaletteFor
import com.app.movieit.ui.viewmodel.DirectorProfileViewModel

private val DirScreenBg = Color(0xFF0B0B0F)
private val DirCardBg = Color(0x662A2A38)
private val DirCardBorder = Color(0x1AFFFFFF)

@Composable
fun DirectorProfileScreen(
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit,
    onImageClick: (String) -> Unit = {},
    viewModel: DirectorProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val (accent, paletteReady) = rememberAccentPaletteFor(state.profile?.profileUrl)

    CompositionLocalProvider(LocalAccentPalette provides accent) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DirScreenBg)
    ) {
        // Palette-driven gradient behind header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(accent.primary.copy(alpha = 0.20f), Color.Transparent)
                    )
                )
        )

        when {
            state.loading || (state.profile != null && !paletteReady) -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentPurple)
            }

            state.error != null && state.profile == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Could not load director", color = TextPrimary, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.load() },
                        colors = ButtonDefaults.buttonColors(containerColor = accent.primary)
                    ) { Text("Retry", color = TextPrimary) }
                }
            }

            state.profile != null -> {
                DirectorContent(
                    profile = state.profile!!,
                    onMovieClick = onMovieClick,
                    onImageClick = onImageClick
                )
            }
        }

        DirectorTopBar(
            title = state.profile?.name ?: "",
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
    } // CompositionLocalProvider
}

@Composable
private fun DirectorContent(
    profile: DirectorProfile,
    onMovieClick: (Int) -> Unit,
    onImageClick: (String) -> Unit = {}
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Top spacer for the sticky top bar
        item(span = { GridItemSpan(2) }) {
            Spacer(Modifier.height(72.dp))
        }

        // Bio section: portrait + biography text
        item(span = { GridItemSpan(2) }) {
            DirectorBioSection(profile = profile, onImageClick = onImageClick)
        }

        // Details strip: birthday, place of birth, deathday
        val hasDetails = listOf(profile.birthday, profile.placeOfBirth, profile.deathday).any { !it.isNullOrBlank() }
        if (hasDetails) {
            item(span = { GridItemSpan(2) }) {
                DirectorDetailsSection(profile = profile)
            }
        }

        // Filmography header
        item(span = { GridItemSpan(2) }) {
            FilmographyHeader(count = profile.movies.size)
        }

        // Poster grid
        items(profile.movies) { movie ->
            DirectorMoviePoster(
                movie = movie,
                onClick = { onMovieClick(movie.id) }
            )
        }

        // Bottom padding
        item(span = { GridItemSpan(2) }) {
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DirectorBioSection(profile: DirectorProfile, onImageClick: (String) -> Unit = {}) {
    val accent = LocalAccentPalette.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Portrait card (portrait-shaped like a movie poster: 96×144dp)
        val hasPhoto = !profile.profileUrl.isNullOrBlank()
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(144.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, accent.primary.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                .background(accent.container)
                .then(if (hasPhoto) Modifier.clickable { onImageClick(profile.profileUrl!!) } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (hasPhoto) {
                AsyncImage(
                    model = profile.profileUrl,
                    contentDescription = "${profile.name} photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Biography text with expand/collapse
        Column(modifier = Modifier.weight(1f)) {
            val bio = profile.biography?.trim().orEmpty()
            if (bio.isNotEmpty()) {
                Text(
                    text = bio,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    maxLines = if (expanded) Int.MAX_VALUE else 6,
                    overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (expanded) "READ LESS" else "READ MORE",
                    color = accent.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            } else {
                Text("No biography available.", color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun DirectorDetailsSection(profile: DirectorProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "DETAILS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(6.dp))

        if (!profile.birthday.isNullOrBlank()) {
            DetailRow(label = "Born", value = profile.birthday)
        }
        if (!profile.placeOfBirth.isNullOrBlank()) {
            DetailRow(label = "From", value = profile.placeOfBirth)
        }
        if (!profile.deathday.isNullOrBlank()) {
            DetailRow(label = "Died", value = profile.deathday)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(40.dp)
        )
        Text(text = value, color = TextPrimary, fontSize = 12.sp)
    }
}

@Composable
private fun FilmographyHeader(count: Int) {
    val accent = LocalAccentPalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Divider(color = accent.primary.copy(alpha = 0.20f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DIRECTOR OF $count FILMS",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
        Divider(color = accent.primary.copy(alpha = 0.20f))
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun DirectorMoviePoster(
    movie: DirectorMovie,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, DirCardBorder, RoundedCornerShape(10.dp))
            .background(DirCardBg)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = movie.posterUrl,
            contentDescription = "${movie.title} poster",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Dark gradient overlay at the bottom for the title
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.6f to Color.Transparent,
                            1f to Color(0xCC070711)
                        )
                    )
                )
        )
        Text(
            text = movie.title.uppercase(),
            color = TextPrimary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun DirectorTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.50f))
        ) {
            IconButton(onClick = onBack, modifier = Modifier.matchParentSize()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (title.isNotBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.50f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
