package com.app.movieit.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.movieit.ui.theme.AccentPurple
import com.app.movieit.ui.theme.BorderColor
import com.app.movieit.ui.theme.DeepBlack
import com.app.movieit.ui.theme.ErrorRed
import com.app.movieit.ui.theme.GlowPurple
import com.app.movieit.ui.theme.TextPrimary
import com.app.movieit.ui.theme.TextSecondary
import com.app.movieit.ui.viewmodel.MoviePickerViewModel

private val MOODS = listOf(
    "comforting" to "Comforting",
    "thoughtful" to "Thoughtful",
    "romantic" to "Romantic",
    "funny" to "Funny",
    "intense" to "Intense",
    "mind_bending" to "Mind-bending",
    "adventurous" to "Adventurous",
    "inspiring" to "Inspiring",
    "dark" to "Dark",
    "melancholic" to "Melancholic",
    "nostalgic" to "Nostalgic",
    "relaxing" to "Relaxing"
)

private val AVOID_SIGNALS = listOf("Horror", "War", "Violence", "Sad", "Dark")
private val INTENSITIES = listOf("low" to "Low", "medium" to "Medium", "high" to "High")

private val STEP_TITLES = listOf(
    "How are you feeling?",
    "Or describe it",
    "Intensity",
    "Anything to avoid?"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoviePickerScreen(
    onBack: () -> Unit,
    onResultReady: () -> Unit,
    viewModel: MoviePickerViewModel
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.pendingResultNavigation) {
        if (state.pendingResultNavigation) {
            onResultReady()
            viewModel.consumeResultNavigation()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Bar ────────────────────────────────────────────────────────
            TopBar(onBack = onBack)

            // ── Step Indicator ─────────────────────────────────────────────────
            StepIndicator(
                currentStep = state.currentStep,
                maxVisitedStep = state.maxVisitedStep,
                onStepClick = { viewModel.goToStep(it) }
            )

            // ── Heading ────────────────────────────────────────────────────────
            Text(
                text = STEP_TITLES[state.currentStep],
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                lineHeight = 28.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            // ── Body: animated step ────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = state.currentStep,
                    transitionSpec = {
                        val forward = targetState > initialState
                        val enter = slideInHorizontally(
                            animationSpec = tween(280),
                            initialOffsetX = { if (forward) it else -it }
                        ) + fadeIn(animationSpec = tween(280))
                        val exit = slideOutHorizontally(
                            animationSpec = tween(280),
                            targetOffsetX = { if (forward) -it else it }
                        ) + fadeOut(animationSpec = tween(280))
                        enter togetherWith exit
                    },
                    label = "picker-step"
                ) { step ->
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        when (step) {
                            0 -> MoodStep(state.selectedMood) { viewModel.selectMood(it) }
                            1 -> DescribeStep(state.prompt) { viewModel.updatePrompt(it) }
                            2 -> IntensityStep(state.intensity) { viewModel.selectIntensity(it) }
                            3 -> AvoidStep(
                                avoid = state.avoid,
                                selectedMood = state.selectedMood,
                                onToggle = { viewModel.toggleAvoid(it) }
                            )
                        }

                        if (step == MoviePickerViewModel.TOTAL_STEPS - 1) {
                            if (state.validationError != null) {
                                Text(
                                    text = state.validationError!!,
                                    color = ErrorRed,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
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
                            if (state.loading) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(color = AccentPurple, strokeWidth = 3.dp)
                                    Text(
                                        text = "Finding something that fits your mood...",
                                        color = TextSecondary,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // ── Footer buttons ─────────────────────────────────────────────────
            FooterButtons(
                currentStep = state.currentStep,
                canSubmit = state.canSubmit,
                loading = state.loading,
                onSkip = { viewModel.nextStep() },
                onNext = { viewModel.nextStep() },
                onBack = { viewModel.prevStep() },
                onSubmit = { viewModel.findMovie() }
            )
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
// Step composables
// ───────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodStep(selectedMood: String?, onSelect: (String) -> Unit) {
    Text(
        text = "Pick the vibe — or skip if you'd rather describe it.",
        color = TextSecondary,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MOODS.forEach { (value, label) ->
            PickerChip(
                label = label,
                selected = selectedMood == value,
                onClick = { onSelect(value) }
            )
        }
    }
}

@Composable
private fun DescribeStep(prompt: String, onChange: (String) -> Unit) {
    Text(
        text = "Tell us in your own words what you need tonight. Skip if a mood is enough.",
        color = TextSecondary,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
    OutlinedTextField(
        value = prompt,
        onValueChange = onChange,
        placeholder = {
            Text(
                "e.g. a slow-burn thriller with a smart twist",
                color = TextSecondary,
                fontSize = 14.sp
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentPurple,
            unfocusedBorderColor = BorderColor,
            focusedContainerColor = Color(0x1AAB6DFF),
            unfocusedContainerColor = Color(0x0DFFFFFF),
            cursorColor = GlowPurple,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        textStyle = TextStyle(fontSize = 14.sp)
    )
}

@Composable
private fun IntensityStep(intensity: String, onSelect: (String) -> Unit) {
    Text(
        text = "How intense should it feel?",
        color = TextSecondary,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp)),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        INTENSITIES.forEach { (value, label) ->
            val selected = intensity == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) AccentPurple else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) TextPrimary else TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AvoidStep(
    avoid: Set<String>,
    selectedMood: String?,
    onToggle: (String) -> Unit
) {
    Text(
        text = "Anything you'd rather not see tonight?",
        color = TextSecondary,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val blockedAvoids = MoviePickerViewModel.blockedAvoidSignalsForMood(selectedMood)
        AVOID_SIGNALS.forEach { signal ->
            val signalValue = signal.lowercase()
            val enabled = signalValue !in blockedAvoids
            PickerChip(
                label = signal,
                selected = signalValue in avoid,
                enabled = enabled,
                onClick = { onToggle(signalValue) }
            )
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
// Chrome
// ───────────────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(onBack: () -> Unit) {
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
                text = "Pick for Tonight",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

@Composable
private fun StepIndicator(
    currentStep: Int,
    maxVisitedStep: Int,
    onStepClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(MoviePickerViewModel.TOTAL_STEPS) { index ->
            val isCurrent = index == currentStep
            val isVisited = index <= maxVisitedStep
            val pillColor = when {
                isCurrent -> AccentPurple
                isVisited -> AccentPurple.copy(alpha = 0.4f)
                else -> BorderColor
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (isCurrent) 6.dp else 4.dp)
                    .clip(CircleShape)
                    .background(pillColor)
                    .clickable(enabled = isVisited) { onStepClick(index) }
            )
        }
    }
    Text(
        text = "Step ${currentStep + 1} of ${MoviePickerViewModel.TOTAL_STEPS}",
        color = TextSecondary,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp)
    )
}

@Composable
private fun FooterButtons(
    currentStep: Int,
    canSubmit: Boolean,
    loading: Boolean,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    val isLastStep = currentStep == MoviePickerViewModel.TOTAL_STEPS - 1
    val isOptionalStep = currentStep == 0 || currentStep == 1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button (when not on first step)
        if (currentStep > 0) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPurple),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentPurple)
            ) {
                Text("Back", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Skip button (only on optional steps)
        if (isOptionalStep) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                Text("Skip", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Primary action: Next or Submit
        if (isLastStep) {
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .weight(1.4f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                enabled = !loading && canSubmit
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Find my movie",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text(
                    text = "Next",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PickerChip(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    !enabled -> Color.Transparent
                    selected -> AccentPurple
                    else -> Color.Transparent
                }
            )
            .border(
                1.dp,
                when {
                    !enabled -> BorderColor.copy(alpha = 0.35f)
                    selected -> AccentPurple
                    else -> BorderColor
                },
                RoundedCornerShape(20.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = when {
                !enabled -> TextSecondary.copy(alpha = 0.4f)
                selected -> TextPrimary
                else -> TextSecondary
            },
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
