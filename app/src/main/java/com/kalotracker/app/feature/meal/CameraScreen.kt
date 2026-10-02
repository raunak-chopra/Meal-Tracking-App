package com.kalotracker.app.feature.meal

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.kalotracker.app.core.designsystem.*
import com.kalotracker.app.core.util.ImageUtils
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    viewModel: MealViewModel,
    onClose: () -> Unit,
    onMealSaved: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsState()
    androidx.activity.compose.BackHandler(enabled = state.isSaving) {}

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Shut down the dedicated camera thread when the composable leaves the composition
    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }
    var showFoodSearch by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.analyzeImageFromGallery(context, uri)
        }
    }

    if (state.resumePending) AlertDialog(onDismissRequest = {},
        title = { Text("Resume your meal?") }, text = { Text("Your unfinished photo meal is still here, including its original date and corrections.") },
        confirmButton = { TextButton(onClick = viewModel::resumeDraft) { Text("Resume") } },
        dismissButton = { TextButton(onClick = viewModel::resetScan) { Text("Discard") } })
    if (state.draftLoading) {
        Box(Modifier.fillMaxSize().background(KaloBackground), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // CameraX Viewfinder
        if (!hasCameraPermission) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Camera access is needed to photograph meals. You can still pick a photo from your gallery.",
                    style = KaloTypography.bodyMedium,
                    color = Color.White
                )
                Button(onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) }) {
                    Text("Allow camera")
                }
            }
        }
        if (hasCameraPermission) AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                enabled = !state.isSaving,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            Text(
                text = "FRAME MEAL",
                style = KaloTypography.labelSmall,
                color = Color.White
            )

            Box(modifier = Modifier.size(44.dp))
        }

        // Shutter & Pre-Modifiers Controls at the Bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error banner if any
            if (!state.errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = KaloFat.copy(alpha = 0.92f),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            text = state.errorMessage ?: "",
                            style = KaloTypography.bodyMedium,
                            color = Color.White
                        )
                        if (state.canRetry) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { viewModel.retryAnalysis() }) {
                                    Text("Retry", color = Color.White)
                                }
                                TextButton(onClick = onOpenSettings) {
                                    Text("Settings", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Quick modifier chip
            Surface(
                onClick = { viewModel.toggleAddedOil() },
                shape = RoundedCornerShape(20.dp),
                color = if (state.hasAddedOil) KaloCarbs else Color.Black.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (state.hasAddedOil) KaloCarbs else Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = if (state.hasAddedOil) "✓ Cooked with Oil / Butter (+120 kcal)" else "+ Cooked with Oil / Butter",
                    style = KaloTypography.bodyMedium,
                    color = if (state.hasAddedOil) Color.Black else Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Gallery picker button
                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Pick from Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Tactile Shutter Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (state.isAnalyzing) KaloProtein else Color.White)
                        .clickable(enabled = !state.isAnalyzing) {
                            val capture = imageCapture ?: return@clickable
                            capture.takePicture(
                                cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        coroutineScope.launch {
                                            try {
                                                val processed = ImageUtils.processAndSaveImage(context, image)
                                                viewModel.analyzeCapturedImage(
                                                    processed.compressedBytes,
                                                    processed.localUri
                                                )
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                viewModel.setError("Image processing failed: ${e.message}")
                                            }
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        viewModel.setError("Couldn't take the photo: ${exception.message}")
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isAnalyzing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                }

                // Balance spacer
                Box(modifier = Modifier.size(52.dp))
            }
        }

        if (showFoodSearch) {
            FoodSearchDialog(
                onSelect = { viewModel.addCatalogItem(it) },
                onDismiss = { showFoodSearch = false }
            )
        }

        // Meal Review Sheet when items are detected
        AnimatedVisibility(
            visible = state.items.isNotEmpty() && !state.resumePending,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            MealReviewBottomSheet(
                uiState = state,
                actions = MealReviewActions(
                    onTitleChange = viewModel::setMealTitle,
                    onScaleMeal = viewModel::scaleMeal,
                    onCookingFatChange = viewModel::setCookingFat,
                    onNutritionChange = viewModel::correctNutrition,
                    onNameChange = viewModel::setItemName,
                    onGramsChange = viewModel::setItemGrams,
                    onRemoveItem = viewModel::removeItem,
                    onAddFood = { showFoodSearch = true },
                    onNoteChange = viewModel::setUserNote,
                    onReanalyze = viewModel::retryAnalysis,
                    onToggleOil = viewModel::toggleAddedOil,
                    onTimeChange = viewModel::setTimestamp,
                    onSave = { viewModel.saveMeal(onMealSaved) },
                    onDiscard = { viewModel.resetScan() }
                )
            )
        }
    }
}
