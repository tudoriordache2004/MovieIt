package com.app.movieit.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.movieit.data.model.LensRecommendation
import com.app.movieit.ui.theme.AccentPurple
import com.app.movieit.ui.theme.BorderColor
import com.app.movieit.ui.theme.DeepBlack
import com.app.movieit.ui.theme.ErrorRed
import com.app.movieit.ui.theme.GlowPurple
import com.app.movieit.ui.theme.GoldAccent
import com.app.movieit.ui.theme.TextPrimary
import com.app.movieit.ui.theme.TextSecondary
import com.app.movieit.ui.viewmodel.MovieItLensViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val LENS_MODES = listOf(
    "vibe" to "My Vibe",
    "cover" to "Book Covers",
    "poster" to "Cinema Posters"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MovieItLensScreen(
    onBack: () -> Unit,
    onMovieClick: (Int) -> Unit,
    viewModel: MovieItLensViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    // Camera permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onPermissionResult(granted) }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) viewModel.onPermissionResult(true)
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Camera binding
    LaunchedEffect(state.permissionGranted) {
        if (!state.permissionGranted) return@LaunchedEffect
        val cameraProvider = withContext(Dispatchers.IO) {
            ProcessCameraProvider.getInstance(context).get()
        }
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            viewModel.onCameraError()
        }
    }

    // Gallery picker — auto-analyzes on pick
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setPickedImage(uri)
            scope.launch(Dispatchers.IO) {
                val file = File(context.cacheDir, "lens_gallery.jpg")
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    withContext(Dispatchers.Main) { viewModel.analyze(file) }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { viewModel.onCaptureError() }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Background: camera preview OR picked image ─────────────────────────
        if (state.pickedImageUri != null) {
            AsyncImage(
                model = state.pickedImageUri,
                contentDescription = "Selected image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Permission denied state ────────────────────────────────────────────
        if (state.permissionRequested && !state.permissionGranted) {
            Box(
                modifier = Modifier.fillMaxSize().background(DeepBlack),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Camera access required",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "MovieIt Lens needs camera access to analyze the scene and suggest films.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant permission", color = TextPrimary)
                    }
                }
            }
        }

        // ── Top bar ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
        ) {
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
                        tint = Color.White
                    )
                }
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) {
                            append("MovieIt ")
                        }
                        withStyle(SpanStyle(color = GoldAccent, fontWeight = FontWeight.ExtraBold)) {
                            append("Lens")
                        }
                    },
                    fontSize = 20.sp,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        // ── Bottom overlay: mode selector + shutter row ────────────────────────
        if (state.permissionGranted || state.pickedImageUri != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp, top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Inline error
                if (state.error != null && !state.loading) {
                    Text(
                        text = state.error!!,
                        color = ErrorRed,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Mode selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                ) {
                    LENS_MODES.forEach { (value, label) ->
                        val selected = state.selectedMode == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) AccentPurple else Color.Transparent)
                                .clickable(enabled = !state.loading) { viewModel.selectMode(value) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.White else Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Shutter row: [gallery]  [shutter]  [spacer]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Gallery button (left)
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        GalleryButton(
                            enabled = !state.loading,
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }

                    // Shutter / back-to-camera button (center)
                    if (state.pickedImageUri != null) {
                        // Tap to go back to camera
                        BackToCameraButton(
                            enabled = !state.loading,
                            onClick = { viewModel.clearPickedImage() }
                        )
                    } else {
                        // Standard camera capture
                        ShutterButton(
                            enabled = !state.loading,
                            onClick = {
                                val file = File(context.cacheDir, "lens_capture.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                                imageCapture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                            viewModel.analyze(file)
                                        }
                                        override fun onError(exception: ImageCaptureException) {
                                            viewModel.onCaptureError()
                                        }
                                    }
                                )
                            }
                        )
                    }

                    // Right spacer to keep shutter visually centered
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // ── Loading overlay ────────────────────────────────────────────────────
        if (state.loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    CircularProgressIndicator(
                        color = AccentPurple,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(52.dp)
                    )
                    Text(
                        text = "Analyzing your cinematic vibe...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // ── Result bottom sheet ────────────────────────────────────────────────
        if (state.showSheet && state.result != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissResult() },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                containerColor = Color(0xFF14142A),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 8.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(BorderColor)
                    )
                }
            ) {
                val result = state.result!!
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    item {
                        Text(
                            text = result.title,
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    item {
                        Text(
                            text = result.description,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }

                    if (result.visualLabels.isNotEmpty()) {
                        item {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                result.visualLabels.forEach { label ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(GoldAccent.copy(alpha = 0.14f))
                                            .border(1.dp, GoldAccent.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = label.label,
                                            color = GoldAccent,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (result.matchedGenres.isNotEmpty()) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                result.matchedGenres.take(5).forEach { genre ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(AccentPurple.copy(alpha = 0.2f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            color = GlowPurple,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, AccentPurple, GlowPurple, AccentPurple, Color.Transparent)
                                    )
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${result.recommendations.size} films matched",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    items(result.recommendations) { rec ->
                        LensRecommendationCard(
                            recommendation = rec,
                            onClick = { onMovieClick(rec.movie.id) }
                        )
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ── Shutter button ─────────────────────────────────────────────────────────────

@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .border(3.dp, Color.White.copy(alpha = if (enabled) 0.85f else 0.3f), CircleShape)
            .padding(5.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.92f else 0.3f))
            .clickable(enabled = enabled, onClick = onClick)
    )
}

// ── Back-to-camera button (shown when gallery image is displayed) ──────────────

@Composable
private fun BackToCameraButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .border(3.dp, AccentPurple.copy(alpha = if (enabled) 0.9f else 0.3f), CircleShape)
            .padding(5.dp)
            .clip(CircleShape)
            .background(AccentPurple.copy(alpha = if (enabled) 0.25f else 0.1f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = "Back to camera",
            tint = Color.White.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(28.dp)
        )
    }
}

// ── Gallery picker button ──────────────────────────────────────────────────────

@Composable
private fun GalleryButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .border(1.5.dp, Color.White.copy(alpha = if (enabled) 0.4f else 0.15f), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = "Pick from gallery",
            tint = Color.White.copy(alpha = if (enabled) 0.9f else 0.3f),
            modifier = Modifier.size(26.dp)
        )
    }
}

// ── Recommendation card ────────────────────────────────────────────────────────

@Composable
private fun LensRecommendationCard(
    recommendation: LensRecommendation,
    onClick: () -> Unit
) {
    val movie = recommendation.movie
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1C0F35))
            .clickable { onClick() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(64.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2A2A2A))
        ) {
            if (!movie.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = "${movie.title} poster",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = movie.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (recommendation.reason.isNotBlank()) {
                Text(
                    text = recommendation.reason,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (recommendation.matchedGenres.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    recommendation.matchedGenres.take(3).forEach { genre ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentPurple.copy(alpha = 0.18f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = genre,
                                color = GlowPurple,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            if (recommendation.visualSimilarity > 0f) {
                Text(
                    text = "Visual match: ${(recommendation.visualSimilarity * 100).toInt()}%",
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
