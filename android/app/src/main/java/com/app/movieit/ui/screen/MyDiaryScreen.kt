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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

private enum class DiaryViewMode { Calendar, List }

private val YearMonthSaver = Saver<YearMonth, String>(
    save = { it.toString() },
    restore = { YearMonth.parse(it) }
)

private val NullableLocalDateSaver = Saver<LocalDate?, String>(
    save = { it?.toString() ?: "" },
    restore = { if (it.isEmpty()) null else LocalDate.parse(it) }
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyDiaryScreen(
    onMovieClick: (Int) -> Unit,
    onEditEntry: (entryId: Int, movieId: Int) -> Unit = { _, _ -> },
    shouldRefresh: Boolean = false,
    onRefreshHandled: () -> Unit = {},
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var confirmDeleteId by remember { mutableStateOf<Int?>(null) }
    var viewMode by rememberSaveable { mutableStateOf(DiaryViewMode.Calendar) }
    var currentMonth by rememberSaveable(stateSaver = YearMonthSaver) { mutableStateOf(YearMonth.now()) }
    var selectedDay by rememberSaveable(stateSaver = NullableLocalDateSaver) { mutableStateOf<LocalDate?>(null) }
    val entriesByDate = remember(state.entries) {
        state.entries.groupBy { LocalDate.parse(it.watchedOn) }
    }

    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            viewModel.load()
            onRefreshHandled()
        }
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
                .statusBarsPadding()
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
                    val today = LocalDate.now()
                    val totalCount = state.entries.size
                    val thisYearCount = state.entries.count { it.watchedOn.startsWith(today.year.toString()) }

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
                                    .padding(top = 24.dp, bottom = 8.dp),
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

                        // ── View-mode toggle ───────────────────────────────────
                        item {
                            DiaryViewModeToggle(
                                viewMode = viewMode,
                                onModeChange = { viewMode = it; selectedDay = null },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }

                        // ── Calendar or List ───────────────────────────────────
                        if (viewMode == DiaryViewMode.Calendar) {
                            item {
                                DiaryCalendarView(
                                    currentMonth = currentMonth,
                                    entriesByDate = entriesByDate,
                                    selectedDay = selectedDay,
                                    today = today,
                                    onMonthPrev = { currentMonth = currentMonth.minusMonths(1); selectedDay = null },
                                    onMonthNext = { currentMonth = currentMonth.plusMonths(1); selectedDay = null },
                                    onMonthChange = { ym -> currentMonth = ym; selectedDay = null },
                                    onDayClick = { day -> selectedDay = if (selectedDay == day) null else day },
                                    onMovieClick = onMovieClick
                                )
                            }
                        } else {
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
                                groups.forEach { (monthKey, monthEntries) ->
                                    stickyHeader {
                                        MonthSectionHeader(monthKey)
                                    }
                                    items(monthEntries, key = { it.id }) { entry ->
                                        DiaryCard(
                                            entry = entry,
                                            onClick = { onMovieClick(entry.movie.id) },
                                            onEdit = { onEditEntry(entry.id, entry.movie.id) },
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

// ── View-mode toggle ───────────────────────────────────────────────────────────
@Composable
private fun DiaryViewModeToggle(
    viewMode: DiaryViewMode,
    onModeChange: (DiaryViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x331A1030))
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            .padding(3.dp),
    ) {
        DiaryViewMode.entries.forEach { mode ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (viewMode == mode) AccentPurple else Color.Transparent)
                    .clickable { onModeChange(mode) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.name,
                    color = if (viewMode == mode) TextPrimary else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Calendar view ──────────────────────────────────────────────────────────────
@Composable
private fun DiaryCalendarView(
    currentMonth: YearMonth,
    entriesByDate: Map<LocalDate, List<DiaryOut>>,
    selectedDay: LocalDate?,
    today: LocalDate,
    onMonthPrev: () -> Unit,
    onMonthNext: () -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onMovieClick: (Int) -> Unit
) {
    val firstDay = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    // ISO: Monday=1 → column 0, Sunday=7 → column 6
    val leadingBlanks = firstDay.dayOfWeek.value - 1
    val totalCells = ((leadingBlanks + daysInMonth + 6) / 7) * 7
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        MonthYearPickerDialog(
            currentMonth = currentMonth,
            onConfirm = { ym -> onMonthChange(ym); showPicker = false },
            onDismiss = { showPicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        // Month navigation header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMonthPrev) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month", tint = TextSecondary)
            }
            val monthLabel = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()} ${currentMonth.year}"
            Text(
                text = monthLabel,
                color = GlowPurple,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showPicker = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            IconButton(onClick = onMonthNext) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next month", tint = TextSecondary)
            }
        }

        // Weekday row
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Day grid — chunked into weeks
        val cellIndices = (0 until totalCells).toList()
        cellIndices.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                week.forEach { idx ->
                    val dayNum = idx - leadingBlanks + 1
                    if (dayNum < 1 || dayNum > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = currentMonth.atDay(dayNum)
                        DayCell(
                            day = date,
                            entries = entriesByDate[date] ?: emptyList(),
                            isSelected = date == selectedDay,
                            isToday = date == today,
                            onClick = { onDayClick(date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Selected-day entry cards
        val selectedEntries = selectedDay?.let { entriesByDate[it] } ?: emptyList()
        if (selectedDay != null && selectedEntries.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            val dayLabel = selectedDay.let {
                "${it.dayOfMonth} ${it.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${it.year}"
            }
            Text(
                text = dayLabel.uppercase(),
                color = GlowPurple,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                selectedEntries.forEach { entry ->
                    DiaryCalendarEntryCard(
                        entry = entry,
                        onClick = { onMovieClick(entry.movie.id) }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── Day cell ───────────────────────────────────────────────────────────────────
@Composable
private fun DayCell(
    day: LocalDate,
    entries: List<DiaryOut>,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasMovies = entries.isNotEmpty()

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) AccentPurple else Color(0x1AFFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .background(Color(0x331A1030))
            .clickable(onClick = onClick)
    ) {
        if (hasMovies) {
            AsyncImage(
                model = entries.first().movie.posterUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Scrim so date number stays legible
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x33000000), Color(0xBB000000))
                        )
                    )
            )
        }

        // Date number + today underline in top-start corner
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                color = if (hasMovies) TextPrimary else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            if (isToday) {
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .height(2.dp)
                        .background(GoldAccent, RoundedCornerShape(1.dp))
                )
            }
        }

        // +N badge for multiple entries
        if (entries.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AccentPurple.copy(alpha = 0.90f))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "+${entries.size - 1}",
                    color = TextPrimary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Calendar selected-day entry card ──────────────────────────────────────────
@Composable
private fun DiaryCalendarEntryCard(
    entry: DiaryOut,
    onClick: () -> Unit
) {
    val movie = entry.movie
    val rating = entry.review?.rating
    val comment = entry.review?.comment?.trim().orEmpty()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
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
                    .width(80.dp)
                    .height(120.dp)
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
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Info column — no line limits, full content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = movie.title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 21.sp
                )
                if (rating != null) {
                    StarDisplay(rating = rating)
                }
                if (comment.isNotEmpty()) {
                    Text(
                        text = comment,
                        color = TextSecondary.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

// ── Month / year picker dialog ─────────────────────────────────────────────────
@Composable
private fun MonthYearPickerDialog(
    currentMonth: YearMonth,
    onConfirm: (YearMonth) -> Unit,
    onDismiss: () -> Unit
) {
    var pickerYear by remember { mutableStateOf(currentMonth.year) }
    val monthNames = (1..12).map { m ->
        YearMonth.of(pickerYear, m).month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1030),
        titleContentColor = TextPrimary,
        title = {
            // Year navigation row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { pickerYear-- }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous year", tint = TextSecondary)
                }
                Text(
                    text = pickerYear.toString(),
                    color = GlowPurple,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { pickerYear++ }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next year", tint = TextSecondary)
                }
            }
        },
        text = {
            // 3 × 4 month grid — tapping a month confirms immediately
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (0 until 4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (0 until 3).forEach { col ->
                            val monthNum = row * 3 + col + 1
                            val isSelected = pickerYear == currentMonth.year && monthNum == currentMonth.monthValue
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AccentPurple else Color(0x331A1030))
                                    .border(1.dp, if (isSelected) AccentPurple else Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                                    .clickable { onConfirm(YearMonth.of(pickerYear, monthNum)) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = monthNames[monthNum - 1],
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
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
