package com.app.movieit.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.movieit.data.model.Movie
import com.app.movieit.data.model.MovieMini
import com.app.movieit.ui.theme.AccentPurple
import com.app.movieit.ui.theme.BorderColor
import com.app.movieit.ui.theme.GlowPurple
import com.app.movieit.ui.theme.TextPrimary
import com.app.movieit.ui.theme.TextSecondary
import com.app.movieit.util.Constants

private val SheetBg = Color(0xFF12121A)
private val FieldBg = Color(0xFF1C1C28)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    editBio: String,
    editTopMovies: List<MovieMini>,
    editSaving: Boolean,
    editError: String?,
    coverUploading: Boolean,
    currentCoverUrl: String?,        // relative URL; will be prefixed with BASE_URL
    searchQuery: String,
    searchResults: List<Movie>,
    searchLoading: Boolean,
    onDismiss: () -> Unit,
    onBioChange: (String) -> Unit,
    onAddMovie: (Movie) -> Unit,
    onRemoveMovieAtIndex: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSave: () -> Unit,
    onChangeCover: () -> Unit,
) {
    var searchActive by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetBg,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Title row
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Edit Profile", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            HorizontalDivider(color = BorderColor)

            // Cover photo row
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cover Photo", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val coverUrl = currentCoverUrl?.let { Constants.BASE_URL.trimEnd('/') + it }
                    Box(
                        Modifier.size(width = 80.dp, height = 48.dp).clip(RoundedCornerShape(8.dp)).background(FieldBg).border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                    ) {
                        if (coverUrl != null) {
                            AsyncImage(model = coverUrl, contentDescription = null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Crop)
                        }
                        if (coverUploading) {
                            CircularProgressIndicator(color = AccentPurple, modifier = Modifier.size(20.dp).align(Alignment.Center), strokeWidth = 2.dp)
                        }
                    }
                    OutlinedButton(
                        onClick = onChangeCover,
                        enabled = !coverUploading,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentPurple),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GlowPurple),
                    ) { Text("Change cover") }
                }
            }

            // Bio field
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Bio", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                OutlinedTextField(
                    value = editBio,
                    onValueChange = onBioChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("A short bio…", color = TextSecondary) },
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FieldBg, unfocusedContainerColor = FieldBg,
                        focusedBorderColor = GlowPurple, unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        cursorColor = GlowPurple,
                    ),
                    shape = RoundedCornerShape(10.dp),
                )
                Text(
                    "${editBio.length}/150",
                    color = if (editBio.length >= 150) AccentPurple else TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.End),
                )
            }

            // Top 4 favorites
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Top 4 Favorites", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 0..3) {
                        val movie = editTopMovies.getOrNull(i)
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(FieldBg)
                                .border(1.dp, if (movie != null) AccentPurple.copy(alpha = 0.5f) else BorderColor, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (movie != null) onRemoveMovieAtIndex(i)
                                    else searchActive = true
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (movie != null) {
                                AsyncImage(model = movie.posterUrl, contentDescription = movie.title, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Crop)
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(2.dp).size(20.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.7f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextPrimary, modifier = Modifier.size(12.dp))
                                }
                            } else {
                                Icon(Icons.Default.Add, contentDescription = "Add movie", tint = TextSecondary, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
                Text("Tap a slot to add · Tap a poster to remove", color = TextSecondary, fontSize = 11.sp)
            }

            // Movie search
            if (searchActive && editTopMovies.size < 4) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search movies…", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = FieldBg, unfocusedContainerColor = FieldBg,
                            focusedBorderColor = GlowPurple, unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            cursorColor = GlowPurple,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = {
                            if (searchLoading) {
                                CircularProgressIndicator(color = AccentPurple, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { onSearchQueryChange(""); searchActive = false }) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary)
                                }
                            }
                        },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        searchResults.forEach { movie ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onAddMovie(movie); searchActive = false }.padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(Modifier.size(width = 32.dp, height = 48.dp).clip(RoundedCornerShape(4.dp)).background(FieldBg)) {
                                    if (movie.posterUrl != null) {
                                        AsyncImage(model = movie.posterUrl, contentDescription = null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Crop)
                                    } else {
                                        Icon(Icons.Default.Movie, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp).align(Alignment.Center))
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(movie.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                    movie.releaseDate?.take(4)?.let { Text(it, color = TextSecondary, fontSize = 12.sp) }
                                }
                            }
                            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            if (editError != null) {
                Text(editError, color = Color(0xFFFF6B6B), fontSize = 13.sp)
            }

            HorizontalDivider(color = BorderColor)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                ) { Text("Cancel") }
                Button(
                    onClick = onSave,
                    enabled = !editSaving,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                ) {
                    if (editSaving) CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Save", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
