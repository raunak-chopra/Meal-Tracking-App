package com.kalotracker.app.feature.barcode

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kalotracker.app.core.designsystem.*
import com.kalotracker.app.core.designsystem.components.KaloButton
import com.kalotracker.app.core.network.ScannedFoodProduct
import com.kalotracker.app.core.util.BarcodeAnalyzer
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerScreen(
    viewModel: BarcodeScannerViewModel,
    onClose: () -> Unit,
    onMealSaved: () -> Unit,
    onProductSelected: ((String, Float, Int, Float, Float, Float) -> Unit)? = null,
    onEnterManually: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsState()

    var camera by remember { mutableStateOf<Camera?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeAnalyzer = remember {
        BarcodeAnalyzer { barcode ->
            viewModel.onBarcodeScanned(barcode)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            barcodeAnalyzer.close()
            cameraExecutor.shutdown()
        }
    }

    // Reactively pause/resume camera analyzer when product popup is shown
    LaunchedEffect(state.product, state.isLookingUp, state.errorMessage) {
        barcodeAnalyzer.setScanningEnabled(
            state.product == null && !state.isLookingUp && state.errorMessage == null
        )
    }

    // Toggle torch on camera instance
    LaunchedEffect(state.isTorchOn, camera) {
        camera?.cameraControl?.enableTorch(state.isTorchOn)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // CameraX Viewfinder
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, barcodeAnalyzer)
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, androidx.core.content.ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Viewfinder Target Frame Overlay
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 280.dp, height = 180.dp)
                        .border(
                            width = 2.dp,
                            color = if (state.product != null) KaloSteps else KaloProtein,
                            shape = RoundedCornerShape(16.dp)
                        )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (state.isLookingUp) "Looking up barcode..." else "Align barcode inside frame",
                        style = KaloTypography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }

        // Top Navigation Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close Button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Torch Toggle Button
                IconButton(
                    onClick = { viewModel.toggleTorch() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = if (state.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Torch",
                        tint = if (state.isTorchOn) KaloCarbs else Color.White
                    )
                }

                // Type Barcode Manually Button
                IconButton(
                    onClick = { viewModel.setManualInputVisible(true) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Enter Manually",
                        tint = Color.White
                    )
                }
            }
        }

        // Loading Overlay
        if (state.isLookingUp) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = KaloProtein)
            }
        }

        // Lookup failure card: never invents a product, offers honest next steps
        AnimatedVisibility(
            visible = state.errorMessage != null && state.product == null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(KaloSurface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (state.reviewLabelUrl != null) "Check this product label" else "Couldn't find this product",
                    style = KaloTypography.titleMedium,
                    color = KaloTextPrimary
                )
                Text(
                    text = state.errorMessage ?: "",
                    style = KaloTypography.bodyMedium,
                    color = KaloTextSecondary
                )
                state.reviewLabelUrl?.let { url ->
                    TextButton(onClick = { uriHandler.openUri(url) }) { Text("Open nutrition label") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.resumeScanning() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Scan again") }
                    if (onEnterManually != null) {
                        Button(
                            onClick = onEnterManually,
                            modifier = Modifier.weight(1f)
                        ) { Text("Enter manually") }
                    }
                }
            }
        }

        // Scanned Product Result Sheet
        AnimatedVisibility(
            visible = state.product != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            state.product?.let { product ->
                ScannedProductCard(
                    product = product,
                    canLog = state.canLog, labelConfirmed = state.labelConfirmed, onConfirmLabel = viewModel::confirmLabel,
                    onOpenLabel = { product.labelUrl?.let { uriHandler.openUri(it) } },
                    timestamp = state.timestamp, onTimeChange = viewModel::setTimestamp,
                    isSaving = state.isSaving,
                    saveError = state.errorMessage,
                    onRefresh = viewModel::refreshProduct,
                    portionGrams = state.portionGrams,
                    calories = state.currentCalories,
                    protein = state.currentProtein,
                    carbs = state.currentCarbs,
                    fat = state.currentFat,
                    onAdjustPortion = { mult -> viewModel.adjustPortion(mult) },
                    onRescan = { viewModel.resumeScanning() },
                    onConfirm = {
                        if (!state.canLog) return@ScannedProductCard
                        if (onProductSelected != null) {
                            onProductSelected(
                                product.name,
                                state.portionGrams,
                                state.currentCalories,
                                state.currentProtein,
                                state.currentCarbs,
                                state.currentFat
                            )
                            onClose()
                        } else {
                            viewModel.logAsMeal(onMealSaved)
                        }
                    }
                )
            }
        }
    }

    // Manual Barcode Input Dialog
    if (state.isManualInputVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.setManualInputVisible(false) },
            containerColor = KaloSurface,
            title = {
                Text(
                    text = "Enter Barcode",
                    style = KaloTypography.titleLarge,
                    color = KaloTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Type the numbers printed beneath the barcode (e.g. 737628064502)",
                        style = KaloTypography.bodyMedium,
                        color = KaloTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.manualBarcodeText,
                        onValueChange = { viewModel.updateManualBarcode(it) },
                        placeholder = { Text("Barcode numbers", color = KaloTextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KaloProtein,
                            unfocusedBorderColor = KaloBorder,
                            focusedTextColor = KaloTextPrimary,
                            unfocusedTextColor = KaloTextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.submitManualBarcode() }) {
                    Text("Lookup", color = KaloProtein, style = KaloTypography.titleMedium)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setManualInputVisible(false) }) {
                    Text("Cancel", color = KaloTextMuted)
                }
            }
        )
    }
}

@Composable
private fun ScannedProductCard(
    product: ScannedFoodProduct,
    canLog: Boolean, labelConfirmed: Boolean, onConfirmLabel: (Boolean) -> Unit, onOpenLabel: () -> Unit,
    timestamp: Long, onTimeChange: (Long) -> Unit,
    isSaving: Boolean,
    saveError: String?,
    onRefresh: () -> Unit,
    portionGrams: Float,
    calories: Int,
    protein: Float,
    carbs: Float,
    fat: Float,
    onAdjustPortion: (Float) -> Unit,
    onRescan: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val maxHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * 0.85f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .navigationBarsPadding()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(KaloSurface)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            com.kalotracker.app.core.designsystem.components.DateTimeChip(timestamp, onTimeChange)
            // Header: Title, Brand, Barcode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = KaloTypography.titleLarge,
                        color = KaloTextPrimary
                    )
                    if (!product.brand.isNullOrBlank()) {
                        Text(
                            text = product.brand,
                            style = KaloTypography.bodyMedium,
                            color = KaloTextSecondary
                        )
                    }
                    Text(
                        text = "Barcode: ${product.barcode}",
                        style = KaloTypography.labelSmall,
                        color = KaloTextMuted
                    )
                }

                // Rescan Icon Button
                IconButton(
                    onClick = onRescan,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(KaloSurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rescan",
                        tint = KaloTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Portion Adjuster
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(KaloSurfaceElevated)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Portion: ${portionGrams.toInt()}g",
                    style = KaloTypography.titleMedium,
                    color = KaloTextPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PortionButton(label = "-10%", onClick = { onAdjustPortion(0.9f) })
                    PortionButton(label = "+10%", onClick = { onAdjustPortion(1.1f) })
                    PortionButton(label = "×2", onClick = { onAdjustPortion(2.0f) })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Macro Pill Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MacroStatPill(
                    label = "CALORIES",
                    value = "$calories kcal",
                    color = KaloCalories,
                    modifier = Modifier.weight(1f)
                )
                MacroStatPill(
                    label = "PROTEIN",
                    value = "${"%.1f".format(protein)}g",
                    color = KaloProtein,
                    modifier = Modifier.weight(1f)
                )
                MacroStatPill(
                    label = "CARBS",
                    value = "${"%.1f".format(carbs)}g",
                    color = KaloCarbs,
                    modifier = Modifier.weight(1f)
                )
                MacroStatPill(
                    label = "FAT",
                    value = "${"%.1f".format(fat)}g",
                    color = KaloFat,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (product.fromCatalog) {
                Text("Indian snack starter catalog · Open Food Facts contributors", style = KaloTypography.labelSmall)
                Text("Check the product and pack size against your packet. Label photos need internet.")
                TextButton(onClick = { uriHandler.openUri("https://openfoodfacts.github.io/documentation/docs/Product-Opener/api/") }) { Text("Data source and licenses") }
            }
            if (product.labelUrl != null) TextButton(onClick = onOpenLabel) { Text("Open nutrition label") }
            if (product.requiresLabelConfirmation) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = labelConfirmed, onCheckedChange = onConfirmLabel)
                    Text("This product and nutrition match my packet", modifier = Modifier.weight(1f))
                }
            }
            saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (product.fromCache) Text("Saved barcode data (works offline).")
            if (!product.fromCatalog) TextButton(onClick = onRefresh, enabled = !isSaving) { Text("Refresh label data") }
            // Log Meal / Confirm Button
            KaloButton(
                text = "Log Food ($calories kcal)",
                onClick = onConfirm,
                loading = isSaving,
                enabled = canLog
            )
        }
    }
}

@Composable
private fun PortionButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(KaloBorder)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = KaloTypography.labelSmall,
            color = KaloTextPrimary
        )
    }
}

@Composable
private fun MacroStatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(KaloSurfaceElevated)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = KaloTypography.labelSmall,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = KaloTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = KaloTextPrimary
        )
    }
}
