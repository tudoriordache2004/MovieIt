package com.app.movieit.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.movieit.data.model.DiaryOut
import com.app.movieit.ui.viewmodel.DiaryViewModel
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDiaryScreen(
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var confirmDeleteId by remember { mutableStateOf<Int?>(null) }

    // Edit dialog
    if (state.editingEntryId != null) {
        AlertDialog(
            onDismissRequest = { if (!state.saving) viewModel.cancelEdit() },
            title = { Text("Edit diary entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.error != null) Text("Error: ${state.error}")

                    OutlinedTextField(
                        value = state.editWatchedOn,
                        onValueChange = viewModel::onEditWatchedOnChange,
                        label = { Text("Watched on (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Rating optional
                    val r = state.editRating
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rating", style = MaterialTheme.typography.titleMedium)

                        if (r == null) {
                            Text("(optional)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { viewModel.onEditRatingChange(6) }) { Text("Set") } // 3★
                        } else {
                            TextButton(onClick = { viewModel.clearEditRating() }) { Text("Clear") }
                        }
                    }

                    if (r != null) {
                        StarRatingInput(
                            rating = r,
                            onRatingChange = { viewModel.onEditRatingChange(it) }
                        )
                    } else {
                        Text("No rating", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    OutlinedTextField(
                        value = state.editComment,
                        onValueChange = viewModel::onEditCommentChange,
                        label = { Text("Comment (optional)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.saveEdit() }, enabled = !state.saving) {
                    if (state.saving) CircularProgressIndicator() else Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelEdit() }, enabled = !state.saving) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirm dialog
    if (confirmDeleteId != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text("Delete entry?") },
            text = { Text("This will delete the diary entry (and its review if present).") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEntry(confirmDeleteId!!)
                        confirmDeleteId = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Diary") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
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

            state.entries.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { Text("No diary entries yet.") }

            else -> {
                val groups = remember(state.entries) {
                    state.entries
                        .groupBy { it.watchedOn.take(7) } // YYYY-MM
                        .toSortedMap(compareByDescending { it })
                }

                DiaryGroupedList(
                    groups = groups,
                    modifier = Modifier.padding(innerPadding),
                    onMovieClick = onMovieClick,
                    onEdit = { viewModel.startEdit(it) },
                    onDelete = { id -> confirmDeleteId = id }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiaryGroupedList(
    groups: Map<String, List<DiaryOut>>,
    modifier: Modifier = Modifier,
    onMovieClick: (Int) -> Unit,
    onEdit: (DiaryOut) -> Unit,
    onDelete: (Int) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groups.forEach { (month, entries) ->
            stickyHeader { MonthHeader(month = month) }

            items(entries, key = { it.id }) { entry ->
                DiaryRow(
                    entry = entry,
                    onClick = { onMovieClick(entry.movie.id) },
                    onEdit = { onEdit(entry) },
                    onDelete = { onDelete(entry.id) }
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(month: String) {
    val ym = YearMonth.parse(month)
    val label = "${ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase()} ${ym.year}"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DiaryRow(
    entry: DiaryOut,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val day = entry.watchedOn.takeLast(2)
    val title = entry.movie.title
    val posterUrl = entry.movie.posterUrl
    val rating = entry.review?.rating
    val comment = entry.review?.comment?.trim().orEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = day,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )

        Box(
            modifier = Modifier
                .width(44.dp)
                .aspectRatio(2f / 3f)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = "$title poster",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    "No\nposter",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // title + menu
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                DiaryRowMenu(onEdit = onEdit, onDelete = onDelete)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (rating != null) {
                    StarRatingDisplay(rating = rating)
                }
                Text(
                    text = entry.watchedOn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (comment.isNotEmpty()) {
                Text(
                    text = comment,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DiaryRowMenu(
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = { expanded = false; onEdit() }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = { expanded = false; onDelete() }
            )
        }
    }
}

@Composable
private fun StarRatingDisplay(rating: Int) {
    val stars = rating / 2
    val half = rating % 2 == 1
    val text = buildString {
        append(stars)
        if (half) append(".5")
        append("★")
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}