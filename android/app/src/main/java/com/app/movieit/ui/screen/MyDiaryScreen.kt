package com.app.movieit.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.movieit.data.model.DiaryOut
import com.app.movieit.ui.theme.AccentPurple
import com.app.movieit.ui.theme.BorderColor
import com.app.movieit.ui.theme.DeepBlack
import com.app.movieit.ui.theme.ErrorRed
import com.app.movieit.ui.theme.GlowPurple
import com.app.movieit.ui.theme.GoldAccent
import com.app.movieit.ui.theme.TextPrimary
import com.app.movieit.ui.theme.TextSecondary
import com.app.movieit.ui.viewmodel.DiaryViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyDiaryScreen(
    onMovieClick: (Int) -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var confirmDeleteId by remember { mutableStateOf<Int?>(null) }

    // ── Edit dialog ────────────────────────────────────────────────────────────
    if (state.editingEntryId != null) {
        AlertDialog(
            onDismissRequest = { if (!state.saving) viewModel.cancelEdit() },
            containerColor = Color(0xFF1A0F2E),
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Edit diary entry", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.error != null) {
                        Text("Error: ${state.error}", color = ErrorRed, fontSize = 13.sp)
                    }
                    OutlinedTextField(
                        value = state.editWatchedOn,
                        onValueChange = viewModel::onEditWatchedOnChange,
                        label = { Text("Watched on (YYYY-MM-DD)", color = TextSecondary, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = BorderColor,
                            cursorColor = GlowPurple,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    val r = state.editRating
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rating", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        if (r == null) {
                            Text("(optional)", color = TextSecondary, fontSize = 13.sp)
                            TextButton(onClick = { viewModel.onEditRatingChange(6) }) {
                                Text("Set", color = AccentPurple)
                            }
                        } else {
                            TextButton(onClick = { viewModel.clearEditRating() }) {
                                Text("Clear", color = ErrorRed)
                            }
                        }
                    }
                    if (r != null) {
                        StarRatingInput(rating = r, onRatingChange = { viewModel.onEditRatingChange(it) })
                    } else {
                        Text("No rating set", color = TextSecondary, fontSize = 13.sp)
                    }
                    OutlinedTextField(
                        value = state.editComment,
                        onValueChange = viewModel::onEditCommentChange,
                        label = { Text("Comment (optional)", color = TextSecondary, fontSize = 12.sp) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = BorderColor,
                            cursorColor = GlowPurple,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveEdit() },
                    enabled = !state.saving,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    if (state.saving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextPrimary)
                    else Text("Save", color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelEdit() }, enabled = !state.saving) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // ── Delete confirm dialog ──────────────────────────────────────────────────
    if (confirmDeleteId != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            containerColor = Color(0xFF1A0F2E),
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Delete entry?", fontWeight = FontWeight.Bold) },
            text = { Text("This will delete the diary entry and its review.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteEntry(confirmDeleteId!!); confirmDeleteId = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Delete", color = TextPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        containerColor = DeepBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepBlack)
                .padding(innerPadding)
        ) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentPurple)
                }

                state.error != null -> Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Could not load diary.", color = ErrorRed, fontSize = 15.sp)
                        Button(
                            onClick = { viewModel.load() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                        ) { Text("Retry", color = TextPrimary) }
                    }
                }

                else -> {
                    val groups = remember(state.entries) {
                        state.entries
                            .groupBy { it.watchedOn.take(7) }
                            .toSortedMap(compareByDescending { it })
                    }
                    val thisYear = LocalDate.now().year.toString()
                    val totalCount = state.entries.size
                    val thisYearCount = state.entries.count { it.watchedOn.startsWith(thisYear) }

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 100.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // ── Page header ────────────────────────────────────────
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(top = 24.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    "My Diary",
                                    color = TextPrimary,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    DiaryStatCard(label = "TOTAL", value = totalCount, valueColor = AccentPurple)
                                    DiaryStatCard(label = "THIS YEAR", value = thisYearCount, valueColor = GoldAccent)
                                }
                            }
                        }

                        if (state.entries.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No diary entries yet.", color = TextSecondary, fontSize = 15.sp)
                                }
                            }
                        } else {
                            groups.forEach { (monthKey, entries) ->
                                stickyHeader {
                                    MonthSectionHeader(monthKey)
                                }
                                items(entries, key = { it.id }) { entry ->
                                    DiaryCard(
                                        entry = entry,
                                        onClick = { onMovieClick(entry.movie.id) },
                                        onEdit = { viewModel.startEdit(entry) },
                                        onDelete = { confirmDeleteId = entry.id }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Stat card ──────────────────────────────────────────────────────────────────
@Composable
private fun DiaryStatCard(label: String, value: Int, valueColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1030))
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = value.toString(),
                color = valueColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Month section header ───────────────────────────────────────────────────────
@Composable
private fun MonthSectionHeader(monthKey: String) {
    val ym = YearMonth.parse(monthKey)
    val label = "${ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase()} ${ym.year}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepBlack)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0x1AFFFFFF))
        )
    }
}

// ── Diary card ─────────────────────────────────────────────────────────────────
@Composable
private fun DiaryCard(
    entry: DiaryOut,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val movie = entry.movie
    val rating = entry.review?.rating
    val watchedLabel = runCatching {
        val d = LocalDate.parse(entry.watchedOn)
        "Watched ${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${d.year}"
    }.getOrElse { "Watched ${entry.watchedOn}" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1030))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Poster
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(108.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2A2A3A)),
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
                    Icon(
                        Icons.Default.Movie,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Info + menu
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = movie.title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    DiaryRowMenu(onEdit = onEdit, onDelete = onDelete)
                }

                Text(
                    text = watchedLabel,
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                if (rating != null) {
                    StarDisplay(rating = rating)
                }

                val comment = entry.review?.comment?.trim().orEmpty()
                if (comment.isNotEmpty()) {
                    Text(
                        text = comment,
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ── Star display (read-only, 1–10 rating → 5 stars) ───────────────────────────
@Composable
private fun StarDisplay(rating: Int) {
    val fullStars = rating / 2
    val hasHalf = rating % 2 == 1
    val emptyStars = 5 - fullStars - (if (hasHalf) 1 else 0)

    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(fullStars) {
            Text("★", color = GoldAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        if (hasHalf) {
            Text("½", color = GoldAccent, fontSize = 13.sp)
        }
        repeat(emptyStars) {
            Text("★", color = TextSecondary.copy(alpha = 0.3f), fontSize = 15.sp)
        }
    }
}

// ── Star rating input (edit dialog, 1–10 scale shown as 5 stars) ──────────────
@Composable
private fun StarRatingInput(rating: Int, onRatingChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 1..5) {
            val value = i * 2
            val filled = rating >= value
            Text(
                text = if (filled) "★" else "☆",
                color = if (filled) GoldAccent else TextSecondary,
                fontSize = 30.sp,
                modifier = Modifier.clickable { onRatingChange(value) }
            )
        }
    }
}

// ── Dropdown menu ──────────────────────────────────────────────────────────────
@Composable
private fun DiaryRowMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color(0xFF1A0F2E)
        ) {
            DropdownMenuItem(
                text = { Text("Edit", color = TextPrimary, fontSize = 14.sp) },
                onClick = { expanded = false; onEdit() }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = ErrorRed, fontSize = 14.sp) },
                onClick = { expanded = false; onDelete() }
            )
        }
    }
}
