package com.hajun.kaloriku.ui.screen

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.hajun.kaloriku.R
import com.hajun.kaloriku.ui.BarcodeState
import com.hajun.kaloriku.ui.MainViewModel
import com.hajun.kaloriku.ui.components.AppCard
import com.hajun.kaloriku.ui.components.IconBadge
import com.hajun.kaloriku.ui.components.KaloriTopBar
import com.hajun.kaloriku.ui.components.PrimaryButton
import com.hajun.kaloriku.ui.components.SecondaryButton
import com.hajun.kaloriku.ui.theme.Green500
import com.hajun.kaloriku.ui.theme.Spacing
import com.hajun.kaloriku.util.formatDecimal
import com.hajun.kaloriku.util.formatWhole
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Memindai barcode kemasan lalu mencari data gizinya di Open Food Facts. */
@Composable
fun BarcodeScreen(
    viewModel: MainViewModel,
    onResult: () -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) { context.barcodeActivity() }
    val state = viewModel.barcodeState
    var cameraError by remember { mutableStateOf<String?>(null) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    var hasPermission by remember(context) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var showPermissionRationale by remember(activity) {
        mutableStateOf(
            activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } == true
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionDenied = !granted
        showPermissionRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } == true
    }

    // Settings may change permission while this screen is stopped. Only read system state.
    DisposableEffect(context, lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                showPermissionRationale = activity?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
                } == true
                if (hasPermission) permissionDenied = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        KaloriTopBar(title = "Scan barcode", subtitle = "Data gizi dari Open Food Facts", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 320.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state is BarcodeState.Loading -> LoadingBox()

                    state is BarcodeState.Success -> SuccessBox(
                        name = state.item.name,
                        calories = state.item.calories,
                        grams = state.item.grams,
                        protein = state.item.proteinG,
                        carbs = state.item.carbsG,
                        fat = state.item.fatG,
                        onUse = {
                            viewModel.addBarcodeResult(state.item)
                            onResult()
                        }
                    )

                    state is BarcodeState.Error -> ErrorBox(
                        message = state.message,
                        onRetry = {
                            cameraError = null
                            viewModel.resetBarcode()
                        },
                        onSearch = onSearch
                    )

                    !hasPermission -> PermissionPrompt(
                        denied = permissionDenied || showPermissionRationale,
                        permanentlyDenied = permissionDenied && !showPermissionRationale && activity != null,
                        onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        onSettings = { context.openBarcodeSettings() },
                        onSearch = onSearch
                    )

                    cameraError != null -> ErrorBox(
                        message = cameraError.orEmpty(),
                        onRetry = { cameraError = null },
                        onSearch = onSearch,
                        retryText = "Coba kamera lagi"
                    )

                    else -> BarcodeCamera(
                        onBarcode = viewModel::lookupBarcode,
                        onError = { cameraError = it }
                    )
                }
            }

            if (state is BarcodeState.Idle && hasPermission && cameraError == null) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    spacing = 8.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            icon = R.drawable.ic_barcode_scanner,
                            tint = MaterialTheme.colorScheme.primary,
                            size = 40.dp,
                            iconSize = 20.dp
                        )
                        Text(
                            text = "Arahkan kamera ke barcode di kemasan. Kotak barcode biasanya ada di bagian belakang.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = Spacing.md)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionPrompt(
    denied: Boolean,
    permanentlyDenied: Boolean,
    onRequest: () -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconBadge(
            icon = R.drawable.ic_photo_camera,
            tint = Color.White,
            container = Color.White.copy(alpha = 0.14f),
            size = 64.dp,
            iconSize = 30.dp,
            shape = CircleShape
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "Butuh izin kamera",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = when {
                permanentlyDenied -> "Izin kamera dinonaktifkan. Aktifkan izin Kamera di Pengaturan aplikasi, atau gunakan Cari Makanan."
                denied -> "Izin kamera belum diberikan. Izin ini hanya untuk memindai barcode; kamu juga bisa memakai Cari Makanan tanpa kamera."
                else -> "Izin ini hanya dipakai untuk memindai barcode kemasan."
            },
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        PrimaryButton(
            text = if (permanentlyDenied) "Buka pengaturan" else "Izinkan kamera",
            onClick = if (permanentlyDenied) onSettings else onRequest,
            modifier = Modifier.fillMaxWidth()
        )
        if (denied && !permanentlyDenied) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            SecondaryButton(text = "Buka pengaturan", onClick = onSettings, modifier = Modifier.fillMaxWidth())
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        SecondaryButton(text = "Cari Makanan", onClick = onSearch, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun LoadingBox() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Green500)
        Spacer(modifier = Modifier.height(Spacing.md))
        Text("Mencari data produk…", color = Color.White, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SuccessBox(
    name: String,
    calories: Double,
    grams: Double,
    protein: Double,
    carbs: Double,
    fat: Double,
    onUse: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconBadge(
            icon = R.drawable.ic_check_circle,
            tint = Color.White,
            container = Green500.copy(alpha = 0.25f),
            size = 52.dp,
            iconSize = 28.dp,
            shape = CircleShape
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = "${calories.formatWhole()} kkal",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = "per sajian ${grams.formatWhole()} g",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "P ${protein.formatDecimal()} g · K ${carbs.formatDecimal()} g · L ${fat.formatDecimal()} g",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        PrimaryButton(
            text = "Pakai data ini",
            onClick = onUse,
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.White,
            contentColor = com.hajun.kaloriku.ui.theme.Ink900
        )
    }
}

@Composable
private fun ErrorBox(
    message: String,
    onRetry: () -> Unit,
    onSearch: () -> Unit,
    retryText: String = "Scan lagi"
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconBadge(
            icon = R.drawable.ic_error,
            tint = MaterialTheme.colorScheme.error,
            container = Color.White.copy(alpha = 0.12f),
            size = 56.dp,
            iconSize = 28.dp,
            shape = CircleShape
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        SecondaryButton(text = retryText, onClick = onRetry, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(Spacing.sm))
        SecondaryButton(text = "Cari Makanan", onClick = onSearch, modifier = Modifier.fillMaxWidth())
    }
}

/** One ML Kit request at a time, with resources scoped to this camera session. */
@androidx.annotation.OptIn(markerClass = [ExperimentalGetImage::class])
@Composable
private fun BarcodeCamera(onBarcode: (String) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember(context) { PreviewView(context) }
    val currentOnBarcode by rememberUpdatedState(onBarcode)
    val currentOnError by rememberUpdatedState(onError)

    DisposableEffect(context, lifecycleOwner, previewView) {
        val disposed = AtomicBoolean(false)
        val inFlight = AtomicBoolean(false)
        val delivered = AtomicBoolean(false)
        val scannerClosed = AtomicBoolean(false)
        val executor = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        // Completion must still release the image after the analyzer executor shuts down.
        val completionExecutor = Executor { command -> command.run() }
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .build()
        val scanner = BarcodeScanning.getClient(options)
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        var cameraProvider: ProcessCameraProvider? = null

        fun closeScanner() {
            if (scannerClosed.compareAndSet(false, true)) scanner.close()
        }

        fun reportError(message: String) {
            delivered.set(true)
            mainExecutor.execute {
                if (!disposed.get()) currentOnError(message)
            }
        }

        analysis.setAnalyzer(executor) { proxy ->
            if (disposed.get() || delivered.get() || !inFlight.compareAndSet(false, true)) {
                proxy.close()
                return@setAnalyzer
            }
            val imageClosed = AtomicBoolean(false)
            fun finishImage() {
                if (imageClosed.compareAndSet(false, true)) {
                    try {
                        proxy.close()
                    } finally {
                        inFlight.set(false)
                        if (disposed.get() && !inFlight.get()) closeScanner()
                    }
                }
            }

            // Disposal can race with acquiring the in-flight slot. Never use a closed client.
            if (disposed.get() || !lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                finishImage()
                return@setAnalyzer
            }
            try {
                val mediaImage = proxy.image
                if (mediaImage == null) {
                    finishImage()
                    return@setAnalyzer
                }
                val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                scanner.process(image).addOnCompleteListener(completionExecutor) { task ->
                    var value: String? = null
                    var failed = false
                    try {
                        if (task.isSuccessful) {
                            value = task.result.firstNotNullOfOrNull { barcode ->
                                barcode.rawValue?.takeIf { it.isNotBlank() }
                            }
                            if (value != null && !delivered.compareAndSet(false, true)) value = null
                        } else {
                            failed = true
                            delivered.set(true)
                        }
                    } finally {
                        finishImage()
                    }
                    if (!disposed.get()) {
                        val detectedValue = value
                        if (failed) {
                            reportError("Barcode belum bisa dipindai. Coba lagi atau gunakan Cari Makanan.")
                        } else if (detectedValue != null) {
                            mainExecutor.execute {
                                if (!disposed.get()) {
                                    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                                        currentOnBarcode(detectedValue)
                                    } else {
                                        // The app paused before delivery; allow scanning again on resume.
                                        delivered.set(false)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                finishImage()
                reportError("Barcode belum bisa dipindai. Coba lagi atau gunakan Cari Makanan.")
            }
        }

        try {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            providerFuture.addListener({
                if (!disposed.get()) {
                    try {
                        val provider = providerFuture.get()
                        cameraProvider = provider
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    } catch (_: Exception) {
                        reportError("Kamera tidak bisa dibuka. Periksa izin kamera, tutup aplikasi lain yang memakai kamera, lalu coba lagi atau gunakan Cari Makanan.")
                    }
                }
            }, mainExecutor)
        } catch (_: Exception) {
            reportError("Kamera tidak tersedia. Coba lagi atau gunakan Cari Makanan.")
        }

        onDispose {
            disposed.set(true)
            try {
                analysis.clearAnalyzer()
                cameraProvider?.unbind(preview, analysis)
            } finally {
                // Drain queued analyzer callbacks so their proxies can still be closed.
                executor.shutdown()
                if (!inFlight.get()) closeScanner()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .semantics { contentDescription = "Pratinjau kamera untuk memindai barcode produk" }
    ) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { previewView })
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = Spacing.xl)
                .widthIn(max = 240.dp)
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(3.dp, Green500, RoundedCornerShape(20.dp))
        )
    }
}

private fun Context.barcodeActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        val base = current.baseContext
        if (base === current) return null
        current = base
    }
    return current as? Activity
}

private fun Context.openBarcodeSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "Buka Pengaturan aplikasi untuk mengizinkan Kamera, atau gunakan Cari Makanan.", Toast.LENGTH_LONG).show()
    } catch (_: SecurityException) {
        Toast.makeText(this, "Buka Pengaturan aplikasi untuk mengizinkan Kamera, atau gunakan Cari Makanan.", Toast.LENGTH_LONG).show()
    }
}
